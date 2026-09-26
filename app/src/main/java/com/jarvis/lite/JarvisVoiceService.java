package com.jarvis.lite;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;

public class JarvisVoiceService extends Service {

    public static final String ACTION_PAUSE =
            "com.jarvis.lite.ACTION_PAUSE";

    public static final String ACTION_RESUME =
            "com.jarvis.lite.ACTION_RESUME";

    private static final String CHANNEL_ID =
            "jarvis_voice_channel";

    private static final int NOTIFICATION_ID = 1001;

    private SpeechRecognizer speechRecognizer;

    private boolean ouvindo = false;
    private boolean pausado = true;

    @Override
    public void onCreate() {
        super.onCreate();

        criarCanalNotificacao();

        Notification notification =
                new Notification.Builder(this, CHANNEL_ID)
                        .setContentTitle("JARVIS Lite")
                        .setContentText(
                                "Escuta automática em espera"
                        )
                        .setSmallIcon(
                                android.R.drawable.ic_btn_speak_now
                        )
                        .setOngoing(true)
                        .build();

        startForeground(
                NOTIFICATION_ID,
                notification
        );

        // IMPORTANTE:
        // NÃO iniciar o microfone aqui.
        // O microfone só será iniciado por ACTION_RESUME.
    }

    private void criarCanalNotificacao() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "JARVIS — Comando de voz",
                            NotificationManager.IMPORTANCE_LOW
                    );

            channel.setDescription(
                    "Serviço de escuta da palavra JARVIS."
            );

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        if (intent == null) {
            pausado = true;
            pararReconhecedor();
            return START_NOT_STICKY;
        }

        String action =
                intent.getAction();

        if (ACTION_PAUSE.equals(action)) {

            pausado = true;

            pararReconhecedor();

            atualizarNotificacao(
                    "Escuta automática pausada"
            );

            return START_NOT_STICKY;
        }

        if (ACTION_RESUME.equals(action)) {

            pausado = false;

            atualizarNotificacao(
                    "Aguardando a palavra JARVIS"
            );

            if (!ouvindo) {
                iniciarEscuta();
            }

            return START_NOT_STICKY;
        }

        return START_NOT_STICKY;
    }

    private void iniciarEscuta() {

        if (pausado) {
            return;
        }

        if (Build.VERSION.SDK_INT < 31) {
            return;
        }

        if (!SpeechRecognizer
                .isOnDeviceRecognitionAvailable(this)) {
            return;
        }

        try {

            pararReconhecedor();

            speechRecognizer =
                    SpeechRecognizer
                            .createOnDeviceSpeechRecognizer(this);

            speechRecognizer.setRecognitionListener(
                    new RecognitionListener() {

                        @Override
                        public void onReadyForSpeech(
                                Bundle params) {

                            ouvindo = true;
                        }

                        @Override
                        public void onBeginningOfSpeech() {
                        }

                        @Override
                        public void onRmsChanged(
                                float rmsdB) {
                        }

                        @Override
                        public void onBufferReceived(
                                byte[] buffer) {
                        }

                        @Override
                        public void onEndOfSpeech() {

                            ouvindo = false;
                        }

                        @Override
                        public void onError(
                                int error) {

                            ouvindo = false;

                            pararReconhecedor();

                            if (!pausado) {
                                iniciarEscuta();
                            }
                        }

                        @Override
                        public void onResults(
                                Bundle results) {

                            ouvindo = false;

                            ArrayList<String> resultados =
                                    results.getStringArrayList(
                                            SpeechRecognizer
                                                    .RESULTS_RECOGNITION
                                    );

                            if (resultados != null
                                    && !resultados.isEmpty()) {

                                String texto =
                                        resultados
                                                .get(0)
                                                .toLowerCase(
                                                        Locale.getDefault()
                                                );

                                if (texto.contains("jarvis")) {

                                    abrirJarvis();

                                    return;
                                }
                            }

                            pararReconhecedor();

                            if (!pausado) {
                                iniciarEscuta();
                            }
                        }

                        @Override
                        public void onPartialResults(
                                Bundle partialResults) {
                        }

                        @Override
                        public void onEvent(
                                int eventType,
                                Bundle params) {
                        }
                    }
            );

            Intent intent =
                    new Intent(
                            RecognizerIntent
                                    .ACTION_RECOGNIZE_SPEECH
                    );

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "pt-BR"
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent
                            .LANGUAGE_MODEL_FREE_FORM
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    false
            );

            speechRecognizer.startListening(intent);

        } catch (Exception e) {

            ouvindo = false;
            pararReconhecedor();
        }
    }

    private void abrirJarvis() {

        // Primeiro libera COMPLETAMENTE o microfone.
        pausado = true;

        pararReconhecedor();

        Intent intent =
                new Intent(
                        JarvisVoiceService.this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
        );

        intent.putExtra(
                "JARVIS_WAKE",
                true
        );

        startActivity(intent);

        stopSelf();
    }

    private void pararReconhecedor() {

        if (speechRecognizer != null) {

            try {
                speechRecognizer.cancel();
            } catch (Exception e) {
                // Nada a fazer.
            }

            try {
                speechRecognizer.destroy();
            } catch (Exception e) {
                // Nada a fazer.
            }

            speechRecognizer = null;
        }

        ouvindo = false;
    }

    private void atualizarNotificacao(
            String texto) {

        try {

            Notification notification =
                    new Notification.Builder(
                            this,
                            CHANNEL_ID
                    )
                            .setContentTitle(
                                    "JARVIS Lite"
                            )
                            .setContentText(texto)
                            .setSmallIcon(
                                    android.R.drawable
                                            .ic_btn_speak_now
                            )
                            .setOngoing(true)
                            .build();

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            if (manager != null) {

                manager.notify(
                        NOTIFICATION_ID,
                        notification
                );
            }

        } catch (Exception e) {
            // Nada a fazer.
        }
    }

    @Override
    public void onDestroy() {

        pararReconhecedor();

        try {

            if (Build.VERSION.SDK_INT >= 24) {

                stopForeground(
                        STOP_FOREGROUND_REMOVE
                );

            } else {

                stopForeground(true);
            }

        } catch (Exception e) {
            // Nada a fazer.
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
