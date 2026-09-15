package br.ufla.gat108.personaltracker.domain;

public final class Fix {
    private final long timestamp;
    private final double latitude;
    private final double longitude;
    private final float velocity;
    private final float accuracy;

    public Fix(long timestamp, double latitude, double longitude, float velocity, float accuracy) {
        // Validação das Invariantes do Domínio
        if (latitude < -90.0 || latitude > 90.0) {
            throw new InvalidCoordinatesException("Latitude inválida: deve estar entre -90 e 90. Valor recebido: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new InvalidCoordinatesException("Longitude inválida: deve estar entre -180 e 180. Valor recebido: " + longitude);
        }
        if (accuracy < 0) {
            throw new InvalidCoordinatesException("Acurácia não pode ser negativa.");
        }
        if (velocity < 0) {
            throw new InvalidCoordinatesException("Velocidade não pode ser negativa.");
        }

        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.velocity = velocity;
        this.accuracy = accuracy;
    }

    // Métodos de acesso (Getters)
    public long getTimestamp() { return timestamp; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public float getVelocity() { return velocity; }
    public float getAccuracy() { return accuracy; }
}