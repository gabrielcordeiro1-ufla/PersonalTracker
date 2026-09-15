package br.ufla.gat108.personaltracker.domain;

import org.junit.Test;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertEquals;

public class FixTest {

    @Test
    public void deveCriarFixComCoordenadasValidas() {
        Fix fix = new Fix(1694784000000L, -21.2255, -44.9782, 1.5f, 4.2f);

        assertEquals(-21.2255, fix.getLatitude(), 0.0001);
        assertEquals(-44.9782, fix.getLongitude(), 0.0001);
    }

    @Test
    public void deveLancarExcecaoParaLatitudeInvalida() {
        assertThrows(InvalidCoordinatesException.class, () -> {
            new Fix(1694784000000L, 150.0, -44.9782, 1.5f, 4.2f);
        });
    }

    @Test
    public void deveLancarExcecaoParaLongitudeInvalida() {
        assertThrows(InvalidCoordinatesException.class, () -> {
            new Fix(1694784000000L, -21.2255, 200.0, 1.5f, 4.2f);
        });
    }

    @Test
    public void deveLancarExcecaoParaAcuraciaNegativa() {
        assertThrows(InvalidCoordinatesException.class, () -> {
            new Fix(1694784000000L, -21.2255, -44.9782, 1.5f, -2.0f);
        });
    }
}