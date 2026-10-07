package br.ufla.gat108.personaltracker.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class FixTest {

    private static final long TS = 1694784000000L;

    @Test
    public void deveCriarFixComTodosOsCamposValidos() {
        Fix fix = new Fix(TS, -21.2255, -44.9782, 1.5f, 4.2f, 0.3f, 90f);

        assertEquals(TS, fix.getTimestamp());
        assertEquals(-21.2255, fix.getLatitude(), 1e-9);
        assertEquals(-44.9782, fix.getLongitude(), 1e-9);
        assertEquals(1.5f, fix.getVelocity(), 1e-6);
        assertEquals(4.2f, fix.getAccuracy(), 1e-6);
        assertEquals(0.3f, fix.getSpeedAccuracy(), 1e-6);
        assertEquals(90f, fix.getBearing(), 1e-6);
    }

    @Test
    public void deveAceitarLimitesDasFaixas() {
        new Fix(0L, -90.0, -180.0, 0f, 0f, 0f, 0f);
        new Fix(0L, 90.0, 180.0, 0f, 0f, 0f, 359.99f);
    }

    @Test
    public void deveLancarExcecaoParaLatitudeInvalida() {
        assertThrows(InvalidCoordinatesException.class,
                () -> new Fix(TS, 150.0, -44.9782, 1.5f, 4.2f, 0.3f, 90f));
    }

    @Test
    public void deveLancarExcecaoParaLongitudeInvalida() {
        assertThrows(InvalidCoordinatesException.class,
                () -> new Fix(TS, -21.2255, 200.0, 1.5f, 4.2f, 0.3f, 90f));
    }

    @Test
    public void deveLancarExcecaoParaCoordenadaNaN() {
        assertThrows(InvalidCoordinatesException.class,
                () -> new Fix(TS, Double.NaN, -44.9782, 1.5f, 4.2f, 0.3f, 90f));
        assertThrows(InvalidCoordinatesException.class,
                () -> new Fix(TS, -21.2255, Double.NaN, 1.5f, 4.2f, 0.3f, 90f));
    }

    @Test
    public void deveLancarExcecaoParaAcuraciaNegativaOuNaN() {
        assertThrows(InvalidCoordinatesException.class,
                () -> new Fix(TS, -21.2255, -44.9782, 1.5f, -2.0f, 0.3f, 90f));
        assertThrows(InvalidCoordinatesException.class,
                () -> new Fix(TS, -21.2255, -44.9782, 1.5f, Float.NaN, 0.3f, 90f));
    }

    @Test
    public void deveLancarExcecaoParaAcuraciaDeVelocidadeInvalida() {
        assertThrows(InvalidCoordinatesException.class,
                () -> new Fix(TS, -21.2255, -44.9782, 1.5f, 4.2f, -0.1f, 90f));
        assertThrows(InvalidCoordinatesException.class,
                () -> new Fix(TS, -21.2255, -44.9782, 1.5f, 4.2f, Float.POSITIVE_INFINITY, 90f));
    }

    @Test
    public void deveLancarExcecaoParaVelocidadeNegativa() {
        assertThrows(InvalidCoordinatesException.class,
                () -> new Fix(TS, -21.2255, -44.9782, -1.0f, 4.2f, 0.3f, 90f));
    }

    @Test
    public void deveLancarExcecaoParaRumoForaDaFaixa() {
        assertThrows(InvalidCoordinatesException.class,
                () -> new Fix(TS, -21.2255, -44.9782, 1.5f, 4.2f, 0.3f, 360f));
        assertThrows(InvalidCoordinatesException.class,
                () -> new Fix(TS, -21.2255, -44.9782, 1.5f, 4.2f, 0.3f, -1f));
        assertThrows(InvalidCoordinatesException.class,
                () -> new Fix(TS, -21.2255, -44.9782, 1.5f, 4.2f, 0.3f, Float.NaN));
    }

    @Test
    public void deveLancarExcecaoParaTimestampNegativo() {
        assertThrows(InvalidCoordinatesException.class,
                () -> new Fix(-1L, -21.2255, -44.9782, 1.5f, 4.2f, 0.3f, 90f));
    }
}
