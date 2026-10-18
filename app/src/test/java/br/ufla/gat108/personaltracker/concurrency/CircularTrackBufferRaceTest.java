package br.ufla.gat108.personaltracker.concurrency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

/** Capacidade 3: teste de corrida, executado 10 vezes e registrado uma a uma. */
public class CircularTrackBufferRaceTest {

    private static final int RUNS = 10;
    private static final int CAPACITY = 64;
    private static final int WRITES = 30_000;
    private static final int READERS = 4;
    private static final int MIN_READS = 500;

    private static List<RaceStress.Result> safeRuns;
    private static List<RaceStress.Result> unsafeRuns;

    private static synchronized List<RaceStress.Result> safeRuns() throws Exception {
        if (safeRuns == null) {
            safeRuns = new ArrayList<>();
            for (int i = 0; i < RUNS; i++) {
                safeRuns.add(RaceStress.run(new SafeBufferAdapter(CAPACITY), WRITES, READERS));
            }
        }
        return safeRuns;
    }

    private static synchronized List<RaceStress.Result> unsafeRuns() throws Exception {
        if (unsafeRuns == null) {
            unsafeRuns = new ArrayList<>();
            for (int i = 0; i < RUNS; i++) {
                unsafeRuns.add(RaceStress.run(new UnsafeBuffer(CAPACITY), WRITES, READERS));
            }
        }
        return unsafeRuns;
    }

    @Test
    public void dezExecucoesDoTesteDeCorridaSemNenhumaFalha() throws Exception {
        List<RaceStress.Result> runs = safeRuns();
        assertEquals(RUNS, runs.size());
        for (int i = 0; i < runs.size(); i++) {
            RaceStress.Result r = runs.get(i);
            assertEquals("execução " + (i + 1) + ": " + r.firstViolation, 0, r.violations);
            assertEquals(WRITES, r.writes);
            // o teste só vale se as leituras de fato ocorreram durante as escritas
            assertTrue("execução " + (i + 1) + ": só " + r.reads + " leituras", r.reads >= MIN_READS);
        }
    }

    @Test
    public void aVersaoSemTravaEhFlagradaPeloMesmoTeste() throws Exception {
        int flagged = 0;
        for (RaceStress.Result r : unsafeRuns()) {
            if (r.violations > 0) {
                flagged++;
            }
        }
        assertTrue("o teste de corrida não flagrou a versão sem trava em nenhuma das " + RUNS
                + " execuções: ele não prova nada", flagged > 0);
    }

    @Test
    public void gerarEvidenciaDaCapacidade3() throws Exception {
        Path dir = Paths.get(System.getProperty("user.dir"), "build", "evidence");
        Files.createDirectories(dir);

        StringBuilder md = new StringBuilder();
        md.append("# Evidência da capacidade 3: teste de corrida\n\n");
        md.append(String.format(Locale.US,
                "Configuração: 1 thread escritora + %d leitoras; buffer de %d fixes; %d escritas por execução; "
              + "processadores disponíveis: %d.%n%n", READERS, CAPACITY, WRITES,
                Runtime.getRuntime().availableProcessors()));

        md.append("## Versão com lock (CircularTrackBuffer)\n\n");
        appendTable(md, safeRuns());
        md.append("\n## Mesmo teste na versão SEM lock (prova de que o teste detecta o defeito)\n\n");
        appendTable(md, unsafeRuns());

        Files.write(dir.resolve("cap3_teste_de_corrida.md"), md.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println(md);
    }

    private static void appendTable(StringBuilder md, List<RaceStress.Result> runs) {
        md.append("| execução | escritas | leituras | violações | duração (ms) | resultado | primeira violação |\n");
        md.append("|---|---|---|---|---|---|---|\n");
        for (int i = 0; i < runs.size(); i++) {
            RaceStress.Result r = runs.get(i);
            md.append(String.format(Locale.US, "| %d | %d | %d | %d | %d | %s | %s |%n",
                    i + 1, r.writes, r.reads, r.violations, r.millis,
                    r.violations == 0 ? "sem falha" : "FALHA",
                    r.firstViolation == null ? "-" : r.firstViolation.replace("|", "/")));
        }
    }
}
