package com.jarvis.lite;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class JarvisVoiceService extends Service {

    private static final String CHANNEL_ID = "jarvis_voice_channel";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onCreate() {
        super.onCreate();

        criarCanalNotificacao();

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("JARVIS Lite")
                .setContentText("Serviço de voz ativo")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "JARVIS — Comando de voz",
                    NotificationManager.IMPORTANCE_LOW
            );

            channel.setDescription(
                    "Indica quando o serviço de voz do JARVIS está ativo."
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // A detecção da palavra "JARVIS"
        // será adicionada no próximo passo.

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
