package br.ufla.gat108.personaltracker.contracts;

import br.ufla.gat108.personaltracker.domain.Fix;

/**
 * Contrato para a fonte de dados do GPS.
 * Responsável por adquirir as coordenadas brutas com sua respectiva incerteza nativa.
 */
public interface FixSource {
    Fix acquire();
}