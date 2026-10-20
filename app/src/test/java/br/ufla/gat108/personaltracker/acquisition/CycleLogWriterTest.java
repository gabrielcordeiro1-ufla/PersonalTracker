package br.ufla.gat108.personaltracker.acquisition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import br.ufla.gat108.personaltracker.domain.Fix;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

public class CycleLogWriterTest {

    @Test
    public void escreveCabecalhoELinhaPorCiclo() throws IOException {
        StringWriter out = new StringWriter();
        CycleLogWriter log = new CycleLogWriter(out);
        Fix f = new Fix(1000L, -21.2, -44.9, 1f, 3.54f, 0.114f, 0f);

        log.writeHeader();
        log.writeCycle(1, 1_700_000_000_000L, 1000.4, 850_000L, 2, 0, f, 2, 2);

        String[] lines = out.toString().split("\n");
        assertEquals(2, lines.length);
        assertEquals(CycleLogWriter.HEADER, lines[0]);
        assertEquals("1,1700000000000,1000.4,850,2,0,3.5,0.114,2,2", lines[1]);
    }

    @Test
    public void cicloSemFixDeixaAcuraciasEmBranco() throws IOException {
        StringWriter out = new StringWriter();
        new CycleLogWriter(out).writeCycle(7, 5L, 999.0, 100_000L, 0, 0, null, 10, 10);

        assertEquals("7,5,999.0,100,0,0,,,10,10", out.toString().trim());
    }

    @Test
    public void oRegistroNaoGravaCoordenadas() throws IOException {
        StringWriter out = new StringWriter();
        CycleLogWriter log = new CycleLogWriter(out);
        log.writeHeader();
        log.writeCycle(1, 5L, 1000.0, 100_000L, 1, 0, new Fix(1000L, -21.2255, -44.9782, 1f, 3f, 0.1f, 0f), 1, 1);

        String text = out.toString().toLowerCase();
        assertFalse(text.contains("lat"));
        assertFalse(text.contains("lon"));
        assertFalse(text.contains("21.2255"));
        assertFalse(text.contains("44.9782"));
        assertTrue(CycleLogWriter.HEADER.contains("accuracy_m"));
    }
}
