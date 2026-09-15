package br.ufla.gat108.personaltracker.contracts;

import br.ufla.gat108.personaltracker.domain.Track;

/**
 * Contrato para o Agente Autônomo.
 * Interpreta o trajeto reconciliado e toma decisões sem exceder o orçamento de latência.
 */
public interface Agent {
    void act(Track track);
}