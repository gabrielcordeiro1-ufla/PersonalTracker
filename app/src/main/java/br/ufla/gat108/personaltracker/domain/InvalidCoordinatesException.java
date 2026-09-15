package br.ufla.gat108.personaltracker.domain;

public class InvalidCoordinatesException extends IllegalArgumentException {
    public InvalidCoordinatesException(String message) {
        super(message);
    }
}