package br.ufla.gat108.personaltracker.contracts;

/**
 * Contrato para a reconciliação de dados.
 * Aplica multiplicadores de Lagrange para ajustar as medições (y)
 * com base na matriz de covariância (v) e nas restrições físicas (a).
 */
public interface Reconciler {
    double[] reconcile(double[] y, double[][] v, double[][] a);
}