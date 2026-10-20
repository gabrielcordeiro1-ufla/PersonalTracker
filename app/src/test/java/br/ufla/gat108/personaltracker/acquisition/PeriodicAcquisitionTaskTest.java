package br.ufla.gat108.personaltracker.acquisition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.ufla.gat108.personaltracker.concurrency.CircularTrackBuffer;
import br.ufla.gat108.personaltracker.contracts.FixSource;
import br.ufla.gat108.personaltracker.domain.Fix;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

public class PeriodicAcquisitionTaskTest {

    private static Fix fix(long t) {
        return new Fix(t, -21.2, -44.9, 1f, 3.5f, 0.2f, 0f);
    }

    /** Fonte falsa: devolve os fixes enfileirados, um a um, e null quando acabam. */
    private static final class FakeSource implements FixSource {
        final Deque<Fix> queue = new ArrayDeque<>();
        RuntimeException failure;

        void offer(Fix... fixes) { queue.addAll(Arrays.asList(fixes)); }

        @Override
        public Fix acquire() {
            if (failure != null) {
                throw failure;
            }
            return queue.poll();
        }
    }

    private static final class Fixture {
        final FakeSource source = new FakeSource();
        final CircularTrackBuffer buffer = new CircularTrackBuffer(16);
        final AcquisitionCounters counters = new AcquisitionCounters();
        final StringWriter logOut = new StringWriter();
        final AtomicLong nano = new AtomicLong(0);
        final AtomicLong wall = new AtomicLong(1_700_000_000_000L);

        PeriodicAcquisitionTask task(Writer out) {
            return new PeriodicAcquisitionTask(source, buffer, counters, new CycleLogWriter(out),
                    nano::get, wall::get);
        }
    }

    @Test
    public void cicloEsvaziaAFonteEEntregaAoBuffer() {
        Fixture f = new Fixture();
        f.source.offer(fix(1000), fix(1500));

        f.task(f.logOut).run();

        assertEquals(2, f.buffer.stats().getSize());
        AcquisitionCounters.Snapshot s = f.counters.snapshot();
        assertEquals(1, s.cycles);
        assertEquals(2, s.acceptedFixes);
        assertEquals(1500L, s.lastFixTimestamp);
    }

    @Test
    public void cicloSemFixContaOCicloENaoQuebra() {
        Fixture f = new Fixture();

        f.task(f.logOut).run();

        AcquisitionCounters.Snapshot s = f.counters.snapshot();
        assertEquals(1, s.cycles);
        assertEquals(0, s.acceptedFixes);
        assertEquals(0, s.errors);
        assertTrue(f.logOut.toString().trim().split(",")[4].equals("0"));   // new_fixes = 0
    }

    @Test
    public void fixForaDeOrdemEDescartadoEContado() {
        Fixture f = new Fixture();
        f.source.offer(fix(2000), fix(1000), fix(3000));

        f.task(f.logOut).run();

        AcquisitionCounters.Snapshot s = f.counters.snapshot();
        assertEquals(2, s.acceptedFixes);
        assertEquals(1, s.rejectedFixes);
        assertEquals(0, s.errors);
        assertEquals(2, f.buffer.stats().getSize());
    }

    @Test
    public void excecaoDaFonteNaoSePropagaEEhContada() {
        Fixture f = new Fixture();
        f.source.failure = new IllegalStateException("sensor quebrou");

        f.task(f.logOut).run();   // não pode lançar

        assertEquals(1, f.counters.snapshot().errors);
        assertEquals(1, f.counters.snapshot().cycles);
    }

    @Test
    public void falhaDeEscritaDoRegistroNaoSePropagaEEhContada() {
        Fixture f = new Fixture();
        f.source.offer(fix(1000));
        Writer broken = new Writer() {
            @Override public void write(char[] c, int o, int l) throws IOException { throw new IOException("disco cheio"); }
            @Override public void flush() throws IOException { throw new IOException("disco cheio"); }
            @Override public void close() { }
        };

        f.task(broken).run();

        assertEquals(1, f.counters.snapshot().logErrors);
        assertEquals(1, f.buffer.stats().getSize());   // a aquisição seguiu
    }

    @Test
    public void medeOIntervaloEntreCiclosEOTempoDoCiclo() {
        Fixture f = new Fixture();
        PeriodicAcquisitionTask task = f.task(f.logOut);

        f.nano.set(5_000_000_000L);
        task.run();
        f.nano.set(6_002_000_000L);   // 1002 ms depois
        task.run();

        AcquisitionCounters.Snapshot s = f.counters.snapshot();
        assertEquals(1002.0, s.lastIntervalMs, 1e-6);
        assertEquals(0L, s.lastCycleNanos);   // relógio falso não avança dentro do ciclo
    }

    @Test
    public void numeroDeSequenciaCrescePorCiclo() {
        Fixture f = new Fixture();
        PeriodicAcquisitionTask task = f.task(f.logOut);
        task.run();
        task.run();
        task.run();

        String[] lines = f.logOut.toString().trim().split("\n");
        assertEquals(3, lines.length);
        assertTrue(lines[0].startsWith("1,"));
        assertTrue(lines[2].startsWith("3,"));
    }

    @Test
    public void sobDoAgendadorRealOPeriodoEhCumpridoEATarefaSobreviveAFalhas() throws Exception {
        Fixture f = new Fixture();
        // a fonte falha em todo ciclo: a tarefa periódica NÃO pode ser cancelada pelo executor
        f.source.failure = new IllegalStateException("falha permanente");
        PeriodicAcquisitionTask task = new PeriodicAcquisitionTask(f.source, f.buffer, f.counters, null,
                System::nanoTime, System::currentTimeMillis);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {
            executor.scheduleAtFixedRate(task, 20, 20, TimeUnit.MILLISECONDS);
            long deadline = System.currentTimeMillis() + 5000;
            while (f.counters.snapshot().cycles < 8 && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
        } finally {
            executor.shutdownNow();
        }

        assertTrue("só " + f.counters.snapshot().cycles + " ciclos", f.counters.snapshot().cycles >= 8);
        assertTrue(f.counters.snapshot().errors >= 8);
    }
}
