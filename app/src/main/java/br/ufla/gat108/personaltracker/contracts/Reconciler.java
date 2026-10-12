package br.ufla.gat108.personaltracker.contracts;

/**
 * Contrato para a reconciliação de dados.
 * Ajusta as medições (y) com base na matriz de covariância (v) e nas restrições
 * lineares (a), de modo que a * y_ajustado = 0.
 */
public interface Reconciler {

    /** Devolve as medições ajustadas: y - V Aᵀ (A V Aᵀ)⁻¹ A y. */
    double[] reconcile(double[] y, double[][] v, double[][] a);

    /** Resíduo das restrições: A y. Reconciliar corretamente leva este vetor a zero. */
    double[] residual(double[] y, double[][] a);
}
