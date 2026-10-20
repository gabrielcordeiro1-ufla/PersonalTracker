package br.ufla.gat108.personaltracker.service;

import br.ufla.gat108.personaltracker.acquisition.AcquisitionCounters;
import br.ufla.gat108.personaltracker.concurrency.CircularTrackBuffer;

/**
 * Estado da aquisição compartilhado entre o serviço (escreve) e a tela (lê). É um singleton
 * simples: serviço e atividade vivem no mesmo processo. Guarda só buffer, contadores e flags,
 * nunca referências a Context.
 */
public final class AcquisitionRuntime {

    private static CircularTrackBuffer buffer = new CircularTrackBuffer();
    private static AcquisitionCounters counters = new AcquisitionCounters();
    private static boolean running;
    private static long startElapsedMs;
    private static String logPath = "";
    private static boolean gpsEnabled = true;

    private AcquisitionRuntime() {
    }

    /** Começa uma sessão nova: buffer e contadores zerados. */
    public static synchronized void startSession(long elapsedRealtimeMs, String logFilePath) {
        buffer = new CircularTrackBuffer();
        counters = new AcquisitionCounters();
        running = true;
        startElapsedMs = elapsedRealtimeMs;
        logPath = logFilePath;
    }

    public static synchronized void stopSession() {
        running = false;
    }

    public static synchronized CircularTrackBuffer buffer() { return buffer; }

    public static synchronized AcquisitionCounters counters() { return counters; }

    public static synchronized boolean isRunning() { return running; }

    public static synchronized long startElapsedMs() { return startElapsedMs; }

    public static synchronized String logPath() { return logPath; }

    public static synchronized boolean isGpsEnabled() { return gpsEnabled; }

    public static synchronized void setGpsEnabled(boolean enabled) { gpsEnabled = enabled; }
}
