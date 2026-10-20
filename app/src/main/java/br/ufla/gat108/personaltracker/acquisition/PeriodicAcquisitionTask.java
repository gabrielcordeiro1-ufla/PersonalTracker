package br.ufla.gat108.personaltracker.acquisition;

import br.ufla.gat108.personaltracker.concurrency.CircularTrackBuffer;
import br.ufla.gat108.personaltracker.contracts.FixSource;
import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.domain.InvalidTrackException;

import java.io.IOException;
import java.util.function.LongSupplier;

/**
 * Tarefa periódica da camada 1 (período fixo de 1 s, agendada com scheduleAtFixedRate).
 * É a ÚNICA thread que escreve no CircularTrackBuffer.
 *
 * A cada ciclo: esvazia a fonte, entrega ao buffer, mede o próprio tempo e o intervalo desde
 * o ciclo anterior (jitter) e escreve uma linha de registro.
 *
 * Nunca propaga exceção: um ScheduledExecutorService cancela em silêncio uma tarefa periódica
 * cuja execução lança, e a aquisição pararia sem aviso.
 */
public final class PeriodicAcquisitionTask implements Runnable {

    private final FixSource source;
    private final CircularTrackBuffer buffer;
    private final AcquisitionCounters counters;
    private final CycleLogWriter log;          // pode ser null
    private final LongSupplier nanoClock;
    private final LongSupplier wallClock;

    private long previousStartNanos = -1;
    private long seq;

    public PeriodicAcquisitionTask(FixSource source, CircularTrackBuffer buffer, AcquisitionCounters counters,
                                   CycleLogWriter log, LongSupplier nanoClock, LongSupplier wallClock) {
        this.source = source;
        this.buffer = buffer;
        this.counters = counters;
        this.log = log;
        this.nanoClock = nanoClock;
        this.wallClock = wallClock;
    }

    @Override
    public void run() {
        try {
            long startNanos = nanoClock.getAsLong();
            long wallMs = wallClock.getAsLong();
            double intervalMs = previousStartNanos < 0 ? 0.0 : (startNanos - previousStartNanos) / 1e6;
            previousStartNanos = startNanos;

            int accepted = 0;
            int rejected = 0;
            Fix lastAccepted = null;
            try {
                Fix fix;
                while ((fix = source.acquire()) != null) {
                    try {
                        buffer.add(fix);
                        accepted++;
                        lastAccepted = fix;
                    } catch (InvalidTrackException e) {
                        rejected++;   // fix fora de ordem (D-10): contado, não derruba a aquisição
                    }
                }
            } catch (RuntimeException e) {
                counters.recordError();
            }

            long cycleNanos = nanoClock.getAsLong() - startNanos;
            counters.recordCycle(accepted, rejected, cycleNanos, intervalMs, lastAccepted);
            seq++;

            if (log != null) {
                try {
                    CircularTrackBuffer.Stats stats = buffer.stats();
                    log.writeCycle(seq, wallMs, intervalMs, cycleNanos, accepted, rejected,
                            lastAccepted, stats.getSize(), stats.getTotalAdded());
                } catch (IOException e) {
                    counters.recordLogError();
                }
            }
        } catch (Throwable t) {
            try {
                counters.recordError();
            } catch (Throwable ignored) {
                // nada mais a fazer: o importante é não propagar
            }
        }
    }
}
