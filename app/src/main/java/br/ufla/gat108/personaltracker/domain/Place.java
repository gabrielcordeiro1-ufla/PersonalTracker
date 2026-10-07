package br.ufla.gat108.personaltracker.domain;

/** Lugar de interesse: um círculo (centro e raio) em torno de uma coordenada. Imutável. */
public final class Place {
    private final double latitude;
    private final double longitude;
    private final double radiusMeters;

    public Place(double latitude, double longitude, double radiusMeters) {
        this.latitude = Validation.latitude(latitude);
        this.longitude = Validation.longitude(longitude);
        if (Double.isNaN(radiusMeters) || Double.isInfinite(radiusMeters) || radiusMeters <= 0.0) {
            throw new InvalidCoordinatesException(
                    "Raio inválido: deve ser finito e positivo. Valor recebido: " + radiusMeters);
        }
        this.radiusMeters = radiusMeters;
    }

    public boolean contains(double lat, double lon) {
        Validation.latitude(lat);
        Validation.longitude(lon);
        return GeoMath.distanceMeters(latitude, longitude, lat, lon) <= radiusMeters;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getRadiusMeters() { return radiusMeters; }
}
