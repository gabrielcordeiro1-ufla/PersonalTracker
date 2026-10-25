package br.ufla.gat108.personaltracker.evidence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.fault.FaultInjector;
import br.ufla.gat108.personaltracker.fault.FaultSpec;
import br.ufla.gat108.personaltracker.reconciliation.KinematicReconciler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

/**
 * Capacidade 5: tabela de sensibilidade da detecção à degradação injetada, sobre dados reais.
 * Semente base declarada; cada tentativa usa SEED_BASE + número da tentativa. Reproduz com
 * ./gradlew cleanTest test; o arquivo sai em app/build/evidence/cap5_sensibilidade.md.
 */
public class Cap5EvidenceTest {

    static final long SEED_BASE = 20261019L;
    static final int SEEDS_PER_WINDOW = 6;

    private static final double[] JUMPS_M = {5, 10, 15, 20, 30, 40, 60, 100};
    private static final double[] NOISE_M = {0, 1, 2, 4, 8, 16};
    private static final double[] LOSS = {0.0, 0.1, 0.3, 0.5, 0.7};
    private static final double[] SPEED_NOISE = {0.0, 0.1, 0.5, 1.0, 2.0};

    /** Resultado agregado de uma célula da tabela. */
    static final class Cell {
        int trials;
        int skipped;        // janela curta demais depois da perda para avaliar
        int detected;       // só faz sentido quando há salto injetado
        int falseAlarm;     // houve remoção de fix que não era o injetado
        double removalsSum;
        double chiSum;

        double detectionRate() { return trials == 0 ? Double.NaN : detected / (double) trials; }
        double falseAlarmRate() { return trials == 0 ? Double.NaN : falseAlarm / (double) trials; }
        double meanRemovals() { return removalsSum / trials; }
        double meanChi() { return chiSum / trials; }
    }

    static Cell run(List<List<Fix>> windows, FaultSpec spec) {
        KinematicReconciler reconciler = new KinematicReconciler();
        Cell cell = new Cell();
        long trialNumber = 0;
        for (List<Fix> window : windows) {
            for (int s = 0; s < SEEDS_PER_WINDOW; s++) {
                FaultInjector.Result inj;
                try {
                    inj = new FaultInjector(SEED_BASE + trialNumber++).inject(window, spec);
                } catch (IllegalArgumentException tooShort) {
                    cell.skipped++;   // a perda deixou fixes demais de menos para pôr o salto
                    continue;
                }
                List<Fix> degraded = inj.getFixes();
                if (degraded.size() < 4) {
                    cell.skipped++;
                    continue;
                }
                KinematicReconciler.Result r = reconciler.reconcile(degraded, true);
                List<Integer> removed = r.getRemovedIndices();
                List<Integer> jumps = inj.getJumpIndices();
                cell.trials++;
                cell.removalsSum += removed.size();
                cell.chiSum += r.getNormalizedChiSquare();
                if (!jumps.isEmpty() && removed.contains(jumps.get(0))) {
                    cell.detected++;
                }
                boolean other = false;
                for (int idx : removed) {
                    if (!jumps.contains(idx)) {
                        other = true;
                    }
                }
                if (other) {
                    cell.falseAlarm++;
                }
            }
        }
        return cell;
    }

    private static String pct(double v) {
        return Double.isNaN(v) ? "-" : String.format(Locale.US, "%.0f%%", 100.0 * v);
    }

    @Test
    public void tabelaDeSensibilidadeDaDeteccao() throws IOException {
        List<List<Fix>> windows = Cap2Data.allRealWindows();
        StringBuilder md = new StringBuilder();
        md.append("# Sensibilidade da detecção à degradação injetada (dados reais)\n\n");
        md.append(String.format(Locale.US,
                "Semente base declarada: **%d** (a tentativa k usa a semente base + k). %d janelas reais de %d fixes dos "
              + "três trajetos x %d sementes por janela = até %d tentativas por linha.%n"
              + "Detector: limiar %.1f no resíduo normalizado, no máximo %d remoções por janela.%n%n",
                SEED_BASE, windows.size(), Cap2Data.WINDOW_SIZE, SEEDS_PER_WINDOW,
                windows.size() * SEEDS_PER_WINDOW, 3.5, 3));

        // ---- salto ----
        md.append("## Salto de posição (1 salto por janela, direção sorteada)\n\n");
        md.append("| salto (m) | detectado | alarme falso | remoções por janela (média) |\n|---|---|---|---|\n");
        Cell baseline = run(windows, FaultSpec.none());
        md.append(String.format(Locale.US, "| 0 (sem falha) | - | %s | %.2f |%n",
                pct(baseline.falseAlarmRate()), baseline.meanRemovals()));
        double firstReliable = Double.NaN;
        Cell at40 = null;
        Cell at100 = null;
        Cell at5 = null;
        for (double m : JUMPS_M) {
            Cell c = run(windows, FaultSpec.none().withJumps(1, m));
            md.append(String.format(Locale.US, "| %.0f | %s | %s | %.2f |%n",
                    m, pct(c.detectionRate()), pct(c.falseAlarmRate()), c.meanRemovals()));
            if (Double.isNaN(firstReliable) && c.detectionRate() >= 0.9) {
                firstReliable = m;
            }
            if (m == 40) at40 = c;
            if (m == 100) at100 = c;
            if (m == 5) at5 = c;
        }
        md.append(String.format(Locale.US,
                "%nMenor salto detectado em pelo menos 90%% das tentativas: **%.0f m**. Abaixo disso a detecção começa a falhar: "
              + "o salto fica da ordem do ruído da própria medida.%n%n", firstReliable));

        // ---- ruído ----
        md.append("## Ruído extra de posição (sem salto)\n\n");
        md.append("| ruído extra (m, por eixo) | janelas com alarme falso | qui² / g.l. médio |\n|---|---|---|\n");
        for (double s : NOISE_M) {
            Cell c = run(windows, FaultSpec.none().withPositionNoise(s));
            md.append(String.format(Locale.US, "| %.0f | %s | %.2f |%n", s, pct(c.falseAlarmRate()), c.meanChi()));
        }
        md.append("\nO detector confia na acurácia declarada pelo sensor: com ruído bem acima dela, o qui² sobe e ele passa a "
                + "remover fixes bons.\n\n");

        // ---- perda ----
        md.append("## Perda de fixes (descarte aleatório; salto de 40 m na coluna de detecção)\n\n");
        md.append("| perda | alarme falso (sem salto) | salto de 40 m detectado | alarme falso (com salto) | tentativas avaliadas (com salto) |\n|---|---|---|---|---|\n");
        for (double p : LOSS) {
            Cell clean = run(windows, FaultSpec.none().withLoss(p));
            Cell jump = run(windows, FaultSpec.none().withLoss(p).withJumps(1, 40.0));
            md.append(String.format(Locale.US, "| %.0f%% | %s | %s | %s | %d de %d |%n", 100 * p,
                    pct(clean.falseAlarmRate()), pct(jump.detectionRate()), pct(jump.falseAlarmRate()),
                    jump.trials, jump.trials + jump.skipped));
        }

        // ---- ruído de velocidade ----
        md.append("\n## Ruído extra de velocidade (sem salto)\n\n");
        md.append("| ruído extra (m/s) | janelas com alarme falso | qui² / g.l. médio |\n|---|---|---|\n");
        for (double s : SPEED_NOISE) {
            Cell c = run(windows, FaultSpec.none().withSpeedNoise(s));
            md.append(String.format(Locale.US, "| %.1f | %s | %.2f |%n", s, pct(c.falseAlarmRate()), c.meanChi()));
        }

        Path dir = Paths.get(System.getProperty("user.dir"), "build", "evidence");
        Files.createDirectories(dir);
        Files.write(dir.resolve("cap5_sensibilidade.md"), md.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println(md);

        // critérios quantitativos
        assertTrue("alarme falso sem falha: " + baseline.falseAlarmRate(), baseline.falseAlarmRate() <= 0.05);
        assertTrue("detecção a 40 m: " + at40.detectionRate(), at40.detectionRate() >= 0.95);
        assertTrue("detecção a 100 m: " + at100.detectionRate(), at100.detectionRate() >= 0.95);
        assertTrue("a detecção deveria falhar com salto de 5 m: " + at5.detectionRate(), at5.detectionRate() < 0.5);
        assertTrue(!Double.isNaN(firstReliable));
    }

    @Test
    public void amesmaSementeRepetidaDaATabelaIdentica() throws IOException {
        List<List<Fix>> windows = Cap2Data.allRealWindows();
        FaultSpec spec = FaultSpec.none().withJumps(1, 20.0).withPositionNoise(2.0);

        Cell a = run(windows, spec);
        Cell b = run(windows, spec);

        assertEquals(a.trials, b.trials);
        assertEquals(a.detected, b.detected);
        assertEquals(a.falseAlarm, b.falseAlarm);
        assertEquals(a.removalsSum, b.removalsSum, 0.0);
        assertEquals(a.chiSum, b.chiSum, 0.0);
    }
}
