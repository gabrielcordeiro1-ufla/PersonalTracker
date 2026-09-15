package br.ufla.gat108.personaltracker.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Track {
    private final List<Fix> fixes;
    private double totalDistanceMeters;
    private Fix lastFix;

    public Track() {
        this.fixes = new ArrayList<>();
        this.totalDistanceMeters = 0.0;
        this.lastFix = null;
    }

    public void addFix(Fix fix) {
        if (fix == null) return;

        // Se já existe um ponto anterior, calcula a distância e soma ao total
        if (lastFix != null) {
            totalDistanceMeters += calculateDistance(lastFix, fix);
        }

        fixes.add(fix);
        this.lastFix = fix;
    }

    // O comprimento é devolvido em memória constante, sem varrer a lista!
    public double getLength() {
        return totalDistanceMeters;
    }

    public List<Fix> getFixes() {
        return Collections.unmodifiableList(fixes);
    }

    // Fórmula de Haversine para calcular a distância em metros entre duas coordenadas
    // Feito em Java puro para não depender da biblioteca do Android na camada de domínio
    private double calculateDistance(Fix f1, Fix f2) {
        final double R = 6371e3; // Raio da Terra em metros
        double lat1 = Math.toRadians(f1.getLatitude());
        double lat2 = Math.toRadians(f2.getLatitude());
        double deltaLat = Math.toRadians(f2.getLatitude() - f1.getLatitude());
        double deltaLon = Math.toRadians(f2.getLongitude() - f1.getLongitude());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}