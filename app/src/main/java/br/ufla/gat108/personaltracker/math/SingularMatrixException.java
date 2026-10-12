package br.ufla.gat108.personaltracker.math;

/** A matriz não tem inversa (pivô nulo ou praticamente nulo na eliminação). */
public class SingularMatrixException extends RuntimeException {
    public SingularMatrixException(String message) {
        super(message);
    }
}
