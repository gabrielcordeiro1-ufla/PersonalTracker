package br.ufla.gat108.personaltracker.domain;

import java.util.Objects;

/** Algo que o sistema detectou em um instante e lugar. Imutável. */
public final class Event {

    public enum Type { GROSS_ERROR, STOP, SPEED_EXCEEDED }

    private final Type type;
    private final long timestamp;
    private final double latitude;
    private final double longitude;
    private final String detail;

    public Event(Type type, long timestamp, double latitude, double longitude, String detail) {
        this.type = Objects.requireNonNull(type, "type");
        this.timestamp = Validation.timestamp(timestamp);
        this.latitude = Validation.latitude(latitude);
        this.longitude = Validation.longitude(longitude);
        this.detail = detail == null ? "" : detail;
    }

    public Type getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getDetail() { return detail; }
}
