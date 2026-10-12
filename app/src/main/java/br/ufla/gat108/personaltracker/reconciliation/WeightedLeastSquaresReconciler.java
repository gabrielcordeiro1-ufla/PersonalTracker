package br.ufla.gat108.personaltracker.reconciliation;

import br.ufla.gat108.personaltracker.contracts.Reconciler;
import br.ufla.gat108.personaltracker.math.Matrix;

/**
 * Reconciliação de dados por mínimos quadrados ponderados com restrições lineares
 * (solução por multiplicadores de Lagrange):
 *
 *   minimizar (y - ŷ)ᵀ V⁻¹ (y - ŷ)   sujeito a   A ŷ = 0
 *   ŷ = y - V Aᵀ (A V Aᵀ)⁻¹ A y
 *
 * Quanto maior a variância de uma medida, maior a correção que ela recebe.
 */
public final class WeightedLeastSquaresReconciler implements Reconciler {

    @Override
    public double[] reconcile(double[] y, double[][] v, double[][] a) {
        Matrix vm = new Matrix(v);
        Matrix am = new Matrix(a);
        check(y, vm, am);

        Matrix at = am.transpose();
        Matrix s = am.multiply(vm).multiply(at);        // A V Aᵀ
        double[] lambda = s.inverse().multiply(am.multiply(y));
        double[] correction = vm.multiply(at).multiply(lambda);

        double[] adjusted = new double[y.length];
        for (int i = 0; i < y.length; i++) {
            adjusted[i] = y[i] - correction[i];
        }
        return adjusted;
    }

    @Override
    public double[] residual(double[] y, double[][] a) {
        Matrix am = new Matrix(a);
        if (y == null || y.length != am.cols()) {
            throw new IllegalArgumentException("y deve ter tantas posições quanto as colunas de A.");
        }
        return am.multiply(y);
    }

    /** Covariância do resíduo: S = A V Aᵀ. Base do resíduo normalizado. */
    public double[][] residualCovariance(double[][] v, double[][] a) {
        Matrix vm = new Matrix(v);
        Matrix am = new Matrix(a);
        check(new double[am.cols()], vm, am);
        return am.multiply(vm).multiply(am.transpose()).toArray();
    }

    /** Estatística qui-quadrado do resíduo: rᵀ S⁻¹ r. Esperado perto do número de restrições. */
    public double chiSquare(double[] y, double[][] v, double[][] a) {
        Matrix vm = new Matrix(v);
        Matrix am = new Matrix(a);
        check(y, vm, am);

        Matrix s = am.multiply(vm).multiply(am.transpose());
        double[] r = am.multiply(y);
        double[] w = s.inverse().multiply(r);
        double sum = 0.0;
        for (int i = 0; i < r.length; i++) {
            sum += r[i] * w[i];
        }
        return sum;
    }

    private static void check(double[] y, Matrix v, Matrix a) {
        if (y == null) {
            throw new IllegalArgumentException("y nulo.");
        }
        int n = y.length;
        if (v.rows() != n || v.cols() != n) {
            throw new IllegalArgumentException("V deve ser " + n + "x" + n + ", mas é "
                    + v.rows() + "x" + v.cols() + ".");
        }
        if (a.cols() != n) {
            throw new IllegalArgumentException("A deve ter " + n + " colunas, mas tem " + a.cols() + ".");
        }
    }
}
