package br.ufla.gat108.personaltracker.domain;

/** Violação de uma invariante da estrutura de trajeto (Track). */
public class InvalidTrackException extends IllegalArgumentException {
    public InvalidTrackException(String message) {
        super(message);
    }
}
