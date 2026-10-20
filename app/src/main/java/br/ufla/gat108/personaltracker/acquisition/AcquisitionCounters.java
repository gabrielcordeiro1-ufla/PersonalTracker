package br.ufla.gat108.personaltracker.acquisition;

import br.ufla.gat108.personaltracker.domain.Fix;

/**
 * Contadores e medidas da aquisição. Escritos pela thread periódica e pelo callback do GPS,
 * lidos pela interface: todos os métodos são sincronizados (a taxa é de 1 Hz, o custo é nulo).
 */
public final class AcquisitionCounters {

    private long cycles;
    private long acceptedFixes;
    private long rejectedFixes;     // fora de ordem, descartados pelo buffer
    private long sensorRejected;    // leituras inválidas, descartadas na conversão
    private long dropped;          // fila cheia
    private long errors;
    private long logErrors;
    private long lastCycleNanos;
    private long maxCycleNanos;
    private double lastIntervalMs;
    private double minIntervalMs = Double.NaN;
    private double maxIntervalMs;
    private long lastFixTimestamp = -1;
    private float lastAccuracy = Float.NaN;
    private float lastSpeedAccuracy = Float.NaN;

    public synchronized void recordCycle(int accepted, int rejected, long cycleNanos,
                                         double intervalMs, Fix lastAccepted) {
        cycles++;
        acceptedFixes += accepted;
        rejectedFixes += rejected;
        lastCycleNanos = cycleNanos;
        maxCycleNanos = Math.max(maxCycleNanos, cycleNanos);
        if (cycles > 1) {   // o primeiro ciclo não tem ciclo anterior
            lastIntervalMs = intervalMs;
            minIntervalMs = Double.isNaN(minIntervalMs) ? intervalMs : Math.min(minIntervalMs, intervalMs);
            maxIntervalMs = Math.max(maxIntervalMs, intervalMs);
        }
        if (lastAccepted != null) {
            lastFixTimestamp = lastAccepted.getTimestamp();
            lastAccuracy = lastAccepted.getAccuracy();
            lastSpeedAccuracy = lastAccepted.getSpeedAccuracy();
        }
    }

    public synchronized void recordSensorRejected() { sensorRejected++; }

    public synchronized void recordDropped() { dropped++; }

    public synchronized void recordError() { errors++; }

    public synchronized void recordLogError() { logErrors++; }

    public synchronized Snapshot snapshot() {
        return new Snapshot(cycles, acceptedFixes, rejectedFixes, sensorRejected, dropped, errors, logErrors,
                lastCycleNanos, maxCycleNanos, lastIntervalMs, Double.isNaN(minIntervalMs) ? 0.0 : minIntervalMs,
                maxIntervalMs, lastFixTimestamp, lastAccuracy, lastSpeedAccuracy);
    }

    /** Foto imutável dos contadores. */
    public static final class Snapshot {
        public final long cycles;
        public final long acceptedFixes;
        public final long rejectedFixes;
        public final long sensorRejected;
        public final long dropped;
        public final long errors;
        public final long logErrors;
        public final long lastCycleNanos;
        public final long maxCycleNanos;
        public final double lastIntervalMs;
        public final double minIntervalMs;
        public final double maxIntervalMs;
        public final long lastFixTimestamp;
        public final float lastAccuracy;
        public final float lastSpeedAccuracy;

        Snapshot(long cycles, long acceptedFixes, long rejectedFixes, long sensorRejected, long dropped,
                 long errors, long logErrors, long lastCycleNanos, long maxCycleNanos,
                 double lastIntervalMs, double minIntervalMs, double maxIntervalMs,
                 long lastFixTimestamp, float lastAccuracy, float lastSpeedAccuracy) {
            this.cycles = cycles;
            this.acceptedFixes = acceptedFixes;
            this.rejectedFixes = rejectedFixes;
            this.sensorRejected = sensorRejected;
            this.dropped = dropped;
            this.errors = errors;
            this.logErrors = logErrors;
            this.lastCycleNanos = lastCycleNanos;
            this.maxCycleNanos = maxCycleNanos;
            this.lastIntervalMs = lastIntervalMs;
            this.minIntervalMs = minIntervalMs;
            this.maxIntervalMs = maxIntervalMs;
            this.lastFixTimestamp = lastFixTimestamp;
            this.lastAccuracy = lastAccuracy;
            this.lastSpeedAccuracy = lastSpeedAccuracy;
        }
    }
}
