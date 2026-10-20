package br.ufla.gat108.personaltracker.acquisition;

import br.ufla.gat108.personaltracker.domain.Fix;

import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

/**
 * Registro de aquisição: uma linha por ciclo, para provar continuidade (30 minutos com o aparelho
 * bloqueado). NÃO grava coordenadas: o registro prova o funcionamento sem guardar dado pessoal
 * em claro no disco (a persistência cifrada dos eventos é da capacidade 9).
 */
public final class CycleLogWriter {

    public static final String HEADER =
            "seq,epoch_ms,interval_ms,cycle_us,new_fixes,rejected,accuracy_m,speed_accuracy_ms,buffer_size,total_fixes";

    private final Writer out;

    public CycleLogWriter(Writer out) {
        this.out = out;
    }

    public void writeHeader() throws IOException {
        out.write(HEADER);
        out.write('\n');
        out.flush();
    }

    /** Escreve e descarrega a linha na hora: se o app morrer, o que já foi gravado fica. */
    public void writeCycle(long seq, long epochMs, double intervalMs, long cycleNanos, int accepted, int rejected,
                           Fix lastAccepted, int bufferSize, long totalFixes) throws IOException {
        String accuracy = lastAccepted == null ? "" : String.format(Locale.US, "%.1f", lastAccepted.getAccuracy());
        String speedAccuracy = lastAccepted == null ? "" : String.format(Locale.US, "%.3f", lastAccepted.getSpeedAccuracy());
        out.write(String.format(Locale.US, "%d,%d,%.1f,%d,%d,%d,%s,%s,%d,%d%n",
                seq, epochMs, intervalMs, cycleNanos / 1000L, accepted, rejected,
                accuracy, speedAccuracy, bufferSize, totalFixes).replace("\r", ""));
        out.flush();
    }

    public void close() throws IOException {
        out.close();
    }
}
