package br.ufla.gat108.personaltracker.math;

/**
 * Matriz densa imutável de double. Existe só para a reconciliação:
 * multiplicação, transposta e inversão por Gauss-Jordan com pivoteamento parcial.
 */
public final class Matrix {

    private final double[][] data;
    private final int rows;
    private final int cols;

    /** Copia os valores. Rejeita nulo, vazio, linhas de tamanhos diferentes e valores não finitos. */
    public Matrix(double[][] values) {
        if (values == null || values.length == 0 || values[0] == null || values[0].length == 0) {
            throw new IllegalArgumentException("Matriz nula ou vazia.");
        }
        this.rows = values.length;
        this.cols = values[0].length;
        this.data = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            if (values[i] == null || values[i].length != cols) {
                throw new IllegalArgumentException("Todas as linhas devem ter o mesmo tamanho.");
            }
            for (int j = 0; j < cols; j++) {
                if (Double.isNaN(values[i][j]) || Double.isInfinite(values[i][j])) {
                    throw new IllegalArgumentException("Valor não finito na posição [" + i + "][" + j + "].");
                }
                data[i][j] = values[i][j];
            }
        }
    }

    public static Matrix identity(int n) {
        double[][] d = new double[n][n];
        for (int i = 0; i < n; i++) {
            d[i][i] = 1.0;
        }
        return new Matrix(d);
    }

    public static Matrix diagonal(double[] diagonal) {
        int n = diagonal.length;
        double[][] d = new double[n][n];
        for (int i = 0; i < n; i++) {
            d[i][i] = diagonal[i];
        }
        return new Matrix(d);
    }

    public int rows() { return rows; }

    public int cols() { return cols; }

    public double get(int i, int j) { return data[i][j]; }

    /** Cópia profunda dos valores. */
    public double[][] toArray() {
        double[][] copy = new double[rows][];
        for (int i = 0; i < rows; i++) {
            copy[i] = data[i].clone();
        }
        return copy;
    }

    public Matrix transpose() {
        double[][] t = new double[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                t[j][i] = data[i][j];
            }
        }
        return new Matrix(t);
    }

    public Matrix multiply(Matrix other) {
        if (cols != other.rows) {
            throw new IllegalArgumentException("Dimensões incompatíveis: " + rows + "x" + cols
                    + " por " + other.rows + "x" + other.cols);
        }
        double[][] result = new double[rows][other.cols];
        for (int i = 0; i < rows; i++) {
            for (int k = 0; k < cols; k++) {
                double aik = data[i][k];
                for (int j = 0; j < other.cols; j++) {
                    result[i][j] += aik * other.data[k][j];
                }
            }
        }
        return new Matrix(result);
    }

    /** Produto matriz-vetor. */
    public double[] multiply(double[] vector) {
        if (vector.length != cols) {
            throw new IllegalArgumentException("Vetor de tamanho " + vector.length
                    + " incompatível com matriz de " + cols + " colunas.");
        }
        double[] result = new double[rows];
        for (int i = 0; i < rows; i++) {
            double sum = 0.0;
            for (int j = 0; j < cols; j++) {
                sum += data[i][j] * vector[j];
            }
            result[i] = sum;
        }
        return result;
    }

    /** Inversa por Gauss-Jordan com pivoteamento parcial. Lança SingularMatrixException se não existir. */
    public Matrix inverse() {
        if (rows != cols) {
            throw new IllegalArgumentException("Só matrizes quadradas têm inversa: " + rows + "x" + cols);
        }
        int n = rows;
        double scale = 0.0;
        double[][] aug = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                aug[i][j] = data[i][j];
                scale = Math.max(scale, Math.abs(data[i][j]));
            }
            aug[i][n + i] = 1.0;
        }
        double tolerance = 1e-12 * scale;

        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int r = col + 1; r < n; r++) {
                if (Math.abs(aug[r][col]) > Math.abs(aug[pivot][col])) {
                    pivot = r;
                }
            }
            if (Math.abs(aug[pivot][col]) <= tolerance) {
                throw new SingularMatrixException("Matriz singular: pivô nulo na coluna " + col + ".");
            }
            double[] tmp = aug[col];
            aug[col] = aug[pivot];
            aug[pivot] = tmp;

            double p = aug[col][col];
            for (int j = 0; j < 2 * n; j++) {
                aug[col][j] /= p;
            }
            for (int r = 0; r < n; r++) {
                if (r != col) {
                    double f = aug[r][col];
                    if (f != 0.0) {
                        for (int j = 0; j < 2 * n; j++) {
                            aug[r][j] -= f * aug[col][j];
                        }
                    }
                }
            }
        }

        double[][] inv = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(aug[i], n, inv[i], 0, n);
        }
        return new Matrix(inv);
    }
}
