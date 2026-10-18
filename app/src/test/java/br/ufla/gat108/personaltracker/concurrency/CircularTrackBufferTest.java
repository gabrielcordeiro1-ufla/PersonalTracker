package br.ufla.gat108.personaltracker.concurrency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.domain.InvalidTrackException;
import br.ufla.gat108.personaltracker.domain.Track;

import java.util.List;

import org.junit.Test;

public class CircularTrackBufferTest {

    private static Fix fix(int i, float velocity) {
        return new Fix(1000L * (i + 1), -21.2255 - 0.001 * i, -44.9782, velocity, 3f, 0.2f, 180f);
    }

    @Test
    public void snapshotDevolveOsFixesEmOrdem() {
        CircularTrackBuffer b = new CircularTrackBuffer(5);
        b.add(fix(0, 1f));
        b.add(fix(1, 1f));

        List<Fix> snap = b.snapshot();

        assertEquals(2, snap.size());
        assertEquals(1000L, snap.get(0).getTimestamp());
        assertEquals(2000L, snap.get(1).getTimestamp());
    }

    @Test
    public void alterarACopiaNaoAfetaOBuffer() {
        CircularTrackBuffer b = new CircularTrackBuffer(5);
        b.add(fix(0, 1f));
        b.add(fix(1, 1f));

        b.snapshot().clear();
        b.latest(2).clear();
        b.slice(0L, 10_000L).clear();

        assertEquals(2, b.snapshot().size());
        assertEquals(2, b.stats().getSize());
    }

    @Test
    public void latestDevolveOsMaisRecentesEmOrdemMesmoDepoisDeDarVolta() {
        CircularTrackBuffer b = new CircularTrackBuffer(3);
        for (int i = 0; i < 5; i++) {
            b.add(fix(i, 1f));      // restam 3000, 4000, 5000
        }

        List<Fix> last2 = b.latest(2);

        assertEquals(2, last2.size());
        assertEquals(4000L, last2.get(0).getTimestamp());
        assertEquals(5000L, last2.get(1).getTimestamp());
    }

    @Test
    public void latestComNMaiorQueOTamanhoDevolveTudoEZeroDevolveVazio() {
        CircularTrackBuffer b = new CircularTrackBuffer(5);
        b.add(fix(0, 1f));
        b.add(fix(1, 1f));

        assertEquals(2, b.latest(10).size());
        assertTrue(b.latest(0).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> b.latest(-1));
    }

    @Test
    public void sliceDevolveSoOIntervalo() {
        CircularTrackBuffer b = new CircularTrackBuffer(10);
        for (int i = 0; i < 5; i++) {
            b.add(fix(i, 1f));
        }
        assertEquals(3, b.slice(2000L, 4000L).size());
    }

    @Test
    public void statsReunemOsContadoresDeUmaVez() {
        CircularTrackBuffer b = new CircularTrackBuffer(3);
        for (int i = 0; i < 5; i++) {
            b.add(fix(i, i + 1f));   // velocidades 1..5
        }

        CircularTrackBuffer.Stats s = b.stats();

        assertEquals(3, s.getSize());
        assertEquals(5L, s.getTotalAdded());
        assertEquals(3.0, s.getMeanSpeed(), 1e-9);
        assertEquals(4 * 111.19, s.getLengthMeters(), 2.0);
    }

    @Test
    public void bufferVazioTemEstatisticasZeradas() {
        CircularTrackBuffer.Stats s = new CircularTrackBuffer(4).stats();
        assertEquals(0, s.getSize());
        assertEquals(0L, s.getTotalAdded());
        assertEquals(0.0, s.getLengthMeters(), 1e-9);
        assertEquals(0.0, s.getMeanSpeed(), 1e-9);
    }

    @Test
    public void herdaAsInvariantesDoTrack() {
        CircularTrackBuffer b = new CircularTrackBuffer(3);
        assertThrows(InvalidTrackException.class, () -> new CircularTrackBuffer(0));
        assertThrows(InvalidTrackException.class, () -> b.add(null));

        b.add(fix(5, 1f));
        assertThrows(InvalidTrackException.class, () -> b.add(fix(0, 1f)));   // fora de ordem
    }

    @Test
    public void construtorPadraoUsaACapacidadePadraoDoTrack() {
        assertEquals(Track.DEFAULT_CAPACITY, new CircularTrackBuffer().capacity());
    }
}
