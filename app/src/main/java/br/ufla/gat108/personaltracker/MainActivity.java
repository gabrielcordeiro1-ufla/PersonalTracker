package br.ufla.gat108.personaltracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import br.ufla.gat108.personaltracker.acquisition.AcquisitionCounters;
import br.ufla.gat108.personaltracker.concurrency.CircularTrackBuffer;
import br.ufla.gat108.personaltracker.databinding.ActivityMainBinding;
import br.ufla.gat108.personaltracker.service.AcquisitionRuntime;
import br.ufla.gat108.personaltracker.service.AcquisitionService;

import java.util.Locale;

/**
 * Tela de operação: pede as permissões na ordem certa, inicia e para a aquisição e mostra, uma
 * vez por segundo, o estado e as medidas da aquisição.
 *
 * Ordem: localização precisa (primeiro plano) -> notificações (Android 13+) -> serviço.
 * A localização em segundo plano não é pedida: o serviço do tipo "location" iniciado com a tela
 * visível continua lendo o GPS com a tela bloqueada (ver D-26).
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            render();
            uiHandler.postDelayed(this, 1000L);
        }
    };

    private final ActivityResultLauncher<String[]> locationLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> afterLocationPermission());

    private final ActivityResultLauncher<String> notificationLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> startAcquisition());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.startButton.setOnClickListener(v -> onStartClicked());
        binding.stopButton.setOnClickListener(v -> stopService(new Intent(this, AcquisitionService.class)));
        binding.replayButton.setOnClickListener(v -> startActivity(new Intent(this, ReplayActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        uiHandler.post(refresh);
    }

    @Override
    protected void onPause() {
        uiHandler.removeCallbacks(refresh);
        super.onPause();
    }

    private void onStartClicked() {
        if (hasFineLocation()) {
            afterLocationPermission();
        } else {
            // as duas juntas: a partir do Android 12 o usuário escolhe entre precisa e aproximada
            locationLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    private void afterLocationPermission() {
        if (!hasFineLocation()) {
            binding.hintText.setText(R.string.hint_permission_denied);
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            startAcquisition();
        }
    }

    private void startAcquisition() {
        binding.hintText.setText("");
        startForegroundService(new Intent(this, AcquisitionService.class));
    }

    private boolean hasFineLocation() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void render() {
        boolean running = AcquisitionRuntime.isRunning();
        AcquisitionCounters.Snapshot s = AcquisitionRuntime.counters().snapshot();
        CircularTrackBuffer.Stats stats = AcquisitionRuntime.buffer().stats();

        long elapsedS = running ? (SystemClock.elapsedRealtime() - AcquisitionRuntime.startElapsedMs()) / 1000L : 0L;
        String lastFix = s.lastFixTimestamp < 0 ? "-"
                : String.format(Locale.US, "há %.0f s", Math.max(0, System.currentTimeMillis() - s.lastFixTimestamp) / 1000.0);

        binding.statusText.setText(String.format(Locale.US,
                "Estado: %s%n"
              + "Tempo adquirindo: %02d:%02d:%02d%n"
              + "Ciclos (1 s): %d%n"
              + "Fixes aceitos: %d%n"
              + "No buffer: %d / %d%n"
              + "Descartados: fora de ordem %d, inválidos %d, fila cheia %d%n"
              + "Erros: %d (registro: %d)%n"
              + "Último fix: %s | acurácia %s m%n"
              + "Intervalo entre ciclos: último %.0f ms (mín %.0f, máx %.0f)%n"
              + "Tempo do ciclo: último %d us, máximo %d us%n"
              + "Registro: %s",
                running ? "ADQUIRINDO" : "parado",
                elapsedS / 3600, (elapsedS / 60) % 60, elapsedS % 60,
                s.cycles, s.acceptedFixes, stats.getSize(), AcquisitionRuntime.buffer().capacity(),
                s.rejectedFixes, s.sensorRejected, s.dropped,
                s.errors, s.logErrors,
                lastFix, Float.isNaN(s.lastAccuracy) ? "-" : String.format(Locale.US, "%.1f", s.lastAccuracy),
                s.lastIntervalMs, s.minIntervalMs, s.maxIntervalMs,
                s.lastCycleNanos / 1000L, s.maxCycleNanos / 1000L,
                AcquisitionRuntime.logPath().isEmpty() ? "-" : AcquisitionRuntime.logPath()));

        if (running && !AcquisitionRuntime.isGpsEnabled()) {
            binding.hintText.setText(R.string.hint_gps_off);
        } else if (!running && !isLocationEnabled()) {
            binding.hintText.setText(R.string.hint_gps_off);
        }
        binding.startButton.setEnabled(!running);
        binding.stopButton.setEnabled(running);
    }

    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        return lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}
