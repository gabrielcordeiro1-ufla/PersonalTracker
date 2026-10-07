package br.ufla.gat108.personaltracker.domain;

/**
 * Uma leitura de posicionamento, imutável. Preserva a incerteza que o próprio
 * sensor informa, porque é ela que alimenta a matriz de covariância (capacidade 2).
 *
 * Unidades: timestamp em ms; latitude/longitude em graus; velocity e speedAccuracy
 * em m/s; accuracy em metros (horizontal); bearing em graus, de 0 (inclusive) a 360 (exclusive).
 */
public final class Fix {
    private final long timestamp;
    private final double latitude;
    private final double longitude;
    private final float velocity;
    private final float accuracy;
    private final float speedAccuracy;
    private final float bearing;

    public Fix(long timestamp, double latitude, double longitude,
               float velocity, float accuracy, float speedAccuracy, float bearing) {
        this.timestamp = Validation.timestamp(timestamp);
        this.latitude = Validation.latitude(latitude);
        this.longitude = Validation.longitude(longitude);
        this.velocity = Validation.nonNegative(velocity, "Velocidade");
        this.accuracy = Validation.nonNegative(accuracy, "Acurácia");
        this.speedAccuracy = Validation.nonNegative(speedAccuracy, "Acurácia de velocidade");
        if (Float.isNaN(bearing) || bearing < 0f || bearing >= 360f) {
            throw new InvalidCoordinatesException(
                    "Rumo inválido: deve estar em [0, 360). Valor recebido: " + bearing);
        }
        this.bearing = bearing;
    }

    public long getTimestamp() { return timestamp; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public float getVelocity() { return velocity; }
    public float getAccuracy() { return accuracy; }
    public float getSpeedAccuracy() { return speedAccuracy; }
    public float getBearing() { return bearing; }
}
