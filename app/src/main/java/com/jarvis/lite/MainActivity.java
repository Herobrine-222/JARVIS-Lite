package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQUEST_AUDIO = 100;

    private TextView clockText;
    private TextView chatText;
    private EditText commandInput;

    private Handler clockHandler;

    private TextToSpeech tts;
    private boolean ttsReady = false;

    private SpeechRecognizer speechRecognizer;
    private boolean ouvindo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * PRIMEIRA PROVA:
         *
         * O serviço automático de voz não participa
         * do reconhecimento enquanto esta Activity estiver aberta.
         *
         * Isso deixa o botão manual como único responsável
         * pelo microfone.
         */
        try {
            stopService(
                    new Intent(
                            this,
                            JarvisVoiceService.class
                    )
            );
        } catch (Exception e) {
            // Nada a fazer.
        }

        configurarTela();
        iniciarRelogio();
        iniciarVoz();

        if (android.os.Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.RECORD_AUDIO
                        },
                        REQUEST_AUDIO
                );
            }
        }

        adicionarMensagem(
                "JARVIS",
                "À sua disposição. Sistemas locais operacionais."
        );

        adicionarMensagem(
                "JARVIS",
                "Modo OFFLINE ativo. Digite ou fale um comando."
        );

        /*
         * IMPORTANTE:
         *
         * Nesta primeira prova NÃO vamos iniciar
         * o reconhecimento automaticamente através
         * do JARVIS_WAKE.
         *
         * O microfone só será usado quando o usuário
         * tocar em "FALAR COM JARVIS".
         */
    }

    /*
     * ============================================================
     * VOZ / TTS
     * ============================================================
     */

    private void iniciarVoz() {

        try {

            tts = new TextToSpeech(
                    getApplicationContext(),
                    status -> {

                        if (status == TextToSpeech.SUCCESS) {

                            try {

                                int resultado =
                                        tts.setLanguage(
                                                new Locale(
                                                        "pt",
                                                        "BR"
                                                )
                                        );

                                if (resultado
                                        != TextToSpeech.LANG_MISSING_DATA
                                        && resultado
                                        != TextToSpeech.LANG_NOT_SUPPORTED) {

                                    ttsReady = true;

                                } else {

                                    ttsReady = false;
                                }

                            } catch (Exception e) {

                                ttsReady = false;
                            }

                        } else {

                            ttsReady = false;
                        }
                    }
            );

        } catch (Exception e) {

            tts = null;
            ttsReady = false;
        }
    }

    private void falar(String texto) {

        if (!ttsReady || tts == null) {
            return;
        }

        try {

            tts.speak(
                    texto,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "jarvis_resposta"
            );

        } catch (Exception e) {
            // Continua funcionando em texto.
        }
    }

    /*
     * ============================================================
     * RECONHECIMENTO DE VOZ OFFLINE
     * ============================================================
     */

    private void iniciarReconhecimento() {

        /*
         * Garante que qualquer reconhecimento anterior
         * seja encerrado antes de criar outro.
         */
        pararReconhecimento();

        if (android.os.Build.VERSION.SDK_INT < 31) {

            responder(
                    "O reconhecimento de voz local desta versão "
                            + "do JARVIS exige Android 12 ou superior."
            );

            return;
        }

        if (checkSelfPermission(
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            responder(
                    "A permissão do microfone ainda não foi concedida."
            );

            requestPermissions(
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    REQUEST_AUDIO
            );

            return;
        }

        if (!SpeechRecognizer
                .isOnDeviceRecognitionAvailable(this)) {

            responder(
                    "O reconhecimento de voz offline não está "
                            + "disponível neste aparelho."
            );

            return;
        }

        try {

            speechRecognizer =
                    SpeechRecognizer
                            .createOnDeviceSpeechRecognizer(this);

            speechRecognizer.setRecognitionListener(
                    new RecognitionListener() {

                        @Override
                        public void onReadyForSpeech(
                                Bundle params) {

                            ouvindo = true;

                            adicionarMensagem(
                                    "JARVIS",
                                    "Estou ouvindo..."
                            );
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

                            String mensagem;

                            switch (error) {

                                case SpeechRecognizer.ERROR_AUDIO:

                                    mensagem =
                                            "Não consegui acessar o áudio.";

                                    break;

                                case SpeechRecognizer.ERROR_NO_MATCH:

                                    mensagem =
                                            "Não consegui entender o que foi dito.";

                                    break;

                                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:

                                    mensagem =
                                            "Não detectei nenhuma fala.";

                                    break;

                                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:

                                    mensagem =
                                            "O reconhecimento de voz estava ocupado.";

                                    break;

                                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:

                                    mensagem =
                                            "A permissão do microfone não está disponível.";

                                    break;

                                default:

                                    mensagem =
                                            "Não foi possível reconhecer o comando. "
                                                    + "Código: "
                                                    + error;

                                    break;
                            }

                            responder(mensagem);

                            liberarReconhecedor();
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

                            if (resultados == null
                                    || resultados.isEmpty()) {

                                responder(
                                        "Não consegui entender o comando."
                                );

                                liberarReconhecedor();

                                return;
                            }

                            String comando =
                                    resultados.get(0);

                            adicionarMensagem(
                                    "VOCÊ",
                                    comando
                            );

                            liberarReconhecedor();

                            processarComando(comando);
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

            /*
             * Mantém o reconhecimento estritamente
             * no reconhecedor local.
             */
            speechRecognizer.startListening(intent);

        } catch (Exception e) {

            ouvindo = false;

            liberarReconhecedor();

            responder(
                    "Não foi possível iniciar o reconhecimento de voz."
            );
        }
    }

    private void pararReconhecimento() {

        ouvindo = false;

        if (speechRecognizer != null) {

            try {
                speechRecognizer.cancel();
            } catch (Exception e) {
            }

            try {
                speechRecognizer.destroy();
            } catch (Exception e) {
            }

            speechRecognizer = null;
        }
    }

    private void liberarReconhecedor() {

        if (speechRecognizer != null) {

            try {
                speechRecognizer.destroy();
            } catch (Exception e) {
            }

            speechRecognizer = null;
        }

        ouvindo = false;
    }

    /*
     * ============================================================
     * INTERFACE
     * ============================================================
     */

    private void configurarTela() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                24,
                24,
                24,
                24
        );

        root.setGravity(
                Gravity.CENTER_HORIZONTAL
        );

        TextView titulo =
                new TextView(this);

        titulo.setText(
                "J.A.R.V.I.S"
        );

        titulo.setTextSize(30);

        titulo.setGravity(
                Gravity.CENTER
        );

        root.addView(
                titulo,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        TextView subtitulo =
                new TextView(this);

        subtitulo.setText(
                "NÚCLEO OFFLINE • V2"
        );

        subtitulo.setTextSize(14);

        subtitulo.setGravity(
                Gravity.CENTER
        );

        root.addView(
                subtitulo,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        clockText =
                new TextView(this);

        clockText.setTextSize(28);

        clockText.setGravity(
                Gravity.CENTER
        );

        LinearLayout.LayoutParams clockParams =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        clockParams.topMargin = 18;

        root.addView(
                clockText,
                clockParams
        );

        TextView reactor =
                new TextView(this);

        reactor.setText(
                "◉"
        );

        reactor.setTextSize(90);

        reactor.setGravity(
                Gravity.CENTER
        );

        LinearLayout.LayoutParams reactorParams =
                new LinearLayout.LayoutParams(
                        -1,
                        150
                );

        reactorParams.topMargin = 10;

        root.addView(
                reactor,
                reactorParams
        );

        TextView modo =
                new TextView(this);

        modo.setText(
                "🔴 MODO OFFLINE ATIVO"
        );

        modo.setTextSize(16);

        modo.setGravity(
                Gravity.CENTER
        );

        root.addView(
                modo,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        chatText =
                new TextView(this);

        chatText.setTextSize(16);

        chatText.setPadding(
                12,
                12,
                12,
                12
        );

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(chatText);

        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                );

        scrollParams.topMargin = 12;

        root.addView(
                scroll,
                scrollParams
        );

        commandInput =
                new EditText(this);

        commandInput.setHint(
                "Digite um comando..."
        );

        root.addView(
                commandInput,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        Button executar =
                new Button(this);

        executar.setText(
                "EXECUTAR COMANDO"
        );

        executar.setOnClickListener(
                v -> {

                    String comando =
                            commandInput
                                    .getText()
                                    .toString()
                                    .trim();

                    if (comando.isEmpty()) {
                        return;
                    }

                    adicionarMensagem(
                            "VOCÊ",
                            comando
                    );

                    processarComando(comando);

                    commandInput.setText("");
                }
        );

        root.addView(
                executar,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        Button falar =
                new Button(this);

        falar.setText(
                "🎙️ FALAR COM JARVIS"
        );

        falar.setOnClickListener(
                v -> {

                    if (!ouvindo) {

                        iniciarReconhecimento();

                    } else {

                        pararReconhecimento();

                        adicionarMensagem(
                                "JARVIS",
                                "Reconhecimento interrompido."
                        );
                    }
                }
        );

        root.addView(
                falar,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        Button online =
                new Button(this);

        online.setText(
                "MODO ONLINE"
        );

        online.setOnClickListener(
                v -> responder(
                        "O módulo online ainda não está disponível."
                )
        );

        root.addView(
                online,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        Button microfone =
                new Button(this);

        microfone.setText(
                "GERENCIAR MICROFONE"
        );

        LinearLayout.LayoutParams micParams =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        micParams.topMargin = 10;

        root.addView(
                microfone,
                micParams
        );

        microfone.setOnClickListener(
                v -> {

                    try {

                        Intent intent =
                                new Intent(
                                        Settings
                                                .ACTION_APPLICATION_DETAILS_SETTINGS
                                );

                        intent.setData(
                                Uri.parse(
                                        "package:"
                                                + getPackageName()
                                )
                        );

                        startActivity(intent);

                    } catch (Exception e) {
                    }
                }
        );

        setContentView(root);
    }

    /*
     * ============================================================
     * RELÓGIO
     * ============================================================
     */

    private void iniciarRelogio() {

        clockHandler =
                new Handler();

        Runnable atualizar =
                new Runnable() {

                    @Override
                    public void run() {

                        if (clockText != null) {

                            String hora =
                                    new SimpleDateFormat(
                                            "HH:mm:ss",
                                            Locale.getDefault()
                                    ).format(
                                            new Date()
                                    );

                            String data =
                                    new SimpleDateFormat(
                                            "dd/MM/yyyy",
                                            Locale.getDefault()
                                    ).format(
                                            new Date()
                                    );

                            clockText.setText(
                                    hora
                                            + "\n"
                                            + data
                            );
                        }

                        clockHandler.postDelayed(
                                this,
                                1000
                        );
                    }
                };

        clockHandler.post(
                atualizar
        );
    }

    /*
     * ============================================================
     * COMANDOS
     * ============================================================
     */

    private void processarComando(
            String comandoOriginal) {

        String comando =
                comandoOriginal
                        .toLowerCase(
                                Locale.getDefault()
                        )
                        .trim();

        if (comando.contains("ajuda")
                || comando.contains("comandos")) {

            responder(
                    "Comandos disponíveis: hora, data, "
                            + "bateria, status e quem é você."
            );

            return;
        }

        if (comando.contains("hora")) {

            String hora =
                    new SimpleDateFormat(
                            "HH:mm:ss",
                            Locale.getDefault()
                    ).format(
                            new Date()
                    );

            responder(
                    "Agora são "
                            + hora
            );

            return;
        }

        if (comando.contains("data")
                || comando.contains("dia")) {

            String data =
                    new SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                    ).format(
                            new Date()
                    );

            responder(
                    "A data atual é "
                            + data
            );

            return;
        }

        if (comando.contains("bateria")
                || comando.contains("carga")) {

            responder(
                    obterBateria()
            );

            return;
        }

        if (comando.contains("status")
                || comando.contains("estado")) {

            responder(
                    obterStatus()
            );

            return;
        }

        if (comando.contains("quem é você")
                || comando.contains("quem voce e")
                || comando.contains("quem é voce")
                || comando.contains("quem voce é")) {

            responder(
                    "Eu sou o JARVIS Lite, "
                            + "um assistente local offline."
            );

            return;
        }

        if (comando.contains("jarvis")
                && (comando.contains("está aí")
                || comando.contains("esta ai"))) {

            responder(
                    "À sua disposição. "
                            + "Sistemas locais operacionais."
            );

            return;
        }

        responder(
                "Não tenho uma função local "
                        + "para esse comando ainda."
        );
    }

    /*
     * ============================================================
     * BATERIA
     * ============================================================
     */

    private String obterBateria() {

        try {

            BatteryManager batteryManager =
                    (BatteryManager)
                            getSystemService(
                                    BATTERY_SERVICE
                            );

            if (batteryManager == null) {

                return "Não consegui obter "
                        + "o nível da bateria.";
            }

            int nivel =
                    batteryManager.getIntProperty(
                            BatteryManager
                                    .BATTERY_PROPERTY_CAPACITY
                    );

            return "A bateria está em "
                    + nivel
                    + "%.";

        } catch (Exception e) {

            return "Não consegui obter "
                    + "o nível da bateria.";
        }
    }

    /*
     * ============================================================
     * STATUS
     * ============================================================
     */

    private String obterStatus() {

        String hora =
                new SimpleDateFormat(
                        "HH:mm:ss",
                        Locale.getDefault()
                ).format(
                        new Date()
                );

        return "Status do sistema: "
                + obterBateria()
                + " Hora "
                + hora
                + ". Modo OFFLINE ativo.";
    }

    /*
     * ============================================================
     * RESPOSTA
     * ============================================================
     */

    private void responder(
            String mensagem) {

        adicionarMensagem(
                "JARVIS",
                mensagem
        );

        falar(mensagem);
    }

    private void adicionarMensagem(
            String autor,
            String mensagem) {

        if (chatText == null) {
            return;
        }

        String atual =
                chatText.getText()
                        .toString();

        if (!atual.isEmpty()) {
            atual += "\n\n";
        }

        atual +=
                autor
                        + ": "
                        + mensagem;

        chatText.setText(
                atual
        );
    }

    /*
     * ============================================================
     * ENCERRAMENTO
     * ============================================================
     */

    @Override
    protected void onDestroy() {

        if (clockHandler != null) {

            clockHandler.removeCallbacksAndMessages(
                    null
            );
        }

        pararReconhecimento();

        if (tts != null) {

            try {

                tts.stop();
                tts.shutdown();

            } catch (Exception e) {
            }

            tts = null;
        }

        super.onDestroy();
    }
}
