package br.ufla.gat108.personaltracker.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlaceTest {

    private static final double LAT = -21.2255;
    private static final double LON = -44.9782;

    @Test
    public void deveCriarLugarValido() {
        Place place = new Place(LAT, LON, 50.0);

        assertEquals(LAT, place.getLatitude(), 1e-9);
        assertEquals(LON, place.getLongitude(), 1e-9);
        assertEquals(50.0, place.getRadiusMeters(), 1e-9);
    }

    @Test
    public void deveRejeitarRaioInvalido() {
        assertThrows(InvalidCoordinatesException.class, () -> new Place(LAT, LON, 0.0));
        assertThrows(InvalidCoordinatesException.class, () -> new Place(LAT, LON, -5.0));
        assertThrows(InvalidCoordinatesException.class, () -> new Place(LAT, LON, Double.NaN));
        assertThrows(InvalidCoordinatesException.class,
                () -> new Place(LAT, LON, Double.POSITIVE_INFINITY));
    }

    @Test
    public void deveRejeitarCoordenadaInvalida() {
        assertThrows(InvalidCoordinatesException.class, () -> new Place(95.0, LON, 50.0));
        assertThrows(InvalidCoordinatesException.class, () -> new Place(LAT, -200.0, 50.0));
    }

    @Test
    public void containsDistingueDentroEFora() {
        Place place = new Place(LAT, LON, 50.0);

        assertTrue(place.contains(LAT - 0.0002, LON));   // ~22 m
        assertFalse(place.contains(LAT - 0.001, LON));   // ~111 m
    }

    @Test
    public void containsRejeitaCoordenadaInvalida() {
        Place place = new Place(LAT, LON, 50.0);
        assertThrows(InvalidCoordinatesException.class, () -> place.contains(100.0, LON));
    }
}
