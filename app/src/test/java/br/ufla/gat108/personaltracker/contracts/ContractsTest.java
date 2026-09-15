package br.ufla.gat108.personaltracker.contracts;

import org.junit.Test;
import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.domain.Track;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

public class ContractsTest {

    @Test
    public void testeAceitacaoAquisicaoFalha() {
        // Criamos uma classe anônima vazia só para simular o contrato
        FixSource stubSource = () -> null; // Ainda sem código real

        Fix fix = stubSource.acquire();

        // O teste VAI FALHAR aqui porque o retorno é nulo
        assertNotNull("O FixSource deve retornar uma leitura válida", fix);
    }

    @Test
    public void testeAceitacaoReconciliacaoFalha() {
        Reconciler stubReconciler = (y, v, a) -> new double[0]; // Retorna array vazio

        double[] medições = {110.5, 60.8}; // Exemplo dos slides
        double[] reconciliado = stubReconciler.reconcile(medições, new double[2][2], new double[1][2]);

        // O teste VAI FALHAR aqui porque esperamos dados ajustados, mas veio vazio
        assertEquals("Deve retornar o mesmo número de fluxos ajustados", 2, reconciliado.length);
    }

    @Test
    public void testeAceitacaoAgenteFalha() {
        Agent stubAgent = track -> { throw new UnsupportedOperationException("IA não implementada"); };

        // O teste VAI FALHAR porque o agente vai estourar um erro ao invés de agir
        stubAgent.act(new Track());
    }
}