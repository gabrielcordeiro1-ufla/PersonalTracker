package br.ufla.gat108.personaltracker.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GeoMathTest {

    // 1 grau de latitude = 2 * pi * R / 360, com R = 6.371.000 m
    private static final double ONE_DEGREE_METERS = 111_194.93;

    @Test
    public void mesmoPontoTemDistanciaZero() {
        assertEquals(0.0, GeoMath.distanceMeters(-21.2255, -44.9782, -21.2255, -44.9782), 1e-9);
    }

    @Test
    public void umGrauDeLatitudeValeAproximadamente111Km() {
        assertEquals(ONE_DEGREE_METERS, GeoMath.distanceMeters(0.0, 0.0, 1.0, 0.0), 1.0);
    }

    @Test
    public void umGrauDeLongitudeNoEquadorValeOMesmoQueUmDeLatitude() {
        assertEquals(ONE_DEGREE_METERS, GeoMath.distanceMeters(0.0, 0.0, 0.0, 1.0), 1.0);
    }

    @Test
    public void umGrauDeLongitudeEmLatitude60ValeAMetade() {
        assertEquals(ONE_DEGREE_METERS / 2, GeoMath.distanceMeters(60.0, 0.0, 60.0, 1.0), 10.0);
    }

    @Test
    public void distanciaEhSimetrica() {
        double ida = GeoMath.distanceMeters(-21.2255, -44.9782, -21.2265, -44.9700);
        double volta = GeoMath.distanceMeters(-21.2265, -44.9700, -21.2255, -44.9782);
        assertEquals(ida, volta, 1e-9);
    }
}
