package br.ufla.gat108.personaltracker.reconciliation;

import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.domain.GeoMath;

import java.util.List;

/**
 * Monta, para cada eixo (leste e norte), o problema de reconciliação de uma janela de fixes.
 *
 * Variáveis do eixo: [x_0 ... x_{n-1}, v_0 ... v_{n-1}] (posições e velocidades medidas).
 * Restrição física (regra do trapézio), uma por intervalo k:
 *     x_{k+1} - x_k - (v_k + v_{k+1}) / 2 * dt_k = 0
 * É linear nas medidas. A covariância é diagonal: o desvio da posição vem da acurácia
 * horizontal e o da velocidade vem da acurácia de velocidade, ambas informadas pelo sensor.
 */
final class KinematicModel {

    static final int EAST = 0;
    static final int NORTH = 1;

    /** Problema de um eixo: y (medidas), v (covariância) e a (restrições). */
    static final class Axis {
        final double[] y;
        final double[][] v;
        final double[][] a;

        Axis(double[] y, double[][] v, double[][] a) {
            this.y = y;
            this.v = v;
            this.a = a;
        }
    }

    final int fixCount;
    final double originLat;
    final double originLon;
    final Axis[] axes;

    private KinematicModel(int fixCount, double originLat, double originLon, Axis[] axes) {
        this.fixCount = fixCount;
        this.originLat = originLat;
        this.originLon = originLon;
        this.axes = axes;
    }

    static KinematicModel build(List<Fix> window, double positionOptimism, double speedOptimism) {
        if (window == null || window.size() < 2) {
            throw new IllegalArgumentException("A janela precisa de pelo menos 2 fixes.");
        }
        int n = window.size();
        double originLat = window.get(0).getLatitude();
        double originLon = window.get(0).getLongitude();

        double[][] y = new double[2][2 * n];
        double[][] variance = new double[2][2 * n];
        for (int k = 0; k < n; k++) {
            Fix f = window.get(k);
            double bearing = Math.toRadians(f.getBearing());
            // rumo medido a partir do norte, no sentido horário
            double ve = f.getVelocity() * Math.sin(bearing);
            double vn = f.getVelocity() * Math.cos(bearing);

            y[EAST][k] = GeoMath.eastMeters(originLat, originLon, f.getLongitude());
            y[NORTH][k] = GeoMath.northMeters(originLat, f.getLatitude());
            y[EAST][n + k] = ve;
            y[NORTH][n + k] = vn;

            double sp = NoiseCharacterizer.positionSigma(f.getAccuracy(), positionOptimism);
            double sv = NoiseCharacterizer.speedSigma(f.getSpeedAccuracy(), speedOptimism);
            for (int axis = 0; axis < 2; axis++) {
                variance[axis][k] = sp * sp;
                variance[axis][n + k] = sv * sv;
            }
        }

        double[][] a = new double[n - 1][2 * n];
        for (int k = 0; k < n - 1; k++) {
            // timestamps em milissegundos, dt em segundos
            double dt = (window.get(k + 1).getTimestamp() - window.get(k).getTimestamp()) / 1000.0;
            a[k][k + 1] = 1.0;
            a[k][k] = -1.0;
            a[k][n + k] = -dt / 2.0;
            a[k][n + k + 1] = -dt / 2.0;
        }

        Axis[] axes = new Axis[2];
        for (int axis = 0; axis < 2; axis++) {
            double[][] v = new double[2 * n][2 * n];
            for (int i = 0; i < 2 * n; i++) {
                v[i][i] = variance[axis][i];
            }
            axes[axis] = new Axis(y[axis], v, a);
        }
        return new KinematicModel(n, originLat, originLon, axes);
    }
}
