package br.ufla.gat108.personaltracker.domain;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TrackTest {

    @Test
    public void deveAcumularDistanciaEmMemoriaConstante() {
        Track track = new Track();

        // Ponto 1: Portaria principal da UFLA
        Fix p1 = new Fix(1000L, -21.2255, -44.9782, 0f, 1f);
        // Ponto 2: Um pouco mais à frente na rua
        Fix p2 = new Fix(2000L, -21.2265, -44.9782, 1.2f, 1f);

        // Adiciona o primeiro ponto (distância deve ser 0)
        track.addFix(p1);
        assertEquals(0.0, track.getLength(), 0.001);

        // Adiciona o segundo ponto (agora deve ter acumulado alguns metros)
        track.addFix(p2);
        assertTrue(track.getLength() > 0.0);
        assertTrue(track.getLength() < 200.0); // O deslocamento foi de ~100 metros
    }
}