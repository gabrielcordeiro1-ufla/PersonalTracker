package br.ufla.gat108.personaltracker.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import br.ufla.gat108.personaltracker.domain.Fix;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.junit.Test;

public class GpsLoggerCsvReaderTest {

    private static final String HEADER = "time,lat,lon,accuracy,bearing,speed,provider,timestamp_ms\n";

    private static GpsLoggerCsvReader.Result read(String body) throws IOException {
        return GpsLoggerCsvReader.read(new StringReader(HEADER + body), 0.2f);
    }

    @Test
    public void leLinhaValidaComTodosOsCampos() throws IOException {
        GpsLoggerCsvReader.Result r = read("x,-21.2,-44.9,3.5,90.0,1.4,gps,1000\n");

        assertEquals(1, r.getFixes().size());
        Fix f = r.getFixes().get(0);
        assertEquals(1000L, f.getTimestamp());
        assertEquals(-21.2, f.getLatitude(), 1e-9);
        assertEquals(3.5f, f.getAccuracy(), 1e-6);
        assertEquals(90f, f.getBearing(), 1e-6);
        assertEquals(1.4f, f.getVelocity(), 1e-6);
        assertEquals(0.2f, f.getSpeedAccuracy(), 1e-6);
    }

    @Test
    public void ignoraPontosQueNaoSaoDoGps() throws IOException {
        GpsLoggerCsvReader.Result r = read(
                "x,-21.2,-44.9,3.5,90,1.4,network,1000\n"
              + "x,-21.2,-44.9,3.5,90,1.4,gps,2000\n");

        assertEquals(1, r.getFixes().size());
        assertEquals(1, r.getSkipped());
    }

    @Test
    public void rumoOuVelocidadeAusentesViramZero() throws IOException {
        Fix f = read("x,-21.2,-44.9,3.5,,,gps,1000\n").getFixes().get(0);
        assertEquals(0f, f.getBearing(), 1e-6);
        assertEquals(0f, f.getVelocity(), 1e-6);
    }

    @Test
    public void rumo360ViraZero() throws IOException {
        assertEquals(0f, read("x,-21.2,-44.9,3.5,360,1,gps,1000\n").getFixes().get(0).getBearing(), 1e-6);
    }

    @Test
    public void descartaLinhasInvalidasEForaDeOrdem() throws IOException {
        GpsLoggerCsvReader.Result r = read(
                "x,-21.2,-44.9,3.5,90,1,gps,2000\n"
              + "x,-21.2,-44.9,3.5,90,1,gps,1000\n"    // fora de ordem
              + "x,95.0,-44.9,3.5,90,1,gps,3000\n"     // latitude inválida
              + "x,abc,-44.9,3.5,90,1,gps,4000\n"      // número inválido
              + "x,-21.2,-44.9,3.5,90,1,gps,5000\n");

        assertEquals(2, r.getFixes().size());
        assertEquals(3, r.getSkipped());
    }

    @Test
    public void colunaObrigatoriaAusenteLancaExcecao() {
        assertThrows(IllegalArgumentException.class,
                () -> GpsLoggerCsvReader.read(new StringReader("lat,lon,accuracy\n1,2,3\n"), 0.2f));
        assertThrows(IllegalArgumentException.class,
                () -> GpsLoggerCsvReader.read(new StringReader(""), 0.2f));
    }

    @Test
    public void splitOnGapsSeparaOndeHaBuraco() throws IOException {
        List<Fix> fixes = read(
                "x,-21.2,-44.9,3,90,1,gps,1000\n"
              + "x,-21.2,-44.9,3,90,1,gps,3000\n"
              + "x,-21.2,-44.9,3,90,1,gps,60000\n"
              + "x,-21.2,-44.9,3,90,1,gps,62000\n").getFixes();

        List<List<Fix>> segments = GpsLoggerCsvReader.splitOnGaps(fixes, 5000L);

        assertEquals(2, segments.size());
        assertEquals(2, segments.get(0).size());
        assertEquals(2, segments.get(1).size());
    }

    @Test
    public void windowsGeraJanelasContiguasComPasso() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append("x,-21.2,-44.9,3,90,1,gps,").append(1000 * i).append("\n");
        }
        List<Fix> fixes = read(sb.toString()).getFixes();

        List<List<Fix>> windows = GpsLoggerCsvReader.windows(fixes, 4, 3);

        assertEquals(3, windows.size());                 // inícios 0, 3, 6
        assertEquals(3000L, windows.get(1).get(0).getTimestamp());
        assertThrows(IllegalArgumentException.class, () -> GpsLoggerCsvReader.windows(fixes, 1, 1));
    }
}
