package br.ufla.gat108.personaltracker.domain;

/**
 * Cálculos geográficos em Java puro, sem depender de classes do Android
 * (assim rodam nos testes de unidade na JVM).
 */
public final class GeoMath {

    public static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private GeoMath() {
    }

    /** Distância em metros entre dois pontos pela fórmula de Haversine. */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    // ---- Projeção local equiretangular (válida para janelas de algumas centenas de metros) ----

    /** Deslocamento para leste, em metros, de (originLat, originLon) até a longitude lon. */
    public static double eastMeters(double originLat, double originLon, double lon) {
        return Math.toRadians(lon - originLon) * EARTH_RADIUS_METERS * Math.cos(Math.toRadians(originLat));
    }

    /** Deslocamento para norte, em metros, de originLat até a latitude lat. */
    public static double northMeters(double originLat, double lat) {
        return Math.toRadians(lat - originLat) * EARTH_RADIUS_METERS;
    }

    /** Inversa de northMeters. */
    public static double latitudeFromNorth(double originLat, double northMeters) {
        return originLat + Math.toDegrees(northMeters / EARTH_RADIUS_METERS);
    }

    /** Inversa de eastMeters. */
    public static double longitudeFromEast(double originLat, double originLon, double eastMeters) {
        return originLon + Math.toDegrees(
                eastMeters / (EARTH_RADIUS_METERS * Math.cos(Math.toRadians(originLat))));
    }
}
