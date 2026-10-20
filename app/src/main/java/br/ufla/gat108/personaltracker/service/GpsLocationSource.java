package br.ufla.gat108.personaltracker.service;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import br.ufla.gat108.personaltracker.acquisition.AcquisitionCounters;
import br.ufla.gat108.personaltracker.acquisition.FixFactory;
import br.ufla.gat108.personaltracker.contracts.FixSource;
import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.domain.InvalidCoordinatesException;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Fonte de fixes do GPS. O callback do sistema (numa HandlerThread) converte cada Location em
 * Fix e põe numa fila; a tarefa periódica esvazia a fila com acquire(). Assim nenhum fix se
 * perde entre dois ciclos e quem escreve no buffer é uma só thread.
 */
public final class GpsLocationSource implements FixSource, LocationListener {

    private static final int QUEUE_CAPACITY = 256;

    private final BlockingQueue<Fix> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final AcquisitionCounters counters;

    public GpsLocationSource(AcquisitionCounters counters) {
        this.counters = counters;
    }

    /** Próximo fix da fila, ou null se não há nenhum (contrato de FixSource.acquire neste projeto). */
    @Override
    public Fix acquire() {
        return queue.poll();
    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            Fix fix = FixFactory.fromSensor(
                    location.getTime(), location.getLatitude(), location.getLongitude(),
                    location.hasSpeed(), location.getSpeed(),
                    location.hasAccuracy(), location.getAccuracy(),
                    location.hasSpeedAccuracy(), location.getSpeedAccuracyMetersPerSecond(),
                    location.hasBearing(), location.getBearing());
            if (!queue.offer(fix)) {
                counters.recordDropped();
            }
        } catch (InvalidCoordinatesException e) {
            counters.recordSensorRejected();
        }
    }

    // Implementados de propósito: em Android anterior à API 30, onStatusChanged é abstrato na
    // plataforma, e deixá-lo de fora causa AbstractMethodError.
    @Override
    @SuppressWarnings("deprecation")
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
        AcquisitionRuntime.setGpsEnabled(true);
    }

    @Override
    public void onProviderDisabled(String provider) {
        AcquisitionRuntime.setGpsEnabled(false);
    }
}
