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

public class JarvisVoiceService extends Service {

    public static final String ACTION_PAUSE =
            "com.jarvis.lite.ACTION_PAUSE";

    public static final String ACTION_RESUME =
            "com.jarvis.lite.ACTION_RESUME";

    private static final String CHANNEL_ID =
            "jarvis_voice_channel";

    private static final int NOTIFICATION_ID = 1001;

    private SpeechRecognizer speechRecognizer;

    private Handler handler;

    private boolean ouvindo = false;
    private boolean pausado = true;

    /*
     * Indica se estamos procurando a palavra
     * "Jarvis" ou esperando um comando.
     */
    private boolean aguardandoWakeWord = true;

    /*
     * Uma única sessão compartilhada por todo
     * o sistema de voz.
     */
    private JarvisSessionManager sessionManager;

    private JarvisWakeWordManager wakeWordManager;
    private JarvisConversationEngine conversationEngine;
    private JarvisVoiceManager voiceManager;

    @Override
    public void onCreate() {
        super.onCreate();

        handler =
                new Handler(
                        getMainLooper()
                );

        /*
         * IMPORTANTE:
         *
         * Todos os componentes de voz usam
         * exatamente a mesma sessão.
         */
        sessionManager =
                new JarvisSessionManager(
                        getApplicationContext()
                );

        wakeWordManager =
                new JarvisWakeWordManager(
                        sessionManager
                );

        conversationEngine =
                new JarvisConversationEngine(
                        getApplicationContext(),
                        sessionManager
                );

        voiceManager =
                new JarvisVoiceManager(
                        getApplicationContext()
                );

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
                                "Escuta automática em espera"
                        )
                        .setSmallIcon(
                                android.R.drawable
                                        .ic_btn_speak_now
                        )
                        .setOngoing(true)
                        .build();

        startForeground(
                NOTIFICATION_ID,
                notification
        );
    }

    private void criarCanalNotificacao() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "JARVIS — Comando de voz",
                            NotificationManager
                                    .IMPORTANCE_LOW
                    );

            channel.setDescription(
                    "Serviço de escuta da palavra JARVIS."
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

            pausarServico();

            return START_NOT_STICKY;
        }

        if (ACTION_RESUME.equals(action)) {

            retomarServico();

            return START_NOT_STICKY;
        }

        return START_NOT_STICKY;
    }

    private void retomarServico() {

        pausado = false;

        aguardandoWakeWord = true;

        /*
         * Garantimos que a sessão começa
         * em estado de espera.
         */
        if (sessionManager != null
                && sessionManager.isActive()) {

            sessionManager.sleep();
        }

        atualizarNotificacao(
                "Aguardando a palavra JARVIS"
        );

        iniciarEscutaComAtraso(300);
    }

    private void pausarServico() {

        pausado = true;

        aguardandoWakeWord = true;

        pararReconhecedor();

        if (handler != null) {

            handler.removeCallbacksAndMessages(
                    null
            );
        }

        atualizarNotificacao(
                "Escuta automática pausada"
        );
    }

    private void iniciarEscutaComAtraso(
            long atraso) {

        if (handler == null) {
            return;
        }

        handler.postDelayed(
                new Runnable() {

                    @Override
                    public void run() {

                        if (!pausado
                                && !ouvindo) {

                            iniciarEscuta();
                        }
                    }
                },
                atraso
        );
    }

    private void iniciarEscuta() {

        if (pausado) {
            return;
        }

        if (Build.VERSION.SDK_INT < 31) {

            atualizarNotificacao(
                    "Reconhecimento offline "
                            + "requer Android 12 ou superior"
            );

            return;
        }

        if (!SpeechRecognizer
                .isOnDeviceRecognitionAvailable(
                        this)) {

            atualizarNotificacao(
                    "Reconhecimento de voz offline "
                            + "não disponível"
            );

            return;
        }

        try {

            pararReconhecedor();

            speechRecognizer =
                    SpeechRecognizer
                            .createOnDeviceSpeechRecognizer(
                                    this
                            );

            speechRecognizer
                    .setRecognitionListener(
                            criarRecognitionListener()
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

            intent.putExtra(
                    RecognizerIntent
                            .EXTRA_MAX_RESULTS,
                    3
            );

            speechRecognizer.startListening(
                    intent
            );

        } catch (Exception e) {

            ouvindo = false;

            pararReconhecedor();

            if (!pausado) {

                iniciarEscutaComAtraso(
                        1000
                );
            }
        }
    }

    private RecognitionListener
    criarRecognitionListener() {

        return new RecognitionListener() {

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

                    iniciarEscutaComAtraso(
                            700
                    );
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

                String texto = "";

                if (resultados != null
                        && !resultados.isEmpty()) {

                    texto =
                            resultados
                                    .get(0);
                }

                processarTextoReconhecido(
                        texto
                );
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
        };
    }

    private void processarTextoReconhecido(
            String texto) {

        if (pausado) {
            return;
        }

        if (texto == null
                || texto.trim().isEmpty()) {

            iniciarEscutaComAtraso(
                    300
            );

            return;
        }

        /*
         * JARVIS está dormindo.
         *
         * Procuramos somente a palavra
         * de ativação.
         */
        if (aguardandoWakeWord) {

            boolean ativou =
                    wakeWordManager
                            .processWakeText(
                                    texto
                            );

            if (ativou) {

                aguardandoWakeWord = false;

                atualizarNotificacao(
                        "JARVIS ativo — aguardando comando"
                );

                falar(
                        wakeWordManager
                                .getActivationMessage()
                );

                /*
                 * Depois da saudação, começamos
                 * a escutar o primeiro comando.
                 */
                iniciarEscutaComAtraso(
                        500
                );

                return;
            }

            iniciarEscutaComAtraso(
                    300
            );

            return;
        }

        /*
         * JARVIS já está em uma sessão ativa.
         */
        processarComandoDaSessao(
                texto
        );
    }

    private void processarComandoDaSessao(
            String texto) {

        /*
         * A ConversationEngine usa a mesma
         * JarvisSessionManager usada pelo
         * WakeWordManager.
         */
        JarvisConversationEngine
                .ConversationResult resultado =
                conversationEngine
                        .processCommand(
                                texto
                        );

        String resposta =
                resultado.getResponse();

        boolean sessaoAtiva =
                resultado
                        .isSessionActive();

        if (resposta != null
                && !resposta.trim().isEmpty()) {

            falar(resposta);
        }

        /*
         * Se o usuário disse
         * "encerrar sessão", voltamos para
         * o estado de espera.
         */
        if (!sessaoAtiva) {

            aguardandoWakeWord = true;

            atualizarNotificacao(
                    "Aguardando a palavra JARVIS"
            );

            iniciarEscutaComAtraso(
                    800
            );

            return;
        }

        /*
         * A sessão continua ativa.
         */
        aguardandoWakeWord = false;

        atualizarNotificacao(
                "JARVIS ativo — aguardando comando"
        );

        iniciarEscutaComAtraso(
                calcularAtrasoDepoisDaResposta(
                        resposta
                )
        );
    }

    private long calcularAtrasoDepoisDaResposta(
            String resposta) {

        if (resposta == null
                || resposta.trim().isEmpty()) {

            return 300;
        }

        /*
         * Evita iniciar outro reconhecimento
         * enquanto uma resposta curta ainda
         * está começando a ser reproduzida.
         */
        long atraso =
                800L
                + (long)
                Math.min(
                        resposta.length() * 8L,
                        2500L
                );

        return atraso;
    }

    private void falar(String texto) {

        if (texto == null
                || texto.trim().isEmpty()) {

            return;
        }

        try {

            voiceManager.speak(
                    texto
            );

        } catch (Exception e) {

            /*
             * Se a voz falhar, o serviço
             * continua funcionando.
             */
        }
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
                            .setContentText(
                                    texto
                            )
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

        pausado = true;

        pararReconhecedor();

        if (handler != null) {

            handler.removeCallbacksAndMessages(
                    null
            );
        }

        if (sessionManager != null) {

            try {

                sessionManager.sleep();

            } catch (Exception e) {
                // Nada a fazer.
            }
        }

        if (voiceManager != null) {

            try {

                voiceManager.stop();

            } catch (Exception e) {
                // Nada a fazer.
            }

            try {

                voiceManager.destroy();

            } catch (Exception e) {
                // Nada a fazer.
            }
        }

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
