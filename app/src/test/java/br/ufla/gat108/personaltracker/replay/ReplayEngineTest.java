package br.ufla.gat108.personaltracker.replay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.fault.FaultSpec;
import br.ufla.gat108.personaltracker.io.GpsLoggerCsvReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

public class ReplayEngineTest {

    private static final long SEED = 20261019L;

    static List<Fix> walk(String name) throws IOException {
        InputStream in = ReplayEngineTest.class.getResourceAsStream("/data/" + name);
        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return GpsLoggerCsvReader.read(r, 0.114f).getFixes();
        }
    }

    @Test
    public void semFalhaNaoHaInjetadosEOsFixesPassamPeloDetector() throws IOException {
        List<Fix> record = walk("walk1.csv");

        ReplayEngine.Report r = new ReplayEngine().run(record, FaultSpec.none(), SEED, 30);

        assertEquals(record.size() / 30, r.windows.size());
        assertTrue(r.injected.isEmpty());
        assertTrue(r.falseAlarms.isEmpty());
        for (ReplayEngine.WindowResult w : r.windows) {
            assertTrue(w.residualBefore > 0.0);
            assertEquals(0.0, w.residualAfter, 1e-6);
        }
    }

    @Test
    public void saltosInjetadosSaoDetectadosNosFixesCertos() throws IOException {
        List<Fix> record = walk("walk1.csv");

        ReplayEngine.Report r = new ReplayEngine().run(record, FaultSpec.none().withJumps(3, 40.0), SEED, 30);

        assertEquals(3, r.injected.size());
        assertEquals(r.injected, r.detected);
        assertTrue(r.missed.isEmpty());
        assertTrue(r.falseAlarms.isEmpty());
    }

    @Test
    public void ensaioEDeterministicoComASementeFixa() throws IOException {
        List<Fix> record = walk("walk2.csv");
        FaultSpec spec = FaultSpec.none().withJumps(2, 30.0).withPositionNoise(2.0).withLoss(0.1);

        ReplayEngine.Report a = new ReplayEngine().run(record, spec, SEED, 30);
        ReplayEngine.Report b = new ReplayEngine().run(record, spec, SEED, 30);

        assertEquals(a.injected, b.injected);
        assertEquals(a.detected, b.detected);
        assertEquals(a.falseAlarms, b.falseAlarms);
        assertEquals(a.dropped, b.dropped);
        assertEquals(a.windows.size(), b.windows.size());
        for (int i = 0; i < a.windows.size(); i++) {
            assertEquals(a.windows.get(i).residualBefore, b.windows.get(i).residualBefore, 0.0);
            assertEquals(a.windows.get(i).removed, b.windows.get(i).removed);
        }
    }

    @Test
    public void janelaPequenaDemaisLancaExcecao() throws IOException {
        List<Fix> record = walk("walk1.csv");
        try {
            new ReplayEngine().run(record, FaultSpec.none(), SEED, 3);
            throw new AssertionError("deveria lançar");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }
}
