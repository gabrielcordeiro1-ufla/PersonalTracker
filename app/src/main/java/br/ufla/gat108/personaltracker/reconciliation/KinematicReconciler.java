package br.ufla.gat108.personaltracker.reconciliation;

import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.domain.GeoMath;

import java.util.ArrayList;
import java.util.List;

/**
 * Reconcilia uma janela de fixes impondo a restrição cinemática: a velocidade integrada
 * no tempo tem de igualar o deslocamento medido. Opcionalmente passa antes pelo
 * GrossErrorDetector, que retira fixes com erro grosseiro.
 *
 * A covariância usa a acurácia informada pelo sensor, corrigida pela razão de otimismo
 * medida no NoiseCharacterizer (1,0 = confiar na acurácia declarada).
 */
public final class KinematicReconciler {

    private final WeightedLeastSquaresReconciler wls = new WeightedLeastSquaresReconciler();
    private final GrossErrorDetector detector;
    private final double positionOptimism;
    private final double speedOptimism;

    public KinematicReconciler() {
        this(1.0, 1.0, GrossErrorDetector.DEFAULT_THRESHOLD, GrossErrorDetector.DEFAULT_MAX_REMOVALS);
    }

    public KinematicReconciler(double positionOptimism, double speedOptimism,
                               double threshold, int maxRemovals) {
        this.detector = new GrossErrorDetector(threshold, maxRemovals, positionOptimism, speedOptimism);
        this.positionOptimism = positionOptimism;
        this.speedOptimism = speedOptimism;
    }

    public Result reconcile(List<Fix> window, boolean useDetector) {
        if (window == null || window.size() < 2) {
            throw new IllegalArgumentException("A janela precisa de pelo menos 2 fixes.");
        }

        List<Integer> removed = useDetector ? detector.findSuspects(window) : new ArrayList<Integer>();
        List<Integer> kept = new ArrayList<>();
        List<Fix> fixes = new ArrayList<>();
        for (int i = 0; i < window.size(); i++) {
            if (!removed.contains(i)) {
                kept.add(i);
                fixes.add(window.get(i));
            }
        }

        KinematicModel model = KinematicModel.build(fixes, positionOptimism, speedOptimism);
        int n = model.fixCount;

        double sumBefore = 0.0;
        double sumAfter = 0.0;
        double chi = 0.0;
        int constraints = 0;
        double[][] adjusted = new double[2][];
        for (int axis = 0; axis < 2; axis++) {
            KinematicModel.Axis p = model.axes[axis];
            double[] rBefore = wls.residual(p.y, p.a);
            adjusted[axis] = wls.reconcile(p.y, p.v, p.a);
            double[] rAfter = wls.residual(adjusted[axis], p.a);
            for (int i = 0; i < rBefore.length; i++) {
                sumBefore += rBefore[i] * rBefore[i];
                sumAfter += rAfter[i] * rAfter[i];
            }
            constraints += rBefore.length;
            chi += wls.chiSquare(p.y, p.v, p.a);
        }

        double[] lat = new double[n];
        double[] lon = new double[n];
        for (int k = 0; k < n; k++) {
            lat[k] = GeoMath.latitudeFromNorth(model.originLat, adjusted[KinematicModel.NORTH][k]);
            lon[k] = GeoMath.longitudeFromEast(model.originLat, model.originLon, adjusted[KinematicModel.EAST][k]);
        }

        return new Result(kept, removed, lat, lon,
                Math.sqrt(sumBefore / constraints), Math.sqrt(sumAfter / constraints), chi, constraints);
    }

    /** Resultado de uma reconciliação. Imutável: os getters devolvem cópias. */
    public static final class Result {
        private final List<Integer> keptIndices;
        private final List<Integer> removedIndices;
        private final double[] latitudes;
        private final double[] longitudes;
        private final double residualRmsBefore;
        private final double residualRmsAfter;
        private final double chiSquare;
        private final int degreesOfFreedom;

        Result(List<Integer> keptIndices, List<Integer> removedIndices,
               double[] latitudes, double[] longitudes,
               double residualRmsBefore, double residualRmsAfter,
               double chiSquare, int degreesOfFreedom) {
            this.keptIndices = new ArrayList<>(keptIndices);
            this.removedIndices = new ArrayList<>(removedIndices);
            this.latitudes = latitudes;
            this.longitudes = longitudes;
            this.residualRmsBefore = residualRmsBefore;
            this.residualRmsAfter = residualRmsAfter;
            this.chiSquare = chiSquare;
            this.degreesOfFreedom = degreesOfFreedom;
        }

        /** Índices, na janela original, dos fixes que entraram na reconciliação. */
        public List<Integer> getKeptIndices() { return new ArrayList<>(keptIndices); }

        /** Índices, na janela original, dos fixes removidos como erro grosseiro. */
        public List<Integer> getRemovedIndices() { return new ArrayList<>(removedIndices); }

        /** Latitudes reconciliadas, na ordem de getKeptIndices(). */
        public double[] getLatitudes() { return latitudes.clone(); }

        /** Longitudes reconciliadas, na ordem de getKeptIndices(). */
        public double[] getLongitudes() { return longitudes.clone(); }

        /** Resíduo RMS (m) das restrições antes de ajustar, nos fixes mantidos. */
        public double getResidualRmsBefore() { return residualRmsBefore; }

        /** Resíduo RMS (m) depois de ajustar. Deve ser praticamente zero. */
        public double getResidualRmsAfter() { return residualRmsAfter; }

        /** Qui-quadrado dos dados contra as restrições. */
        public double getChiSquare() { return chiSquare; }

        public int getDegreesOfFreedom() { return degreesOfFreedom; }

        /** Qui-quadrado por grau de liberdade. Perto de 1 = acurácias declaradas coerentes; bem acima = sensor otimista. */
        public double getNormalizedChiSquare() { return chiSquare / degreesOfFreedom; }
    }
}
