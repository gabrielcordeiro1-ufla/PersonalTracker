package br.ufla.gat108.personaltracker.concurrency;

import br.ufla.gat108.personaltracker.domain.Fix;

import java.util.List;

/** Contrato mínimo que o teste de corrida exige, para rodar o MESMO teste em duas implementações. */
interface BufferUnderTest {
    void add(Fix fix);

    List<Fix> snapshot();

    List<Fix> latest(int n);

    CircularTrackBuffer.Stats stats();

    int capacity();
}
