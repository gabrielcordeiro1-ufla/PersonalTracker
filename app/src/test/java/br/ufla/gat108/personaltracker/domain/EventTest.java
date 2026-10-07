package br.ufla.gat108.personaltracker.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class EventTest {

    @Test
    public void deveCriarEventoValido() {
        Event event = new Event(Event.Type.GROSS_ERROR, 5000L, -21.2255, -44.9782, "salto de 80 m");

        assertEquals(Event.Type.GROSS_ERROR, event.getType());
        assertEquals(5000L, event.getTimestamp());
        assertEquals(-21.2255, event.getLatitude(), 1e-9);
        assertEquals(-44.9782, event.getLongitude(), 1e-9);
        assertEquals("salto de 80 m", event.getDetail());
    }

    @Test
    public void detalheNuloViraTextoVazio() {
        Event event = new Event(Event.Type.STOP, 0L, 0.0, 0.0, null);
        assertEquals("", event.getDetail());
    }

    @Test
    public void deveRejeitarTipoNulo() {
        assertThrows(NullPointerException.class, () -> new Event(null, 0L, 0.0, 0.0, ""));
    }

    @Test
    public void deveRejeitarCoordenadaOuTimestampInvalido() {
        assertThrows(InvalidCoordinatesException.class,
                () -> new Event(Event.Type.STOP, 0L, 91.0, 0.0, ""));
        assertThrows(InvalidCoordinatesException.class,
                () -> new Event(Event.Type.STOP, 0L, 0.0, 181.0, ""));
        assertThrows(InvalidCoordinatesException.class,
                () -> new Event(Event.Type.STOP, -1L, 0.0, 0.0, ""));
    }
}
