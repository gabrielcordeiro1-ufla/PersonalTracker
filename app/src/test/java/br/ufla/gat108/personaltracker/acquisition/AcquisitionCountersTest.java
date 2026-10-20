package br.ufla.gat108.personaltracker.acquisition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.ufla.gat108.personaltracker.domain.Fix;

import org.junit.Test;

public class AcquisitionCountersTest {

    private static Fix fix(long t) {
        return new Fix(t, -21.2, -44.9, 1f, 3.5f, 0.2f, 0f);
    }

    @Test
    public void comecaZerado() {
        AcquisitionCounters.Snapshot s = new AcquisitionCounters().snapshot();
        assertEquals(0, s.cycles);
        assertEquals(0, s.acceptedFixes);
        assertEquals(-1, s.lastFixTimestamp);
        assertEquals(0.0, s.minIntervalMs, 1e-9);
        assertTrue(Float.isNaN(s.lastAccuracy));
    }

    @Test
    public void acumulaCiclosFixesEMedidas() {
        AcquisitionCounters c = new AcquisitionCounters();
        c.recordCycle(2, 0, 500_000L, 0.0, fix(1000));
        c.recordCycle(0, 1, 300_000L, 1002.0, null);
        c.recordCycle(1, 0, 900_000L, 998.0, fix(3000));

        AcquisitionCounters.Snapshot s = c.snapshot();

        assertEquals(3, s.cycles);
        assertEquals(3, s.acceptedFixes);
        assertEquals(1, s.rejectedFixes);
        assertEquals(900_000L, s.maxCycleNanos);
        assertEquals(900_000L, s.lastCycleNanos);
        assertEquals(3000L, s.lastFixTimestamp);
        assertEquals(3.5f, s.lastAccuracy, 1e-6);
    }

    @Test
    public void intervaloIgnoraOPrimeiroCicloEGuardaMinimoEMaximo() {
        AcquisitionCounters c = new AcquisitionCounters();
        c.recordCycle(0, 0, 1L, 0.0, null);        // primeiro: sem ciclo anterior
        c.recordCycle(0, 0, 1L, 1002.0, null);
        c.recordCycle(0, 0, 1L, 996.0, null);
        c.recordCycle(0, 0, 1L, 1010.0, null);

        AcquisitionCounters.Snapshot s = c.snapshot();

        assertEquals(996.0, s.minIntervalMs, 1e-9);
        assertEquals(1010.0, s.maxIntervalMs, 1e-9);
        assertEquals(1010.0, s.lastIntervalMs, 1e-9);
    }

    @Test
    public void contadoresDeFalhaSaoIndependentes() {
        AcquisitionCounters c = new AcquisitionCounters();
        c.recordSensorRejected();
        c.recordSensorRejected();
        c.recordDropped();
        c.recordError();
        c.recordLogError();

        AcquisitionCounters.Snapshot s = c.snapshot();

        assertEquals(2, s.sensorRejected);
        assertEquals(1, s.dropped);
        assertEquals(1, s.errors);
        assertEquals(1, s.logErrors);
    }
}
