package br.ufla.gat108.personaltracker.concurrency;

import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.domain.Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Versão SEM lock, só para provar que o teste de corrida detecta o defeito.
 * Nunca use fora de teste.
 */
final class UnsafeBuffer implements BufferUnderTest {
    private final Track track;

    UnsafeBuffer(int capacity) {
        this.track = new Track(capacity);
    }

    @Override public void add(Fix fix) { track.addFix(fix); }

    @Override public List<Fix> snapshot() { return track.getFixes(); }

    @Override public List<Fix> latest(int n) {
        List<Fix> all = track.getFixes();
        return new ArrayList<>(all.subList(Math.max(0, all.size() - n), all.size()));
    }

    @Override public CircularTrackBuffer.Stats stats() {
        // quatro leituras separadas: sem o lock, a escrita pode entrar entre elas
        return new CircularTrackBuffer.Stats(track.size(), track.getTotalAdded(),
                track.getLength(), track.getMeanSpeed());
    }

    @Override public int capacity() { return track.getCapacity(); }
}
