package br.ufla.gat108.personaltracker;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import br.ufla.gat108.personaltracker.acquisition.FixFactory;
import br.ufla.gat108.personaltracker.databinding.ActivityReplayBinding;
import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.fault.FaultSpec;
import br.ufla.gat108.personaltracker.io.GpsLoggerCsvReader;
import br.ufla.gat108.personaltracker.replay.ReplayEngine;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ensaio reprodutível na tela: reproduz, de forma acelerada, um registro real gravado (anonimizado,
 * em res/raw), injeta uma falha com semente fixa e mostra, janela a janela, o resíduo antes e depois
 * da reconciliação e se o erro grosseiro foi detectado. Mesma semente = mesmo resultado, sempre.
 */
public class ReplayActivity extends AppCompatActivity {

    private static final long SEED = 20261019L;
    private static final int WINDOW = 30;
    private static final long STEP_MS = 450L;   // pausa entre janelas, só para a plateia acompanhar

    private ActivityReplayBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityReplayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.runCleanButton.setOnClickListener(v -> run("sem falha", FaultSpec.none()));
        binding.runJumpButton.setOnClickListener(v ->
                run("3 saltos de posição de 40 m", FaultSpec.none().withJumps(3, 40.0)));
        binding.runNoiseButton.setOnClickListener(v ->
                run("ruído extra de 4 m na posição", FaultSpec.none().withPositionNoise(4.0)));
        binding.runLossButton.setOnClickListener(v ->
                run("perda de 30% dos fixes + 2 saltos de 40 m", FaultSpec.none().withLoss(0.3).withJumps(2, 40.0)));
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        ui.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void run(String label, FaultSpec spec) {
        setButtonsEnabled(false);
        binding.replayText.setText(getString(R.string.replay_running));
        executor.execute(() -> {
            try {
                List<Fix> track = loadTrack();
                long t0 = System.nanoTime();
                ReplayEngine.Report report = new ReplayEngine().run(track, spec, SEED, WINDOW);
                long computeMs = (System.nanoTime() - t0) / 1_000_000L;
                double dataSeconds = (track.get(track.size() - 1).getTimestamp() - track.get(0).getTimestamp()) / 1000.0;
                showLines(format(report, label, dataSeconds, computeMs));
            } catch (IOException | RuntimeException e) {
                ui.post(() -> {
                    binding.replayText.setText("Erro no ensaio: " + e);
                    setButtonsEnabled(true);
                });
            }
        });
    }

    private List<Fix> loadTrack() throws IOException {
        try (InputStream in = getResources().openRawResource(R.raw.walk_demo);
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return GpsLoggerCsvReader.read(reader, FixFactory.DEFAULT_SPEED_ACCURACY).getFixes();
        }
    }

    private static List<String> format(ReplayEngine.Report r, String label, double dataSeconds, long computeMs) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format(Locale.US, "Ensaio: %s | semente %d", label, r.seed));
        lines.add(String.format(Locale.US, "Registro: %d fixes (%.0f s de dado) -> degradado: %d (perdidos %d)",
                r.inputFixes, dataSeconds, r.degradedFixes, r.dropped));
        lines.add("Saltos injetados nos fixes: " + (r.injected.isEmpty() ? "-" : r.injected.toString()));
        lines.add("");
        for (ReplayEngine.WindowResult w : r.windows) {
            String mark = "";
            for (int idx : w.removed) {
                mark += r.injected.contains(idx) ? "  <- ERRO GROSSEIRO DETECTADO" : "  <- ALARME FALSO";
            }
            lines.add(String.format(Locale.US,
                    "Janela %d (fixes %d-%d)%n  resíduo %.3f -> %.3f m | qui²/gl %.2f%n  removidos: %s%s",
                    w.index, w.startIndex, w.startIndex + r.windowSize - 1,
                    w.residualBefore, w.residualAfter, w.normalizedChiSquare,
                    w.removed.isEmpty() ? "-" : w.removed.toString(), mark));
        }
        lines.add("");
        lines.add(String.format(Locale.US, "Resumo: injetados %d | detectados %d | perdidos %d | alarmes falsos %d",
                r.injected.size(), r.detected.size(), r.missed.size(), r.falseAlarms.size()));
        lines.add(String.format(Locale.US, "%.0f s de dado reproduzidos em %.1f s (cálculo: %d ms)",
                dataSeconds, r.windows.size() * STEP_MS / 1000.0, computeMs));
        return lines;
    }

    /** Mostra uma linha por vez, com pausa, para parecer reprodução acelerada. */
    private void showLines(List<String> lines) {
        StringBuilder shown = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            final boolean last = i == lines.size() - 1;
            ui.postDelayed(() -> {
                if (shown.length() > 0) {
                    shown.append('\n');
                }
                shown.append(line);
                binding.replayText.setText(shown.toString());
                if (last) {
                    setButtonsEnabled(true);
                }
            }, i < 4 ? 0L : (long) (i - 3) * STEP_MS / 3);
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        binding.runCleanButton.setEnabled(enabled);
        binding.runJumpButton.setEnabled(enabled);
        binding.runNoiseButton.setEnabled(enabled);
        binding.runLossButton.setEnabled(enabled);
    }
}
