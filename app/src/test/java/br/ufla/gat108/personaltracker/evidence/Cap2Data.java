package br.ufla.gat108.personaltracker.evidence;

import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.io.GpsLoggerCsvReader;
import br.ufla.gat108.personaltracker.reconciliation.NoiseCharacterizer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Carrega os dados reais (src/test/resources/data) e prepara o que a evidência da capacidade 2 usa. */
final class Cap2Data {

    static final String[] WALKS = {"walk1.csv", "walk2.csv", "walk3.csv"};
    static final int WINDOW_SIZE = 30;
    /** Pula os primeiros fixes de cada trajeto (o GPS ainda está se acomodando). */
    static final int WINDOW_START = 30;

    private Cap2Data() {
    }

    static List<Fix> load(String name, float speedAccuracy) throws IOException {
        InputStream in = Cap2Data.class.getResourceAsStream("/data/" + name);
        if (in == null) {
            throw new IOException("Dado não encontrado em src/test/resources/data: " + name);
        }
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return GpsLoggerCsvReader.read(reader, speedAccuracy).getFixes();
        }
    }

    static NoiseCharacterizer.NoiseProfile staticProfile() throws IOException {
        return new NoiseCharacterizer().characterize(load("static.csv", 0.1f));
    }

    /**
     * Acurácia de velocidade estimada: o CSV do GPSLogger não a traz e a velocidade parado vem
     * sempre 0. Estima-se pela dispersão da velocidade nas caminhadas: desvio-padrão das
     * diferenças entre fixes consecutivos dividido por raiz de 2. É um limite superior, porque a
     * velocidade verdadeira também varia.
     */
    static float estimatedSpeedAccuracy() throws IOException {
        double sum = 0.0;
        long count = 0;
        List<Double> diffs = new ArrayList<>();
        for (String walk : WALKS) {
            List<Fix> fixes = load(walk, 0.1f);
            for (int i = 0; i + 1 < fixes.size(); i++) {
                diffs.add((double) (fixes.get(i + 1).getVelocity() - fixes.get(i).getVelocity()));
            }
        }
        for (double d : diffs) {
            sum += d;
        }
        double mean = sum / diffs.size();
        double sq = 0.0;
        for (double d : diffs) {
            sq += (d - mean) * (d - mean);
        }
        return (float) (Math.sqrt(sq / (diffs.size() - 1)) / Math.sqrt(2.0));
    }

    /** Uma janela real por trajeto: fixes [WINDOW_START, WINDOW_START + WINDOW_SIZE). */
    static List<List<Fix>> threeRealWindows() throws IOException {
        float speedAccuracy = estimatedSpeedAccuracy();
        List<List<Fix>> windows = new ArrayList<>();
        for (String walk : WALKS) {
            List<Fix> fixes = load(walk, speedAccuracy);
            windows.add(new ArrayList<>(fixes.subList(WINDOW_START, WINDOW_START + WINDOW_SIZE)));
        }
        return windows;
    }

    /** Todas as janelas disjuntas dos três trajetos (estatística em dados reais). */
    static List<List<Fix>> allRealWindows() throws IOException {
        float speedAccuracy = estimatedSpeedAccuracy();
        List<List<Fix>> windows = new ArrayList<>();
        for (String walk : WALKS) {
            windows.addAll(GpsLoggerCsvReader.windows(load(walk, speedAccuracy), WINDOW_SIZE, WINDOW_SIZE));
        }
        return windows;
    }
}
