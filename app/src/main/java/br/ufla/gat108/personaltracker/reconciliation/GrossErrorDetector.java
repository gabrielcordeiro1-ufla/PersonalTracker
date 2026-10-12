package br.ufla.gat108.personaltracker.reconciliation;

import br.ufla.gat108.personaltracker.domain.Fix;

import java.util.ArrayList;
import java.util.List;

/**
 * Detecção de erro grosseiro por resíduo normalizado.
 *
 * Critério de corte: z_i = r_i / sqrt(S_ii), com r = A y e S = A V Aᵀ. Se algum |z_i|
 * passa do limiar, a janela tem erro grosseiro. Para achar QUAL fix é o culpado, testa-se a
 * remoção de cada fix e fica o que mais reduz o qui-quadrado total; repete até a janela
 * passar no teste ou até o máximo de remoções.
 *
 * Atenção: se a acurácia do sensor for otimista, o teste dispara em fixes bons. Calibre a
 * razão de otimismo com o NoiseCharacterizer antes de confiar nas remoções.
 */
public final class GrossErrorDetector {

    /**
     * Limiar do resíduo normalizado. Com 3,0 uma janela limpa de 30 fixes dispara alarme falso
     * em ~17% das vezes (são 58 testes por janela); com 3,5 cai para ~2%. Valores medidos em
     * simulação com semente fixa; a tabela de sensibilidade da capacidade 5 deve confirmar com dados reais.
     */
    public static final double DEFAULT_THRESHOLD = 3.5;
    public static final int DEFAULT_MAX_REMOVALS = 3;

    private final double threshold;
    private final int maxRemovals;
    private final double positionOptimism;
    private final double speedOptimism;
    private final WeightedLeastSquaresReconciler wls = new WeightedLeastSquaresReconciler();

    public GrossErrorDetector() {
        this(DEFAULT_THRESHOLD, DEFAULT_MAX_REMOVALS, 1.0, 1.0);
    }

    public GrossErrorDetector(double threshold, int maxRemovals,
                              double positionOptimism, double speedOptimism) {
        if (!(threshold > 0.0) || maxRemovals < 0 || !(positionOptimism > 0.0) || !(speedOptimism > 0.0)) {
            throw new IllegalArgumentException(
                    "Limiar e razões de otimismo devem ser positivos, e as remoções não negativas.");
        }
        this.threshold = threshold;
        this.maxRemovals = maxRemovals;
        this.positionOptimism = positionOptimism;
        this.speedOptimism = speedOptimism;
    }

    public double getThreshold() { return threshold; }

    /** Maior |z| entre todas as restrições, nos dois eixos. */
    public double maxNormalizedResidual(List<Fix> window) {
        KinematicModel model = KinematicModel.build(window, positionOptimism, speedOptimism);
        double max = 0.0;
        for (KinematicModel.Axis axis : model.axes) {
            double[] r = wls.residual(axis.y, axis.a);
            double[][] s = wls.residualCovariance(axis.v, axis.a);
            for (int i = 0; i < r.length; i++) {
                max = Math.max(max, Math.abs(r[i]) / Math.sqrt(s[i][i]));
            }
        }
        return max;
    }

    /** Índices (na janela original) dos fixes removidos como erro grosseiro, na ordem de remoção. */
    public List<Integer> findSuspects(List<Fix> window) {
        List<Integer> removed = new ArrayList<>();
        if (window == null || window.size() < 4) {
            return removed;
        }
        List<Integer> kept = new ArrayList<>();
        for (int i = 0; i < window.size(); i++) {
            kept.add(i);
        }

        while (removed.size() < maxRemovals && kept.size() > 3) {
            List<Fix> current = subset(window, kept, -1);
            if (maxNormalizedResidual(current) <= threshold) {
                break;
            }
            int bestPosition = -1;
            double bestChi = Double.POSITIVE_INFINITY;
            for (int c = 0; c < kept.size(); c++) {
                double chi = totalChiSquare(subset(window, kept, c));
                if (chi < bestChi) {
                    bestChi = chi;
                    bestPosition = c;
                }
            }
            removed.add(kept.remove(bestPosition));
        }
        return removed;
    }

    private double totalChiSquare(List<Fix> fixes) {
        KinematicModel model = KinematicModel.build(fixes, positionOptimism, speedOptimism);
        double total = 0.0;
        for (KinematicModel.Axis axis : model.axes) {
            total += wls.chiSquare(axis.y, axis.v, axis.a);
        }
        return total;
    }

    /** Fixes de 'indices', sem a posição 'skipPosition' (use -1 para não pular nenhuma). */
    private static List<Fix> subset(List<Fix> window, List<Integer> indices, int skipPosition) {
        List<Fix> result = new ArrayList<>(indices.size());
        for (int p = 0; p < indices.size(); p++) {
            if (p != skipPosition) {
                result.add(window.get(indices.get(p)));
            }
        }
        return result;
    }
}
