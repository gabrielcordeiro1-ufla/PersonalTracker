package br.ufla.gat108.personaltracker.domain;

/**
 * Validações compartilhadas pelas classes do domínio (Fix, Place, Event).
 * Visibilidade de pacote: é detalhe interno, não faz parte do contrato público.
 */
final class Validation {

    private Validation() {
    }

    static double latitude(double value) {
        if (Double.isNaN(value) || value < -90.0 || value > 90.0) {
            throw new InvalidCoordinatesException(
                    "Latitude inválida: deve estar entre -90 e 90. Valor recebido: " + value);
        }
        return value;
    }

    static double longitude(double value) {
        if (Double.isNaN(value) || value < -180.0 || value > 180.0) {
            throw new InvalidCoordinatesException(
                    "Longitude inválida: deve estar entre -180 e 180. Valor recebido: " + value);
        }
        return value;
    }

    /** Aceita zero e positivos finitos. Rejeita negativo, NaN e infinito. */
    static float nonNegative(float value, String name) {
        if (Float.isNaN(value) || Float.isInfinite(value) || value < 0f) {
            throw new InvalidCoordinatesException(
                    name + " inválida: deve ser finita e não negativa. Valor recebido: " + value);
        }
        return value;
    }

    static long timestamp(long value) {
        if (value < 0L) {
            throw new InvalidCoordinatesException(
                    "Timestamp inválido: não pode ser negativo. Valor recebido: " + value);
        }
        return value;
    }
}
