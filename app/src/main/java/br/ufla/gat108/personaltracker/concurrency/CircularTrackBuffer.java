package br.ufla.gat108.personaltracker.concurrency;

import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.domain.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Buffer circular COMPARTILHADO entre a thread de aquisição (escreve) e a interface e o
 * agente (leem). O Track guarda o estado e não tem sincronização; esta classe é a região
 * crítica: todo acesso ao Track acontece com o lock tomado.
 *
 * Regras:
 *  - Uma única thread escreve (a de aquisição). Os fixes chegam em ordem de tempo.
 *  - Qualquer número de threads lê.
 *  - Nada devolve referência interna: as consultas devolvem cópias.
 *  - stats() lê todos os contadores sob o mesmo lock, então o resultado é consistente.
 *
 * O trecho protegido é curto: add é O(1); as cópias são O(capacidade).
 */
public final class CircularTrackBuffer {

    private final ReentrantLock lock = new ReentrantLock();
    private final Track track;   // só é tocado com o lock tomado

    public CircularTrackBuffer() {
        this(Track.DEFAULT_CAPACITY);
    }

    public CircularTrackBuffer(int capacity) {
        this.track = new Track(capacity);
    }

    public void add(Fix fix) {
        lock.lock();
        try {
            track.addFix(fix);
        } finally {
            lock.unlock();
        }
    }

    /** Cópia de todos os fixes guardados, do mais antigo ao mais novo. */
    public List<Fix> snapshot() {
        lock.lock();
        try {
            return track.getFixes();
        } finally {
            lock.unlock();
        }
    }

    /** Cópia dos últimos n fixes (menos, se houver menos), do mais antigo ao mais novo. */
    public List<Fix> latest(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n não pode ser negativo: " + n);
        }
        lock.lock();
        try {
            List<Fix> all = track.getFixes();
            return new ArrayList<>(all.subList(Math.max(0, all.size() - n), all.size()));
        } finally {
            lock.unlock();
        }
    }

    /** Cópia dos fixes com timestamp em [fromTimestamp, toTimestamp]. */
    public List<Fix> slice(long fromTimestamp, long toTimestamp) {
        lock.lock();
        try {
            return track.slice(fromTimestamp, toTimestamp);
        } finally {
            lock.unlock();
        }
    }

    /** Contadores lidos de uma vez, de forma consistente entre si. */
    public Stats stats() {
        lock.lock();
        try {
            return new Stats(track.size(), track.getTotalAdded(), track.getLength(), track.getMeanSpeed());
        } finally {
            lock.unlock();
        }
    }

    public int capacity() {
        return track.getCapacity();   // final, nunca muda: não precisa de lock
    }

    /** Foto consistente dos contadores do buffer. Imutável. */
    public static final class Stats {
        private final int size;
        private final long totalAdded;
        private final double lengthMeters;
        private final double meanSpeed;

        // visibilidade de pacote: os testes montam Stats para a versão sem trava
        Stats(int size, long totalAdded, double lengthMeters, double meanSpeed) {
            this.size = size;
            this.totalAdded = totalAdded;
            this.lengthMeters = lengthMeters;
            this.meanSpeed = meanSpeed;
        }

        public int getSize() { return size; }
        public long getTotalAdded() { return totalAdded; }
        public double getLengthMeters() { return lengthMeters; }
        public double getMeanSpeed() { return meanSpeed; }
    }
}
