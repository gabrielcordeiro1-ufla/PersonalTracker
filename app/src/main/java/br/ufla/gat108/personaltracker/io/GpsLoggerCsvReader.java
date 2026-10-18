package br.ufla.gat108.personaltracker.io;

import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.domain.InvalidCoordinatesException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Leitor de registros do GPSLogger (CSV). Lê as colunas pelo nome do cabeçalho.
 *
 * Obrigatórias: lat, lon, accuracy, timestamp_ms. Opcionais: speed, bearing, provider.
 * O CSV não traz a acurácia de velocidade, então ela é informada por parâmetro (D-19).
 *
 * Regras: só pontos do provedor "gps" (se a coluna existir); velocidade ou rumo ausentes
 * viram 0 (o Android não informa rumo com o aparelho parado); linhas inválidas ou fora
 * de ordem são descartadas e contadas.
 */
public final class GpsLoggerCsvReader {

    private GpsLoggerCsvReader() {
    }

    /** Fixes lidos e quantas linhas foram descartadas. */
    public static final class Result {
        private final List<Fix> fixes;
        private final int skipped;

        Result(List<Fix> fixes, int skipped) {
            this.fixes = fixes;
            this.skipped = skipped;
        }

        public List<Fix> getFixes() { return new ArrayList<>(fixes); }

        public int getSkipped() { return skipped; }
    }

    public static Result read(Reader reader, float speedAccuracy) throws IOException {
        BufferedReader in = new BufferedReader(reader);
        String headerLine = in.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Arquivo vazio.");
        }
        List<String> header = split(headerLine);
        int iLat = column(header, "lat", true);
        int iLon = column(header, "lon", true);
        int iAcc = column(header, "accuracy", true);
        int iTs = column(header, "timestamp_ms", true);
        int iSpeed = column(header, "speed", false);
        int iBearing = column(header, "bearing", false);
        int iProvider = column(header, "provider", false);

        List<Fix> fixes = new ArrayList<>();
        int skipped = 0;
        long lastTimestamp = Long.MIN_VALUE;
        String line;
        while ((line = in.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }
            List<String> f = split(line);
            try {
                if (iProvider >= 0 && !"gps".equalsIgnoreCase(get(f, iProvider).trim())) {
                    skipped++;
                    continue;
                }
                long ts = Long.parseLong(get(f, iTs).trim());
                if (ts < lastTimestamp) {
                    skipped++;
                    continue;
                }
                float bearing = optionalFloat(f, iBearing);
                bearing = ((bearing % 360f) + 360f) % 360f;
                if (bearing >= 360f) {
                    bearing = 0f;
                }
                fixes.add(new Fix(ts,
                        Double.parseDouble(get(f, iLat).trim()),
                        Double.parseDouble(get(f, iLon).trim()),
                        optionalFloat(f, iSpeed),
                        Float.parseFloat(get(f, iAcc).trim()),
                        speedAccuracy, bearing));
                lastTimestamp = ts;
            } catch (NumberFormatException | InvalidCoordinatesException e) {
                skipped++;
            }
        }
        return new Result(fixes, skipped);
    }

    /** Divide a sequência onde o intervalo entre dois fixes passa de maxGapMs. */
    public static List<List<Fix>> splitOnGaps(List<Fix> fixes, long maxGapMs) {
        List<List<Fix>> segments = new ArrayList<>();
        List<Fix> current = new ArrayList<>();
        for (Fix f : fixes) {
            if (!current.isEmpty()
                    && f.getTimestamp() - current.get(current.size() - 1).getTimestamp() > maxGapMs) {
                segments.add(current);
                current = new ArrayList<>();
            }
            current.add(f);
        }
        if (!current.isEmpty()) {
            segments.add(current);
        }
        return segments;
    }

    /** Janelas contíguas de 'size' fixes, avançando 'stride' a cada uma. Sobra incompleta é ignorada. */
    public static List<List<Fix>> windows(List<Fix> fixes, int size, int stride) {
        if (size < 2 || stride < 1) {
            throw new IllegalArgumentException("size deve ser >= 2 e stride >= 1.");
        }
        List<List<Fix>> result = new ArrayList<>();
        for (int start = 0; start + size <= fixes.size(); start += stride) {
            result.add(new ArrayList<>(fixes.subList(start, start + size)));
        }
        return result;
    }

    private static int column(List<String> header, String name, boolean required) {
        for (int i = 0; i < header.size(); i++) {
            if (header.get(i).trim().equalsIgnoreCase(name)) {
                return i;
            }
        }
        if (required) {
            throw new IllegalArgumentException("Coluna obrigatória ausente no CSV: " + name);
        }
        return -1;
    }

    private static String get(List<String> fields, int index) {
        return index >= 0 && index < fields.size() ? fields.get(index) : "";
    }

    private static float optionalFloat(List<String> fields, int index) {
        String s = get(fields, index).trim();
        return s.isEmpty() ? 0f : Float.parseFloat(s);
    }

    /** Divide por vírgula respeitando campos entre aspas. */
    private static List<String> split(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            } else if (c == ',' && !quoted) {
                parts.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        parts.add(sb.toString());
        return parts;
    }
}
