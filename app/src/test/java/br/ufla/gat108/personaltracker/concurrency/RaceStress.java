package br.ufla.gat108.personaltracker.concurrency;

import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.domain.GeoMath;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Teste de corrida: UMA thread escreve (como a aquisição) e várias leem em laço fechado
 * (como a interface e o agente), todas ao mesmo tempo, sobre um buffer pequeno para que ele
 * dê muitas voltas. Os fixes têm timestamp = 0, 1, 2, ... e passo constante, então dá para
 * conferir de fora o que qualquer leitura deveria ver:
 *  - a cópia é contígua (sem buracos, sem repetição, sem null) e cabe na capacidade;
 *  - os contadores batem entre si (tamanho, total, comprimento, velocidade média);
 *  - total e tamanho nunca diminuem, mesmo que o leitor esvazie a própria cópia.
 */
final class RaceStress {

    private static final double LAT0 = -21.2255;
    private static final double LON0 = -44.9782;
    private static final double STEP_DEGREES = 1e-6;

    private RaceStress() {
    }

    static final class Result {
        final long writes;
        final long reads;
        final int violations;
        final String firstViolation;
        final long millis;

        Result(long writes, long reads, int violations, String firstViolation, long millis) {
            this.writes = writes;
            this.reads = reads;
            this.violations = violations;
            this.firstViolation = firstViolation;
            this.millis = millis;
        }
    }

    static Result run(BufferUnderTest buffer, int writes, int readers) throws Exception {
        final List<Fix> fixes = new ArrayList<>(writes);
        final double[] expectedLength = new double[writes];
        for (int k = 0; k < writes; k++) {
            fixes.add(new Fix(k, LAT0 + k * STEP_DEGREES, LON0, 1.0f, 3f, 0.1f, 0f));
            if (k > 0) {
                // mesma conta e mesma ordem de soma do Track
                expectedLength[k] = expectedLength[k - 1] + GeoMath.distanceMeters(
                        fixes.get(k - 1).getLatitude(), fixes.get(k - 1).getLongitude(),
                        fixes.get(k).getLatitude(), fixes.get(k).getLongitude());
            }
        }

        final AtomicBoolean writerDone = new AtomicBoolean(false);
        final AtomicInteger violations = new AtomicInteger();
        final AtomicReference<String> first = new AtomicReference<>();
        final AtomicLong reads = new AtomicLong();
        final CountDownLatch start = new CountDownLatch(1);

        ExecutorService pool = Executors.newFixedThreadPool(readers + 1);
        long t0 = System.nanoTime();
        try {
            List<Future<?>> futures = new ArrayList<>();
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    int n = 0;
                    for (Fix f : fixes) {
                        buffer.add(f);
                        if ((++n & 255) == 0) {
                            Thread.yield();   // garante intercalação mesmo em máquina de 1 núcleo
                        }
                    }
                } catch (Throwable t) {
                    violate(violations, first, "escritor lançou " + t);
                } finally {
                    writerDone.set(true);
                }
                return null;
            }));
            for (int r = 0; r < readers; r++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    long prevTotal = -1;
                    int prevSize = -1;
                    boolean last = false;
                    while (!last) {
                        last = writerDone.get();   // uma última verificação depois que o escritor termina
                        try {
                            long[] prev = {prevTotal, prevSize};
                            checkOnce(buffer, expectedLength, prev, violations, first);
                            prevTotal = prev[0];
                            prevSize = (int) prev[1];
                        } catch (Throwable t) {
                            violate(violations, first, "leitor lançou " + t);
                        }
                        reads.incrementAndGet();
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> f : futures) {
                f.get(120, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        long millis = (System.nanoTime() - t0) / 1_000_000L;
        return new Result(writes, reads.get(), violations.get(), first.get(), millis);
    }

    private static void checkOnce(BufferUnderTest buffer, double[] expectedLength, long[] prev,
                                  AtomicInteger violations, AtomicReference<String> first) {
        int capacity = buffer.capacity();

        List<Fix> snap = buffer.snapshot();
        checkContiguous("snapshot", snap, capacity, violations, first);
        snap.clear();   // a cópia é do leitor: esvaziá-la não pode afetar o buffer (conferido pelos contadores)

        List<Fix> last = buffer.latest(5);
        checkContiguous("latest(5)", last, 5, violations, first);

        CircularTrackBuffer.Stats s = buffer.stats();
        long total = s.getTotalAdded();
        if (s.getSize() != Math.min((long) capacity, total)) {
            violate(violations, first, "tamanho " + s.getSize() + " incompatível com total " + total);
        }
        if (total > 0) {
            double expected = expectedLength[(int) (total - 1)];
            if (Math.abs(s.getLengthMeters() - expected) > 1e-9 * Math.max(1.0, expected)) {
                violate(violations, first, "comprimento " + s.getLengthMeters()
                        + " não bate com o esperado " + expected + " para total " + total);
            }
            if (Math.abs(s.getMeanSpeed() - 1.0) > 1e-9) {
                violate(violations, first, "velocidade média " + s.getMeanSpeed() + " em vez de 1,0");
            }
        }
        if (total < prev[0] || s.getSize() < prev[1]) {
            violate(violations, first, "contador diminuiu: total " + prev[0] + " -> " + total
                    + ", tamanho " + prev[1] + " -> " + s.getSize());
        }
        prev[0] = total;
        prev[1] = s.getSize();
    }

    private static void checkContiguous(String what, List<Fix> list, int maxSize,
                                        AtomicInteger violations, AtomicReference<String> first) {
        if (list.size() > maxSize) {
            violate(violations, first, what + " com " + list.size() + " fixes, acima do limite " + maxSize);
        }
        for (int i = 0; i < list.size(); i++) {
            Fix f = list.get(i);
            if (f == null) {
                violate(violations, first, what + " contém null na posição " + i);
                return;
            }
            if (i > 0 && list.get(i - 1) != null && f.getTimestamp() != list.get(i - 1).getTimestamp() + 1) {
                violate(violations, first, what + " não contígua: " + list.get(i - 1).getTimestamp()
                        + " seguido de " + f.getTimestamp());
                return;
            }
        }
    }

    private static void violate(AtomicInteger violations, AtomicReference<String> first, String message) {
        violations.incrementAndGet();
        first.compareAndSet(null, message);
    }
}
