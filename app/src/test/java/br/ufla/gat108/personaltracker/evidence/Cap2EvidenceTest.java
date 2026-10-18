package br.ufla.gat108.personaltracker.evidence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.domain.GeoMath;
import br.ufla.gat108.personaltracker.reconciliation.KinematicReconciler;
import br.ufla.gat108.personaltracker.reconciliation.NoiseCharacterizer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

/**
 * Evidências da capacidade 2 sobre dados REAIS (GPSLogger). Reproduz com: ./gradlew test
 * Os arquivos gerados ficam em app/build/evidence/.
 */
public class Cap2EvidenceTest {

    private static final int INJECTED_INDEX = 15;
    private static final double JUMP_EAST = 40.0;
    private static final double JUMP_NORTH = -25.0;

    private static double positionOptimism() throws IOException {
        return Cap2Data.staticProfile().getPositionOptimismRatio();
    }

    private static KinematicReconciler reconciler() throws IOException {
        return new KinematicReconciler(positionOptimism(), 1.0,
                br.ufla.gat108.personaltracker.reconciliation.GrossErrorDetector.DEFAULT_THRESHOLD,
                br.ufla.gat108.personaltracker.reconciliation.GrossErrorDetector.DEFAULT_MAX_REMOVALS);
    }

    private static List<Fix> jump(List<Fix> window, int k, double dEast, double dNorth) {
        List<Fix> copy = new ArrayList<>(window);
        Fix f = copy.get(k);
        double lat0 = f.getLatitude();
        double lon0 = f.getLongitude();
        copy.set(k, new Fix(f.getTimestamp(),
                GeoMath.latitudeFromNorth(lat0, dNorth),
                GeoMath.longitudeFromEast(lat0, lon0, dEast),
                f.getVelocity(), f.getAccuracy(), f.getSpeedAccuracy(), f.getBearing()));
        return copy;
    }

    /** Desvio RMS (m) entre duas reconciliações, ignorando o índice 'skip' da janela original. */
    private static double deviation(KinematicReconciler.Result a, KinematicReconciler.Result reference, int skip) {
        double sum = 0.0;
        int count = 0;
        List<Integer> ia = a.getKeptIndices();
        List<Integer> ir = reference.getKeptIndices();
        double[] latA = a.getLatitudes();
        double[] lonA = a.getLongitudes();
        double[] latR = reference.getLatitudes();
        double[] lonR = reference.getLongitudes();
        for (int p = 0; p < ia.size(); p++) {
            int k = ia.get(p);
            if (k == skip) {
                continue;
            }
            int q = ir.indexOf(k);
            double d = GeoMath.distanceMeters(latA[p], lonA[p], latR[q], lonR[q]);
            sum += d * d;
            count++;
        }
        return Math.sqrt(sum / count);
    }

    @Test
    public void razaoDeOtimismoEMedidaComOAparelhoParado() throws IOException {
        NoiseCharacterizer.NoiseProfile p = Cap2Data.staticProfile();

        assertTrue(p.getCount() > 200);
        assertTrue(p.getRadius68Meters() > 0.0);
        assertTrue(p.getPositionOptimismRatio() > 0.3 && p.getPositionOptimismRatio() < 3.0);
    }

    @Test
    public void residuoCaiEmTresJanelasReais() throws IOException {
        List<List<Fix>> windows = Cap2Data.threeRealWindows();
        assertEquals(3, windows.size());

        for (List<Fix> window : windows) {
            KinematicReconciler.Result r = reconciler().reconcile(window, false);
            assertTrue(r.getResidualRmsBefore() > 1e-3);
            assertEquals(0.0, r.getResidualRmsAfter(), 1e-6);
            assertTrue(r.getResidualRmsAfter() < r.getResidualRmsBefore());
        }
    }

    @Test
    public void erroGrosseiroInjetadoEDetectadoESemDetectorOResultadoPiora() throws IOException {
        for (List<Fix> window : Cap2Data.threeRealWindows()) {
            KinematicReconciler.Result clean = reconciler().reconcile(window, false);
            List<Fix> corrupted = jump(window, INJECTED_INDEX, JUMP_EAST, JUMP_NORTH);

            KinematicReconciler.Result with = reconciler().reconcile(corrupted, true);
            KinematicReconciler.Result without = reconciler().reconcile(corrupted, false);

            assertEquals(Collections.singletonList(INJECTED_INDEX), with.getRemovedIndices());
            double devWith = deviation(with, clean, INJECTED_INDEX);
            double devWithout = deviation(without, clean, INJECTED_INDEX);
            assertTrue("sem detector " + devWithout + " deveria ser pior que com " + devWith,
                    devWithout > 1.5 * devWith);
        }
    }

    @Test
    public void gerarArquivosDeEvidencia() throws IOException {
        Path dir = Paths.get(System.getProperty("user.dir"), "build", "evidence");
        Files.createDirectories(dir);

        NoiseCharacterizer.NoiseProfile p = Cap2Data.staticProfile();
        float speedAcc = Cap2Data.estimatedSpeedAccuracy();
        StringBuilder md = new StringBuilder();
        md.append("# Evidências da capacidade 2 (dados reais)\n\n");

        md.append("## Aparelho parado\n\n");
        md.append(String.format(Locale.US,
                "- leituras: %d; desvio leste %.2f m, norte %.2f m%n"
              + "- raio observado de 68%%: %.2f m; acurácia declarada média: %.2f m%n"
              + "- **razão de otimismo (posição): %.2f**%n"
              + "- acurácia de velocidade estimada nas caminhadas: %.3f m/s%n%n",
                p.getCount(), p.getStdEastMeters(), p.getStdNorthMeters(), p.getRadius68Meters(),
                p.getMeanDeclaredAccuracy(), p.getPositionOptimismRatio(), speedAcc));

        md.append("## Resíduo antes e depois (3 janelas reais de 30 fixes)\n\n");
        md.append("| janela | resíduo RMS antes (m) | depois (m) | qui² / g.l. | removidos |\n|---|---|---|---|---|\n");
        List<List<Fix>> windows = Cap2Data.threeRealWindows();
        for (int i = 0; i < windows.size(); i++) {
            KinematicReconciler.Result r = reconciler().reconcile(windows.get(i), true);
            md.append(String.format(Locale.US, "| %s | %.3f | %.2e | %.2f | %s |%n",
                    Cap2Data.WALKS[i], r.getResidualRmsBefore(), r.getResidualRmsAfter(),
                    r.getNormalizedChiSquare(), r.getRemovedIndices()));
        }

        md.append(String.format(Locale.US,
                "%n## Erro grosseiro injetado (salto de %.0f m no fix %d)%n%n", Math.hypot(JUMP_EAST, JUMP_NORTH),
                INJECTED_INDEX));
        md.append("| janela | removido pelo detector | desvio sem detector (m) | desvio com detector (m) |\n|---|---|---|---|\n");
        for (int i = 0; i < windows.size(); i++) {
            KinematicReconciler.Result clean = reconciler().reconcile(windows.get(i), false);
            List<Fix> corrupted = jump(windows.get(i), INJECTED_INDEX, JUMP_EAST, JUMP_NORTH);
            KinematicReconciler.Result with = reconciler().reconcile(corrupted, true);
            KinematicReconciler.Result without = reconciler().reconcile(corrupted, false);
            md.append(String.format(Locale.US, "| %s | %s | %.2f | %.2f |%n", Cap2Data.WALKS[i],
                    with.getRemovedIndices(), deviation(without, clean, INJECTED_INDEX),
                    deviation(with, clean, INJECTED_INDEX)));
        }
        md.append("\nDesvio = RMS da distância entre a solução reconciliada e a solução da janela sem a injeção, nos demais fixes.\n");

        List<List<Fix>> all = Cap2Data.allRealWindows();
        int flagged = 0;
        List<Double> chis = new ArrayList<>();
        for (List<Fix> w : all) {
            KinematicReconciler.Result r = reconciler().reconcile(w, true);
            if (!r.getRemovedIndices().isEmpty()) {
                flagged++;
            }
            chis.add(r.getNormalizedChiSquare());
        }
        Collections.sort(chis);
        md.append(String.format(Locale.US,
                "%n## Todas as janelas disjuntas dos três trajetos%n%n"
              + "- janelas: %d; com remoção pelo detector: %d%n"
              + "- qui² / g.l. mediano: %.2f (p10 %.2f, p90 %.2f)%n",
                all.size(), flagged, chis.get(chis.size() / 2),
                chis.get((int) (0.1 * chis.size())), chis.get((int) (0.9 * chis.size()))));

        Files.write(dir.resolve("cap2_evidencias.md"), md.toString().getBytes(StandardCharsets.UTF_8));
        writeScatter(dir.resolve("cap2_dispersao_parado.svg"), p);
        System.out.println(md);
    }

    private static void writeScatter(Path file, NoiseCharacterizer.NoiseProfile p) throws IOException {
        double[] e = p.getEastOffsets();
        double[] n = p.getNorthOffsets();
        double max = Math.max(p.getMeanDeclaredAccuracy(), p.getRadius68Meters());
        for (int i = 0; i < e.length; i++) {
            max = Math.max(max, Math.max(Math.abs(e[i]), Math.abs(n[i])));
        }
        max *= 1.1;
        double c = 210.0;
        double s = 190.0 / max;
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"420\" height=\"460\" viewBox=\"0 0 420 460\">\n");
        svg.append("<rect width=\"420\" height=\"460\" fill=\"white\"/>\n");
        svg.append("<line x1=\"10\" y1=\"210\" x2=\"410\" y2=\"210\" stroke=\"#bbb\"/><line x1=\"210\" y1=\"10\" x2=\"210\" y2=\"410\" stroke=\"#bbb\"/>\n");
        for (int i = 0; i < e.length; i++) {
            svg.append(String.format(Locale.US, "<circle cx=\"%.1f\" cy=\"%.1f\" r=\"2.5\" fill=\"#1b6ec2\" fill-opacity=\"0.6\"/>%n",
                    c + e[i] * s, c - n[i] * s));
        }
        svg.append(String.format(Locale.US,
                "<circle cx=\"210\" cy=\"210\" r=\"%.1f\" fill=\"none\" stroke=\"#d9381e\" stroke-width=\"2\"/>%n", p.getRadius68Meters() * s));
        svg.append(String.format(Locale.US,
                "<circle cx=\"210\" cy=\"210\" r=\"%.1f\" fill=\"none\" stroke=\"#2e8b57\" stroke-width=\"2\" stroke-dasharray=\"6 4\"/>%n", p.getMeanDeclaredAccuracy() * s));
        svg.append(String.format(Locale.US,
                "<text x=\"10\" y=\"430\" font-size=\"12\" font-family=\"sans-serif\">vermelho: raio observado 68%% = %.2f m</text>%n", p.getRadius68Meters()));
        svg.append(String.format(Locale.US,
                "<text x=\"10\" y=\"446\" font-size=\"12\" font-family=\"sans-serif\">verde: acurácia declarada média = %.2f m (razão %.2f)</text>%n", p.getMeanDeclaredAccuracy(), p.getPositionOptimismRatio()));
        svg.append("</svg>\n");
        Files.write(file, svg.toString().getBytes(StandardCharsets.UTF_8));
    }
}
