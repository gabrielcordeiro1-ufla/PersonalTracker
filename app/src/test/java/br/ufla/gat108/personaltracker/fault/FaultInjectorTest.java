package br.ufla.gat108.personaltracker.fault;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.domain.GeoMath;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class FaultInjectorTest {

    private static final double LAT0 = -21.2255;
    private static final double LON0 = -44.9782;

    /** Registro limpo: 1 fix por segundo andando para leste a 1,5 m/s. */
    private static List<Fix> clean(int n) {
        List<Fix> list = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            list.add(new Fix(1000L * k, LAT0, GeoMath.longitudeFromEast(LAT0, LON0, 1.5 * k),
                    1.5f, 3f, 0.2f, 90f));
        }
        return list;
    }

    private static boolean sameFix(Fix a, Fix b) {
        return a.getTimestamp() == b.getTimestamp() && a.getLatitude() == b.getLatitude()
                && a.getLongitude() == b.getLongitude() && a.getVelocity() == b.getVelocity();
    }

    private static boolean sameList(List<Fix> a, List<Fix> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!sameFix(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static final FaultSpec EVERYTHING = FaultSpec.none()
            .withLoss(0.2).withPositionNoise(4.0).withSpeedNoise(0.3).withJumps(3, 40.0);

    @Test
    public void mesmaSementeDaExatamenteOMesmoResultado() {
        List<Fix> record = clean(200);

        FaultInjector.Result a = new FaultInjector(2026L).inject(record, EVERYTHING);
        FaultInjector.Result b = new FaultInjector(2026L).inject(record, EVERYTHING);

        assertTrue(sameList(a.getFixes(), b.getFixes()));
        assertEquals(a.getJumpIndices(), b.getJumpIndices());
        assertEquals(a.getDropped(), b.getDropped());
    }

    @Test
    public void repetirAChamadaNoMesmoInjetorNaoMudaOResultado() {
        List<Fix> record = clean(200);
        FaultInjector injector = new FaultInjector(7L);

        assertTrue(sameList(injector.inject(record, EVERYTHING).getFixes(),
                injector.inject(record, EVERYTHING).getFixes()));
    }

    @Test
    public void sementesDiferentesDaoResultadosDiferentes() {
        List<Fix> record = clean(200);

        FaultInjector.Result a = new FaultInjector(1L).inject(record, EVERYTHING);
        FaultInjector.Result b = new FaultInjector(2L).inject(record, EVERYTHING);

        assertFalse(sameList(a.getFixes(), b.getFixes()));
    }

    @Test
    public void semFalhaDevolveUmaCopiaIgualDoRegistro() {
        List<Fix> record = clean(50);

        FaultInjector.Result r = new FaultInjector(1L).inject(record, FaultSpec.none());

        assertTrue(sameList(record, r.getFixes()));
        assertTrue(r.getJumpIndices().isEmpty());
        assertEquals(0, r.getDropped());
    }

    @Test
    public void naoAlteraOregistroDeEntrada() {
        List<Fix> record = clean(60);
        Fix before = record.get(10);

        new FaultInjector(1L).inject(record, EVERYTHING);

        assertEquals(60, record.size());
        assertTrue(sameFix(before, record.get(10)));
    }

    @Test
    public void perdaDescartaAproximadamenteAFracaoPedidaEPreservaPontasEOrdem() {
        List<Fix> record = clean(2000);

        FaultInjector.Result r = new FaultInjector(5L).inject(record, FaultSpec.none().withLoss(0.3));
        List<Fix> out = r.getFixes();

        assertEquals(600.0, r.getDropped(), 80.0);
        assertEquals(2000, out.size() + r.getDropped());
        assertEquals(0L, out.get(0).getTimestamp());
        assertEquals(1999_000L, out.get(out.size() - 1).getTimestamp());
        for (int i = 1; i < out.size(); i++) {
            assertTrue(out.get(i).getTimestamp() > out.get(i - 1).getTimestamp());
        }
    }

    @Test
    public void ruidoDePosicaoTemODesvioPedidoEMediaProximaDeZero() {
        List<Fix> record = clean(4000);

        List<Fix> out = new FaultInjector(9L).inject(record, FaultSpec.none().withPositionNoise(5.0)).getFixes();

        double sum = 0.0;
        double sumSq = 0.0;
        for (int i = 0; i < record.size(); i++) {
            double e = GeoMath.eastMeters(record.get(i).getLatitude(), record.get(i).getLongitude(),
                    out.get(i).getLongitude());
            sum += e;
            sumSq += e * e;
        }
        double mean = sum / record.size();
        double std = Math.sqrt(sumSq / record.size() - mean * mean);
        assertEquals(0.0, mean, 0.3);
        assertEquals(5.0, std, 0.3);
    }

    @Test
    public void ruidoDeVelocidadeNuncaGeraVelocidadeNegativa() {
        List<Fix> out = new FaultInjector(3L).inject(clean(500), FaultSpec.none().withSpeedNoise(5.0)).getFixes();
        for (Fix f : out) {
            assertTrue(f.getVelocity() >= 0f);
        }
    }

    @Test
    public void saltosTemOTamanhoPedidoSoNosFixesSorteadosComMargemEEspacamento() {
        List<Fix> record = clean(120);

        FaultInjector.Result r = new FaultInjector(11L).inject(record, FaultSpec.none().withJumps(4, 40.0));
        List<Fix> out = r.getFixes();
        List<Integer> jumps = r.getJumpIndices();

        assertEquals(4, jumps.size());
        for (int i = 0; i < out.size(); i++) {
            double d = GeoMath.distanceMeters(record.get(i).getLatitude(), record.get(i).getLongitude(),
                    out.get(i).getLatitude(), out.get(i).getLongitude());
            if (jumps.contains(i)) {
                assertEquals(40.0, d, 0.05);
            } else {
                assertEquals(0.0, d, 1e-9);
            }
        }
        for (int k = 0; k < jumps.size(); k++) {
            assertTrue(jumps.get(k) >= FaultInjector.JUMP_MARGIN);
            assertTrue(jumps.get(k) <= 119 - FaultInjector.JUMP_MARGIN);
            if (k > 0) {
                assertTrue(jumps.get(k) - jumps.get(k - 1) >= FaultInjector.JUMP_SPACING);
            }
        }
    }

    @Test
    public void ligarSaltoNaoMudaOQueORuidoSorteia() {
        List<Fix> record = clean(100);
        FaultSpec noiseOnly = FaultSpec.none().withPositionNoise(4.0);

        List<Fix> a = new FaultInjector(21L).inject(record, noiseOnly).getFixes();
        FaultInjector.Result b = new FaultInjector(21L).inject(record, noiseOnly.withJumps(2, 30.0));

        for (int i = 0; i < a.size(); i++) {
            if (!b.getJumpIndices().contains(i)) {
                assertTrue("fix " + i + " diferiu", sameFix(a.get(i), b.getFixes().get(i)));
            }
        }
    }

    @Test
    public void especificacaoInvalidaLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> FaultSpec.none().withPositionNoise(-1.0));
        assertThrows(IllegalArgumentException.class, () -> FaultSpec.none().withSpeedNoise(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> FaultSpec.none().withLoss(1.0));
        assertThrows(IllegalArgumentException.class, () -> FaultSpec.none().withLoss(-0.1));
        assertThrows(IllegalArgumentException.class, () -> FaultSpec.none().withJumps(-1, 10.0));
        assertThrows(IllegalArgumentException.class, () -> FaultSpec.none().withJumps(1, Double.POSITIVE_INFINITY));
    }

    @Test
    public void registroCurtoDemaisParaOsSaltosPedidosLancaExcecao() {
        assertThrows(IllegalArgumentException.class,
                () -> new FaultInjector(1L).inject(clean(10), FaultSpec.none().withJumps(2, 30.0)));
        assertThrows(IllegalArgumentException.class,
                () -> new FaultInjector(1L).inject(null, FaultSpec.none()));
    }
}
