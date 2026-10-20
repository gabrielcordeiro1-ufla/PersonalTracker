package br.ufla.gat108.personaltracker.acquisition;

import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.domain.InvalidCoordinatesException;

/**
 * Converte os campos de uma leitura do sensor em um Fix, sem depender de classes do Android
 * (assim roda nos testes de unidade). A camada Android só extrai os campos do Location.
 *
 * Regras (D-20): se o sensor não informa a acurácia de velocidade, ou informa 0 (é o que o
 * Android devolve quando não tem), usa-se a estimativa medida nas caminhadas. Sem isso a
 * covariância confiaria cegamente na velocidade. Rumo ou velocidade ausentes viram 0.
 */
public final class FixFactory {

    /** Acurácia de velocidade estimada nos dados reais (D-20), em m/s. */
    public static final float DEFAULT_SPEED_ACCURACY = 0.114f;

    private FixFactory() {
    }

    /** @throws InvalidCoordinatesException se a leitura não tem acurácia ou tem valores inválidos */
    public static Fix fromSensor(long timeMs, double latitude, double longitude,
                                 boolean hasSpeed, float speed,
                                 boolean hasAccuracy, float accuracy,
                                 boolean hasSpeedAccuracy, float speedAccuracy,
                                 boolean hasBearing, float bearing) {
        if (!hasAccuracy) {
            throw new InvalidCoordinatesException("Leitura sem acurácia: não dá para montar a covariância.");
        }
        float usedSpeedAccuracy = (hasSpeedAccuracy && speedAccuracy > 0f) ? speedAccuracy : DEFAULT_SPEED_ACCURACY;

        float usedBearing = 0f;
        if (hasBearing) {
            usedBearing = ((bearing % 360f) + 360f) % 360f;
            if (usedBearing >= 360f) {
                usedBearing = 0f;
            }
        }
        return new Fix(timeMs, latitude, longitude, hasSpeed ? speed : 0f, accuracy, usedSpeedAccuracy, usedBearing);
    }
}
