package br.ufla.gat108.personaltracker.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class TrackTest {

    // 0,001 grau de latitude = ~111,19 m
    private static final double STEP_METERS = 111.19;

    /** Fix andando para o sul, 0,001 grau por passo, 1 s entre fixes. */
    private static Fix fixAt(int i, float velocity) {
        return new Fix(1000L * (i + 1), -21.2255 - 0.001 * i, -44.9782, velocity, 3f, 0.2f, 180f);
    }

    @Test
    public void trajetoVazioTemComprimentoEMediaZero() {
        Track track = new Track(5);

        assertEquals(0.0, track.getLength(), 1e-9);
        assertEquals(0.0, track.getMeanSpeed(), 1e-9);
        assertEquals(0, track.size());
        assertTrue(track.getFixes().isEmpty());
        assertTrue(track.slice(0L, Long.MAX_VALUE).isEmpty());
    }

    @Test
    public void deveAcumularDistanciaEntreFixes() {
        Track track = new Track(5);

        track.addFix(fixAt(0, 1f));
        assertEquals(0.0, track.getLength(), 1e-9);

        track.addFix(fixAt(1, 1f));
        assertEquals(STEP_METERS, track.getLength(), 0.5);

        track.addFix(fixAt(2, 1f));
        assertEquals(2 * STEP_METERS, track.getLength(), 1.0);
    }

    @Test
    public void memoriaPermaneceConstanteMesmoComMuitosFixes() {
        int capacity = 5;
        int total = 10_000;
        Track track = new Track(capacity);

        for (int i = 0; i < total; i++) {
            track.addFix(new Fix(1000L + i, -21.2255, -44.9782, 1f, 3f, 0.2f, 0f));
        }

        assertEquals(capacity, track.size());
        assertEquals(capacity, track.getFixes().size());
        assertEquals(total, track.getTotalAdded());
        // ficam só os cinco mais recentes, em ordem cronológica
        List<Fix> kept = track.getFixes();
        assertEquals(1000L + total - capacity, kept.get(0).getTimestamp());
        assertEquals(1000L + total - 1, kept.get(capacity - 1).getTimestamp());
    }

    @Test
    public void comprimentoContaTambemOsFixesJaDescartados() {
        Track track = new Track(3);

        for (int i = 0; i < 5; i++) {
            track.addFix(fixAt(i, 1f));
        }

        assertEquals(3, track.size());
        assertEquals(4 * STEP_METERS, track.getLength(), 2.0);
    }

    @Test
    public void deveCalcularVelocidadeMedia() {
        Track track = new Track(10);
        track.addFix(fixAt(0, 1f));
        track.addFix(fixAt(1, 2f));
        track.addFix(fixAt(2, 3f));

        assertEquals(2.0, track.getMeanSpeed(), 1e-9);
    }

    @Test
    public void sliceDevolveSoOsFixesDoIntervaloInclusivo() {
        Track track = new Track(10);
        for (int i = 0; i < 5; i++) {
            track.addFix(fixAt(i, 1f)); // timestamps 1000..5000
        }

        List<Fix> slice = track.slice(2000L, 4000L);

        assertEquals(3, slice.size());
        assertEquals(2000L, slice.get(0).getTimestamp());
        assertEquals(4000L, slice.get(2).getTimestamp());
    }

    @Test
    public void sliceNaoVeFixesJaDescartados() {
        Track track = new Track(3);
        for (int i = 0; i < 5; i++) {
            track.addFix(fixAt(i, 1f)); // restam 3000, 4000, 5000
        }

        assertEquals(3, track.slice(0L, 10_000L).size());
        assertTrue(track.slice(1000L, 2000L).isEmpty());
    }

    @Test
    public void sliceComIntervaloInvertidoLancaExcecao() {
        Track track = new Track(3);
        assertThrows(InvalidTrackException.class, () -> track.slice(5000L, 1000L));
    }

    @Test
    public void getFixesDevolveCopiaEntaoAlterarACopiaNaoAfetaOTrajeto() {
        Track track = new Track(5);
        track.addFix(fixAt(0, 1f));
        track.addFix(fixAt(1, 1f));

        List<Fix> copy = track.getFixes();
        copy.clear();

        assertEquals(2, track.size());
        assertEquals(2, track.getFixes().size());
    }

    @Test
    public void deveRejeitarFixForaDeOrdem() {
        Track track = new Track(5);
        track.addFix(fixAt(2, 1f)); // timestamp 3000

        assertThrows(InvalidTrackException.class, () -> track.addFix(fixAt(0, 1f))); // 1000
    }

    @Test
    public void deveRejeitarFixNulo() {
        Track track = new Track(5);
        assertThrows(InvalidTrackException.class, () -> track.addFix(null));
    }

    @Test
    public void deveRejeitarCapacidadeInvalida() {
        assertThrows(InvalidTrackException.class, () -> new Track(0));
        assertThrows(InvalidTrackException.class, () -> new Track(-3));
    }
}
