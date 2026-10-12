package br.ufla.gat108.personaltracker.reconciliation;

import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.domain.GeoMath;

import java.util.Arrays;
import java.util.List;

/**
 * Incerteza do sensor: converte a acurácia que o GPS informa em desvio-padrão e
 * mede, com o aparelho parado, o quanto o sensor é otimista.
 *
 * O Android define a acurácia horizontal como o raio de 68% de confiança. Para um erro
 * 2D isotrópico, esse raio vale 1,51 vezes o desvio-padrão por eixo, por isso a conversão.
 */
public final class NoiseCharacterizer {

    /** 1 / sqrt(-2 ln(1 - 0,68)): desvio-padrão por eixo = acurácia * esta constante. */
    public static final double ACCURACY_TO_SIGMA = 1.0 / Math.sqrt(-2.0 * Math.log(1.0 - 0.68));

    /** Piso para nunca gerar variância nula (que tornaria a covariância singular). */
    public static final double MIN_POSITION_SIGMA = 0.01;
    public static final double MIN_SPEED_SIGMA = 0.01;

    /** Desvio-padrão (m) de cada eixo da posição, a partir da acurácia informada. */
    public static double positionSigma(float accuracy, double optimismRatio) {
        return Math.max(accuracy * optimismRatio * ACCURACY_TO_SIGMA, MIN_POSITION_SIGMA);
    }

    /** Desvio-padrão (m/s) de cada componente da velocidade, a partir da acurácia de velocidade. */
    public static double speedSigma(float speedAccuracy, double optimismRatio) {
        return Math.max(speedAccuracy * optimismRatio, MIN_SPEED_SIGMA);
    }

    /**
     * Analisa leituras de um aparelho parado.
     * razão de otimismo = erro observado (percentil 68) / acurácia declarada.
     * Maior que 1: o sensor promete mais do que entrega.
     */
    public NoiseProfile characterize(List<Fix> staticReadings) {
        if (staticReadings == null || staticReadings.size() < 2) {
            throw new IllegalArgumentException("São necessárias pelo menos 2 leituras estáticas.");
        }
        int n = staticReadings.size();

        double meanLat = 0.0;
        double meanLon = 0.0;
        double meanAccuracy = 0.0;
        double meanSpeedAccuracy = 0.0;
        for (Fix f : staticReadings) {
            meanLat += f.getLatitude();
            meanLon += f.getLongitude();
            meanAccuracy += f.getAccuracy();
            meanSpeedAccuracy += f.getSpeedAccuracy();
        }
        meanLat /= n;
        meanLon /= n;
        meanAccuracy /= n;
        meanSpeedAccuracy /= n;
        if (meanAccuracy <= 0.0 || meanSpeedAccuracy <= 0.0) {
            throw new IllegalArgumentException("Acurácia declarada média nula: não dá para calcular a razão.");
        }

        double[] east = new double[n];
        double[] north = new double[n];
        double[] distances = new double[n];
        double[] speeds = new double[n];
        double sumE2 = 0.0;
        double sumN2 = 0.0;
        for (int i = 0; i < n; i++) {
            Fix f = staticReadings.get(i);
            east[i] = GeoMath.eastMeters(meanLat, meanLon, f.getLongitude());
            north[i] = GeoMath.northMeters(meanLat, f.getLatitude());
            distances[i] = Math.hypot(east[i], north[i]);
            speeds[i] = f.getVelocity();
            sumE2 += east[i] * east[i];
            sumN2 += north[i] * north[i];
        }

        double radius68 = percentile68(distances);
        double speed68 = percentile68(speeds);

        return new NoiseProfile(n, meanLat, meanLon,
                Math.sqrt(sumE2 / (n - 1)), Math.sqrt(sumN2 / (n - 1)),
                radius68, meanAccuracy, radius68 / meanAccuracy,
                speed68, meanSpeedAccuracy, speed68 / meanSpeedAccuracy,
                east, north);
    }

    /** Percentil 68 pelo método do posto mais próximo. */
    private static double percentile68(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int index = (int) Math.ceil(0.68 * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(index, sorted.length - 1))];
    }

    /** Resultado da caracterização. Imutável. */
    public static final class NoiseProfile {
        private final int count;
        private final double meanLatitude;
        private final double meanLongitude;
        private final double stdEastMeters;
        private final double stdNorthMeters;
        private final double radius68Meters;
        private final double meanDeclaredAccuracy;
        private final double positionOptimismRatio;
        private final double speed68;
        private final double meanDeclaredSpeedAccuracy;
        private final double speedOptimismRatio;
        private final double[] eastOffsets;
        private final double[] northOffsets;

        NoiseProfile(int count, double meanLatitude, double meanLongitude,
                     double stdEastMeters, double stdNorthMeters,
                     double radius68Meters, double meanDeclaredAccuracy, double positionOptimismRatio,
                     double speed68, double meanDeclaredSpeedAccuracy, double speedOptimismRatio,
                     double[] eastOffsets, double[] northOffsets) {
            this.count = count;
            this.meanLatitude = meanLatitude;
            this.meanLongitude = meanLongitude;
            this.stdEastMeters = stdEastMeters;
            this.stdNorthMeters = stdNorthMeters;
            this.radius68Meters = radius68Meters;
            this.meanDeclaredAccuracy = meanDeclaredAccuracy;
            this.positionOptimismRatio = positionOptimismRatio;
            this.speed68 = speed68;
            this.meanDeclaredSpeedAccuracy = meanDeclaredSpeedAccuracy;
            this.speedOptimismRatio = speedOptimismRatio;
            this.eastOffsets = eastOffsets;
            this.northOffsets = northOffsets;
        }

        public int getCount() { return count; }
        public double getMeanLatitude() { return meanLatitude; }
        public double getMeanLongitude() { return meanLongitude; }
        public double getStdEastMeters() { return stdEastMeters; }
        public double getStdNorthMeters() { return stdNorthMeters; }
        public double getRadius68Meters() { return radius68Meters; }
        public double getMeanDeclaredAccuracy() { return meanDeclaredAccuracy; }
        public double getPositionOptimismRatio() { return positionOptimismRatio; }
        public double getSpeed68() { return speed68; }
        public double getMeanDeclaredSpeedAccuracy() { return meanDeclaredSpeedAccuracy; }
        public double getSpeedOptimismRatio() { return speedOptimismRatio; }

        /** Desvios para leste (m) em relação à média: eixo x do gráfico de dispersão. */
        public double[] getEastOffsets() { return eastOffsets.clone(); }

        /** Desvios para norte (m) em relação à média: eixo y do gráfico de dispersão. */
        public double[] getNorthOffsets() { return northOffsets.clone(); }
    }
}
