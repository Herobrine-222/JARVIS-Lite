package com.jarvis.lite;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;

public class JarvisVoiceService extends Service {

    private static final String CHANNEL_ID =
            "jarvis_voice_channel";

    private static final int NOTIFICATION_ID = 1001;

    private SpeechRecognizer speechRecognizer;

    private Handler handler;

    private boolean ouvindo = false;
    private boolean iniciando = false;

    @Override
    public void onCreate() {

        super.onCreate();

        handler = new Handler();

        criarCanalNotificacao();

        Notification notification =
                new Notification.Builder(
                        this,
                        CHANNEL_ID
                )
                        .setContentTitle(
                                "JARVIS Lite"
                        )
                        .setContentText(
                                "Aguardando a palavra JARVIS"
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

        iniciarEscutaComAtraso();
    }

    private void criarCanalNotificacao() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "JARVIS — Comando de voz",
                            NotificationManager.IMPORTANCE_LOW
                    );

            channel.setDescription(
                    "Escuta da palavra JARVIS."
            );

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            if (manager != null) {
                manager.createNotificationChannel(
                        channel
                );
            }
        }
    }

    private void iniciarEscutaComAtraso() {

        if (handler == null) {
            return;
        }

        handler.postDelayed(
                () -> iniciarEscuta(),
                500
        );
    }

    private void iniciarEscuta() {

        if (ouvindo || iniciando) {
            return;
        }

        if (Build.VERSION.SDK_INT < 31) {
            return;
        }

        if (!SpeechRecognizer
                .isOnDeviceRecognitionAvailable(this)) {
            return;
        }

        iniciando = true;

        try {

            if (speechRecognizer != null) {

                speechRecognizer.destroy();

                speechRecognizer = null;
            }

            speechRecognizer =
                    SpeechRecognizer
                            .createOnDeviceSpeechRecognizer(
                                    this
                            );

            speechRecognizer.setRecognitionListener(
                    new RecognitionListener() {

                        @Override
                        public void onReadyForSpeech(
                                Bundle params) {

                            iniciando = false;
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
                            iniciando = false;

                            reiniciarEscuta();
                        }

                        @Override
                        public void onResults(
                                Bundle results) {

                            ouvindo = false;
                            iniciando = false;

                            ArrayList<String>
                                    resultados =
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
                                                        Locale
                                                                .getDefault()
                                                );

                                if (texto.contains(
                                        "jarvis")) {

                                    abrirJarvis();
                                }
                            }

                            reiniciarEscuta();
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

            speechRecognizer.startListening(
                    intent
            );

        } catch (Exception e) {

            ouvindo = false;
            iniciando = false;

            reiniciarEscuta();
        }
    }

    private void reiniciarEscuta() {

        if (handler == null) {
            return;
        }

        handler.postDelayed(
                () -> iniciarEscuta(),
                1000
        );
    }

    private void abrirJarvis() {

        Intent intent =
                new Intent(
                        this,
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
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        if (!ouvindo && !iniciando) {

            iniciarEscutaComAtraso();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {

        if (handler != null) {

            handler.removeCallbacksAndMessages(
                    null
            );
        }

        if (speechRecognizer != null) {

            try {
                speechRecognizer.destroy();

            } catch (Exception e) {
                // Nada a fazer.
            }

            speechRecognizer = null;
        }

        ouvindo = false;
        iniciando = false;

        super.onDestroy();
    }

    @Override
    public IBinder onBind(
            Intent intent) {

        return null;
    }
}
