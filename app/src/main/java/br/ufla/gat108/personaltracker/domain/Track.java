package br.ufla.gat108.personaltracker.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Trajeto de capacidade fixa, implementado como buffer circular.
 *
 * Memória: O(capacidade), constante ao longo do tempo. Ao encher, o fix mais
 * antigo é descartado.
 *
 * Complexidade:
 *  - addFix:      O(1)
 *  - getLength:   O(1)  (distância acumulada, inclusive dos fixes já descartados)
 *  - getMeanSpeed O(1)  (média acumulada de todos os fixes recebidos)
 *  - slice:       O(capacidade)
 *  - getFixes:    O(capacidade)
 *
 * Esta classe ainda NÃO é thread-safe: a sincronização entra na capacidade 3.
 * As consultas já devolvem cópias, nunca a referência interna.
 */
public class Track {

    public static final int DEFAULT_CAPACITY = 1024;

    private final Fix[] buffer;
    private int head;   // posição do fix mais antigo
    private int size;   // quantos fixes estão guardados agora
    private long totalAdded;
    private double totalDistanceMeters;
    private double speedSum;
    private Fix lastFix;

    public Track() {
        this(DEFAULT_CAPACITY);
    }

    public Track(int capacity) {
        if (capacity <= 0) {
            throw new InvalidTrackException("Capacidade deve ser positiva. Valor recebido: " + capacity);
        }
        this.buffer = new Fix[capacity];
    }

    public void addFix(Fix fix) {
        if (fix == null) {
            throw new InvalidTrackException("Fix nulo não pode ser adicionado ao trajeto.");
        }
        if (lastFix != null && fix.getTimestamp() < lastFix.getTimestamp()) {
            throw new InvalidTrackException("Fix fora de ordem: timestamp " + fix.getTimestamp()
                    + " é anterior ao último (" + lastFix.getTimestamp() + ").");
        }

        if (lastFix != null) {
            totalDistanceMeters += GeoMath.distanceMeters(
                    lastFix.getLatitude(), lastFix.getLongitude(),
                    fix.getLatitude(), fix.getLongitude());
        }
        speedSum += fix.getVelocity();
        totalAdded++;

        if (size == buffer.length) {
            // cheio: sobrescreve o mais antigo e avança o início
            buffer[head] = fix;
            head = (head + 1) % buffer.length;
        } else {
            buffer[(head + size) % buffer.length] = fix;
            size++;
        }
        lastFix = fix;
    }

    /** Comprimento total percorrido, em metros. Não varre o buffer. */
    public double getLength() {
        return totalDistanceMeters;
    }

    /** Velocidade média (m/s) de todos os fixes recebidos. Zero se vazio. */
    public double getMeanSpeed() {
        return totalAdded == 0 ? 0.0 : speedSum / totalAdded;
    }

    /** Fixes com timestamp em [fromTimestamp, toTimestamp], do mais antigo ao mais novo. */
    public List<Fix> slice(long fromTimestamp, long toTimestamp) {
        if (fromTimestamp > toTimestamp) {
            throw new InvalidTrackException("Intervalo inválido: início (" + fromTimestamp
                    + ") maior que o fim (" + toTimestamp + ").");
        }
        List<Fix> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Fix f = buffer[(head + i) % buffer.length];
            if (f.getTimestamp() >= fromTimestamp && f.getTimestamp() <= toTimestamp) {
                result.add(f);
            }
        }
        return result;
    }

    /** Cópia dos fixes guardados, do mais antigo ao mais novo. */
    public List<Fix> getFixes() {
        List<Fix> copy = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            copy.add(buffer[(head + i) % buffer.length]);
        }
        return copy;
    }

    public int size() { return size; }

    public int getCapacity() { return buffer.length; }

    /** Total de fixes já recebidos, inclusive os descartados. */
    public long getTotalAdded() { return totalAdded; }
}
