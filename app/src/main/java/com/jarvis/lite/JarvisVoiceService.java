
package com.jarvis.lite;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
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
    private boolean aguardandoWakeWord = true;

    private JarvisSessionManager sessionManager;
    private JarvisWakeWordManager wakeWordManager;
    private JarvisConversationEngine conversationEngine;
    private JarvisVoiceManager voiceManager;

    @Override
    public void onCreate() {
        super.onCreate();

        /*
         * Defesa adicional. O componente que inicia este serviço
         * também precisa verificar a permissão antes da chamada.
         */
        if (!temPermissaoMicrofone()) {
            stopSelf();
            return;
        }

        handler = new Handler(getMainLooper());

        sessionManager = new JarvisSessionManager(
                getApplicationContext()
        );

        wakeWordManager = new JarvisWakeWordManager(
                sessionManager
        );

        conversationEngine = new JarvisConversationEngine(
                getApplicationContext(),
                sessionManager
        );

        voiceManager = new JarvisVoiceManager(
                getApplicationContext()
        );

        criarCanalNotificacao();

        try {
            startForeground(
                    NOTIFICATION_ID,
                    criarNotificacao(
                            "Escuta automática em espera"
                    )
            );
        } catch (SecurityException e) {
            /*
             * O Android pode negar o início do serviço em primeiro
             * plano se as permissões necessárias não estiverem válidas.
             */
            pausado = true;
            stopSelf();
        }
    }

    private boolean temPermissaoMicrofone() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || checkSelfPermission(
                        android.Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED;
    }

    private Notification criarNotificacao(String texto) {
        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(
                    this,
                    CHANNEL_ID
            );
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle("JARVIS Lite")
                .setContentText(texto)
                .setSmallIcon(
                        android.R.drawable.ic_btn_speak_now
                )
                .setOngoing(true)
                .build();
    }

    private void criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "JARVIS — Comando de voz",
                NotificationManager.IMPORTANCE_LOW
        );

        channel.setDescription(
                "Serviço de escuta da palavra JARVIS."
        );

        NotificationManager manager =
                getSystemService(NotificationManager.class);

        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        if (!temPermissaoMicrofone()) {
            pausarServico();
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        if (intent == null) {
            pausarServico();
            return START_NOT_STICKY;
        }

        String action = intent.getAction();

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
        if (!temPermissaoMicrofone()) {
            pausarServico();
            stopSelf();
            return;
        }

        pausado = false;
        aguardandoWakeWord = true;

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

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        pararReconhecedor();

        atualizarNotificacao(
                "Escuta automática pausada"
        );
    }

    private void iniciarEscutaComAtraso(long atraso) {
        if (handler == null || pausado) {
            return;
        }

        /*
         * Evita acumular diversas tentativas pendentes de escuta.
         */
        handler.removeCallbacksAndMessages(null);

        handler.postDelayed(() -> {
            if (!pausado && !ouvindo) {
                iniciarEscuta();
            }
        }, Math.max(0L, atraso));
    }

    private void iniciarEscuta() {
        if (pausado) {
            return;
        }

        if (!temPermissaoMicrofone()) {
            pausarServico();
            stopSelf();
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            atualizarNotificacao(
                    "Reconhecimento offline requer Android 12 ou superior"
            );
            return;
        }

        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            atualizarNotificacao(
                    "Reconhecimento de voz offline não disponível"
            );
            return;
        }

        try {
            pararReconhecedor();

            speechRecognizer =
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(this);

            speechRecognizer.setRecognitionListener(
                    criarRecognitionListener()
            );

            Intent intent = new Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "pt-BR"
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    false
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    3
            );

            speechRecognizer.startListening(intent);

        } catch (SecurityException e) {
            pausado = true;
            pararReconhecedor();

            atualizarNotificacao(
                    "Permissão do microfone necessária"
            );

            stopSelf();

        } catch (Exception e) {
            ouvindo = false;
            pararReconhecedor();

            if (!pausado) {
                iniciarEscutaComAtraso(1000);
            }
        }
    }

    private RecognitionListener criarRecognitionListener() {
        return new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                ouvindo = true;
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                ouvindo = false;
            }

            @Override
            public void onError(int error) {
                ouvindo = false;
                pararReconhecedor();

                if (!pausado) {
                    iniciarEscutaComAtraso(700);
                }
            }

            @Override
            public void onResults(Bundle results) {
                ouvindo = false;

                ArrayList<String> resultados =
                        results == null
                                ? null
                                : results.getStringArrayList(
                                        SpeechRecognizer.RESULTS_RECOGNITION
                                );

                String texto = "";

                if (resultados != null && !resultados.isEmpty()) {
                    texto = resultados.get(0);
                }

                processarTextoReconhecido(texto);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        };
    }

    private void processarTextoReconhecido(String texto) {
        if (pausado) {
            return;
        }

        if (texto == null || texto.trim().isEmpty()) {
            iniciarEscutaComAtraso(300);
            return;
        }

        if (aguardandoWakeWord) {
            boolean ativou = wakeWordManager.processWakeText(texto);

            if (ativou) {
                aguardandoWakeWord = false;

                atualizarNotificacao(
                        "JARVIS ativo — aguardando comando"
                );

                falar(wakeWordManager.getActivationMessage());

                iniciarEscutaComAtraso(500);
                return;
            }

            iniciarEscutaComAtraso(300);
            return;
        }

        processarComandoDaSessao(texto);
    }

    private void processarComandoDaSessao(String texto) {
        if (conversationEngine == null) {
            aguardandoWakeWord = true;
            iniciarEscutaComAtraso(500);
            return;
        }

        JarvisConversationEngine.ConversationResult resultado =
                conversationEngine.processCommand(texto);

        if (resultado == null) {
            iniciarEscutaComAtraso(500);
            return;
        }

        String resposta = resultado.getResponse();
        boolean sessaoAtiva = resultado.isSessionActive();

        if (resposta != null && !resposta.trim().isEmpty()) {
            falar(resposta);
        }

        if (!sessaoAtiva) {
            aguardandoWakeWord = true;

            atualizarNotificacao(
                    "Aguardando a palavra JARVIS"
            );

            iniciarEscutaComAtraso(800);
            return;
        }

        aguardandoWakeWord = false;

        atualizarNotificacao(
                "JARVIS ativo — aguardando comando"
        );

        iniciarEscutaComAtraso(
                calcularAtrasoDepoisDaResposta(resposta)
        );
    }

    private long calcularAtrasoDepoisDaResposta(String resposta) {
        if (resposta == null || resposta.trim().isEmpty()) {
            return 300;
        }

        return 800L + Math.min(
                resposta.length() * 8L,
                2500L
        );
    }

    private void falar(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return;
        }

        if (voiceManager == null) {
            return;
        }

        try {
            voiceManager.speak(texto);
        } catch (Exception e) {
            /*
             * Uma falha de síntese de voz não deve encerrar
             * inesperadamente o serviço de reconhecimento.
             */
        }
    }

    private void pararReconhecedor() {
        if (speechRecognizer != null) {
            try {
                speechRecognizer.cancel();
            } catch (Exception ignored) {
            }

            try {
                speechRecognizer.destroy();
            } catch (Exception ignored) {
            }

            speechRecognizer = null;
        }

        ouvindo = false;
    }

    private void atualizarNotificacao(String texto) {
        try {
            Notification notification = criarNotificacao(texto);

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.notify(NOTIFICATION_ID, notification);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onDestroy() {
        pausado = true;

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        pararReconhecedor();

        if (sessionManager != null) {
            try {
                sessionManager.sleep();
            } catch (Exception ignored) {
            }
        }

        if (voiceManager != null) {
            try {
                voiceManager.stop();
            } catch (Exception ignored) {
            }

            try {
                voiceManager.destroy();
            } catch (Exception ignored) {
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
        } catch (Exception ignored) {
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
