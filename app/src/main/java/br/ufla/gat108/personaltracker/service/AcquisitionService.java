package br.ufla.gat108.personaltracker.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.LocationManager;
import android.os.Build;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;

import br.ufla.gat108.personaltracker.MainActivity;
import br.ufla.gat108.personaltracker.acquisition.AcquisitionCounters;
import br.ufla.gat108.personaltracker.acquisition.CycleLogWriter;
import br.ufla.gat108.personaltracker.acquisition.PeriodicAcquisitionTask;
import br.ufla.gat108.personaltracker.concurrency.CircularTrackBuffer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Serviço em primeiro plano (tipo location) que mantém a aquisição viva com a tela bloqueada.
 *
 * Camada 1: callback do GPS (HandlerThread) -> fila -> tarefa periódica de 1 s (thread própria)
 * -> CircularTrackBuffer. Um wake lock parcial mantém a CPU ligada.
 *
 * Iniciar com startForegroundService (a partir da tela, visível) e parar com stopService.
 */
public class AcquisitionService extends Service {

    private static final String CHANNEL_ID = "acquisition";
    private static final int NOTIFICATION_ID = 1;
    private static final long PERIOD_MS = 1000L;
    private static final long WAKE_LOCK_TIMEOUT_MS = 3L * 60L * 60L * 1000L;

    private boolean started;
    private HandlerThread gpsThread;
    private ScheduledExecutorService executor;
    private PowerManager.WakeLock wakeLock;
    private LocationManager locationManager;
    private GpsLocationSource source;
    private CycleLogWriter log;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startAcquisition();
        return START_NOT_STICKY;
    }

    private void startAcquisition() {
        if (started) {
            return;
        }
        started = true;

        // O Android exige startForeground poucos segundos depois de startForegroundService
        createChannel();
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        File logFile = newLogFile();
        AcquisitionRuntime.startSession(SystemClock.elapsedRealtime(), logFile.getAbsolutePath());
        AcquisitionCounters counters = AcquisitionRuntime.counters();
        CircularTrackBuffer buffer = AcquisitionRuntime.buffer();
        source = new GpsLocationSource(counters);
        log = openLog(logFile);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PersonalTracker:acquisition");
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);

        gpsThread = new HandlerThread("gps-callback");
        gpsThread.start();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            AcquisitionRuntime.setGpsEnabled(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, PERIOD_MS, 0f,
                    source, gpsThread.getLooper());
        } catch (SecurityException e) {
            stopSelf();   // permissão revogada: não há o que adquirir
            return;
        }

        PeriodicAcquisitionTask task = new PeriodicAcquisitionTask(
                source, buffer, counters, log, System::nanoTime, System::currentTimeMillis);
        executor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "acquisition-1Hz"));
        executor.scheduleAtFixedRate(task, PERIOD_MS, PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onDestroy() {
        if (locationManager != null && source != null) {
            locationManager.removeUpdates(source);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
        if (gpsThread != null) {
            gpsThread.quitSafely();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (log != null) {
            try {
                log.close();
            } catch (IOException ignored) {
                // fim da sessão: nada a fazer
            }
        }
        AcquisitionRuntime.stopSession();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Aquisição de posição", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Mantém a aquisição de GPS ativa com a tela bloqueada");
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("PersonalTracker")
                .setContentText("Adquirindo posição a cada 1 s")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pending)
                .setOngoing(true)
                .build();
    }

    private File newLogFile() {
        File dir = getExternalFilesDir(null);
        if (dir == null) {
            dir = getFilesDir();
        }
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(dir, "acquisition_" + stamp + ".csv");
    }

    private CycleLogWriter openLog(File file) {
        try {
            CycleLogWriter writer = new CycleLogWriter(new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)));
            writer.writeHeader();
            return writer;
        } catch (IOException e) {
            AcquisitionRuntime.counters().recordLogError();
            return null;   // a aquisição segue sem registro
        }
    }
}
