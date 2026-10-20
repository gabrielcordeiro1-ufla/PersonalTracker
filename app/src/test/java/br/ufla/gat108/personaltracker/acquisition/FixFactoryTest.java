package br.ufla.gat108.personaltracker.acquisition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.domain.InvalidCoordinatesException;

import org.junit.Test;

public class FixFactoryTest {

    private static final long T = 1_700_000_000_000L;

    @Test
    public void convertePreservandoTodosOsCamposDoSensor() {
        Fix f = FixFactory.fromSensor(T, -21.2, -44.9, true, 1.4f, true, 3.5f, true, 0.2f, true, 90f);

        assertEquals(T, f.getTimestamp());
        assertEquals(1.4f, f.getVelocity(), 1e-6);
        assertEquals(3.5f, f.getAccuracy(), 1e-6);
        assertEquals(0.2f, f.getSpeedAccuracy(), 1e-6);
        assertEquals(90f, f.getBearing(), 1e-6);
    }

    @Test
    public void semAcuraciaDeVelocidadeUsaAEstimativaDoD20() {
        Fix f = FixFactory.fromSensor(T, -21.2, -44.9, true, 1.4f, true, 3.5f, false, 0f, true, 90f);
        assertEquals(FixFactory.DEFAULT_SPEED_ACCURACY, f.getSpeedAccuracy(), 1e-6);
    }

    @Test
    public void acuraciaDeVelocidadeZeroTambemUsaAEstimativa() {
        // o Android devolve 0 quando não tem a informação, mesmo com hasSpeedAccuracy verdadeiro
        Fix f = FixFactory.fromSensor(T, -21.2, -44.9, true, 1.4f, true, 3.5f, true, 0f, true, 90f);
        assertEquals(FixFactory.DEFAULT_SPEED_ACCURACY, f.getSpeedAccuracy(), 1e-6);
    }

    @Test
    public void velocidadeOuRumoAusentesViramZero() {
        Fix f = FixFactory.fromSensor(T, -21.2, -44.9, false, 7f, true, 3.5f, true, 0.2f, false, 90f);
        assertEquals(0f, f.getVelocity(), 1e-6);
        assertEquals(0f, f.getBearing(), 1e-6);
    }

    @Test
    public void rumo360ViraZeroENegativoEhNormalizado() {
        assertEquals(0f, FixFactory.fromSensor(T, -21.2, -44.9, true, 1f, true, 3f, true, 0.2f, true, 360f)
                .getBearing(), 1e-6);
        assertEquals(350f, FixFactory.fromSensor(T, -21.2, -44.9, true, 1f, true, 3f, true, 0.2f, true, -10f)
                .getBearing(), 1e-4);
    }

    @Test
    public void semAcuraciaLancaExcecaoEspecifica() {
        assertThrows(InvalidCoordinatesException.class,
                () -> FixFactory.fromSensor(T, -21.2, -44.9, true, 1f, false, 0f, true, 0.2f, true, 90f));
    }

    @Test
    public void valoresInvalidosDoSensorLancamExcecaoEspecifica() {
        assertThrows(InvalidCoordinatesException.class,
                () -> FixFactory.fromSensor(T, 95.0, -44.9, true, 1f, true, 3f, true, 0.2f, true, 90f));
        assertThrows(InvalidCoordinatesException.class,
                () -> FixFactory.fromSensor(T, -21.2, -44.9, true, -1f, true, 3f, true, 0.2f, true, 90f));
    }
}
