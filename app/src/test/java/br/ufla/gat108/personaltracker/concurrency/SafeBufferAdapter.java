package br.ufla.gat108.personaltracker.concurrency;

import br.ufla.gat108.personaltracker.domain.Fix;

import java.util.List;

/** A implementação de verdade (com lock), vista pelo teste de corrida. */
final class SafeBufferAdapter implements BufferUnderTest {
    private final CircularTrackBuffer buffer;

    SafeBufferAdapter(int capacity) {
        this.buffer = new CircularTrackBuffer(capacity);
    }

    @Override public void add(Fix fix) { buffer.add(fix); }
    @Override public List<Fix> snapshot() { return buffer.snapshot(); }
    @Override public List<Fix> latest(int n) { return buffer.latest(n); }
    @Override public CircularTrackBuffer.Stats stats() { return buffer.stats(); }
    @Override public int capacity() { return buffer.capacity(); }
}
