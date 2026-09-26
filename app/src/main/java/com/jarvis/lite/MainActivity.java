package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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

        configurarTela();
        iniciarRelogio();
        iniciarVoz();

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.RECORD_AUDIO},
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
    }

    private void iniciarVoz() {

        try {
            tts = new TextToSpeech(
                    getApplicationContext(),
                    status -> {

                        if (status == TextToSpeech.SUCCESS) {

                            try {

                                int resultado =
                                        tts.setLanguage(
                                                new Locale("pt", "BR")
                                        );

                                if (resultado != TextToSpeech.LANG_MISSING_DATA
                                        && resultado != TextToSpeech.LANG_NOT_SUPPORTED) {

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
            // O JARVIS continua funcionando em texto.
        }
    }

    private void iniciarReconhecimento() {

        if (android.os.Build.VERSION.SDK_INT < 31) {

            responder(
                    "O reconhecimento de voz local desta versão " +
                    "do JARVIS exige Android 12 ou superior."
            );

            return;
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            responder(
                    "A permissão do microfone ainda não foi concedida."
            );

            return;
        }

        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {

            responder(
                    "O reconhecimento de voz offline não está " +
                    "disponível neste aparelho."
            );

            return;
        }

        try {

            if (speechRecognizer != null) {
                speechRecognizer.destroy();
                speechRecognizer = null;
            }

            speechRecognizer =
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(this);

            speechRecognizer.setRecognitionListener(
                    new RecognitionListener() {

                        @Override
                        public void onReadyForSpeech(Bundle params) {
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

                                default:
                                    mensagem =
                                            "Não foi possível reconhecer o comando.";
                                    break;
                            }

                            responder(mensagem);
                        }

                        @Override
                        public void onResults(Bundle results) {

                            ouvindo = false;

                            ArrayList<String> resultados =
                                    results.getStringArrayList(
                                            SpeechRecognizer.RESULTS_RECOGNITION
                                    );

                            if (resultados == null
                                    || resultados.isEmpty()) {

                                responder(
                                        "Não consegui entender o comando."
                                );

                                return;
                            }

                            String comando = resultados.get(0);

                            adicionarMensagem(
                                    "VOCÊ",
                                    comando
                            );

                            processarComando(comando);
                        }

                        @Override
                        public void onPartialResults(Bundle partialResults) {
                        }

                        @Override
                        public void onEvent(
                                int eventType,
                                Bundle params
                        ) {
                        }
                    }
            );

            Intent intent =
                    new Intent(
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

            speechRecognizer.startListening(intent);

        } catch (Exception e) {

            ouvindo = false;

            responder(
                    "Não foi possível iniciar o reconhecimento de voz."
            );
        }
    }

    private void configurarTela() {

        int padding = dp(16);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(
                padding,
                padding,
                padding,
                padding
        );
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("J.A.R.V.I.S");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText("NÚCLEO OFFLINE • V2");
        subtitle.setTextSize(13);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(6), 0, dp(8));

        clockText = new TextView(this);
        clockText.setTextSize(18);
        clockText.setGravity(Gravity.CENTER);
        clockText.setPadding(0, 0, 0, dp(8));

        TextView reactor = new TextView(this);
        reactor.setText("◉");
        reactor.setTextSize(80);
        reactor.setGravity(Gravity.CENTER);

        TextView mode = new TextView(this);
        mode.setText("🔴 MODO OFFLINE ATIVO");
        mode.setTextSize(16);
        mode.setGravity(Gravity.CENTER);
        mode.setPadding(0, dp(4), 0, dp(8));

        ScrollView scrollView = new ScrollView(this);

        chatText = new TextView(this);
        chatText.setTextSize(16);
        chatText.setPadding(
                dp(10),
                dp(10),
                dp(10),
                dp(10)
        );

        scrollView.addView(chatText);

        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                );

        commandInput = new EditText(this);
        commandInput.setHint("Digite um comando...");
        commandInput.setSingleLine(false);
        commandInput.setMaxLines(3);

        Button executeButton = new Button(this);
        executeButton.setText("EXECUTAR COMANDO");

        executeButton.setOnClickListener(v -> {

            String command =
                    commandInput.getText()
                            .toString()
                            .trim();

            if (command.isEmpty()) {
                return;
            }

            adicionarMensagem(
                    "VOCÊ",
                    command
            );

            processarComando(command);

            commandInput.setText("");
        });

        Button voiceButton = new Button(this);
        voiceButton.setText("🎙️ FALAR COM JARVIS");

        voiceButton.setOnClickListener(v -> {

            if (!ouvindo) {
                iniciarReconhecimento();
            }
        });

        Button onlineButton = new Button(this);
        onlineButton.setText("MODO ONLINE");

        onlineButton.setOnClickListener(v ->
                responder(
                        "O módulo online ainda não está disponível. " +
                        "O JARVIS continua offline."
                )
        );

        Button micButton = new Button(this);
        micButton.setText("GERENCIAR MICROFONE");

        LinearLayout.LayoutParams micParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        micParams.topMargin = dp(10);

        micButton.setLayoutParams(micParams);

        micButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    );

            intent.setData(
                    android.net.Uri.parse(
                            "package:" + getPackageName()
                    )
            );

            startActivity(intent);
        });

        root.addView(title);
        root.addView(subtitle);
        root.addView(clockText);
        root.addView(reactor);
        root.addView(mode);

        root.addView(
                scrollView,
                scrollParams
        );

        root.addView(commandInput);
        root.addView(executeButton);
        root.addView(voiceButton);
        root.addView(onlineButton);
        root.addView(micButton);

        setContentView(root);
    }

    private void iniciarRelogio() {

        clockHandler = new Handler();

        Runnable atualizar = new Runnable() {

            @Override
            public void run() {

                String hora =
                        new SimpleDateFormat(
                                "HH:mm:ss",
                                Locale.getDefault()
                        ).format(new Date());

                String data =
                        new SimpleDateFormat(
                                "dd/MM/yyyy",
                                Locale.getDefault()
                        ).format(new Date());

                if (clockText != null) {

                    clockText.setText(
                            hora + "\n" + data
                    );
                }

                clockHandler.postDelayed(
                        this,
                        1000
                );
            }
        };

        clockHandler.post(atualizar);
    }

    private void processarComando(String command) {

        String texto =
                command.toLowerCase(
                        Locale.getDefault()
                );

        if (texto.contains("ajuda")
                || texto.contains("comandos")) {

            responder(
                    "Comandos disponíveis: hora, data, " +
                    "bateria, status, quem é você e ajuda."
            );

            return;
        }

        if (texto.contains("hora")) {

            String hora =
                    new SimpleDateFormat(
                            "HH:mm",
                            Locale.getDefault()
                    ).format(new Date());

            responder(
                    "Agora são " + hora + "."
            );

            return;
        }

        if (texto.contains("data")
                || texto.contains("dia")) {

            String data =
                    new SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                    ).format(new Date());

            responder(
                    "A data atual é " + data + "."
            );

            return;
        }

        if (texto.contains("bateria")
                || texto.contains("carga")) {

            responder(
                    obterBateria()
            );

            return;
        }

        if (texto.contains("status")
                || texto.contains("estado")) {

            responder(
                    obterStatus()
            );

            return;
        }

        if (texto.contains("quem é você")
                || texto.contains("quem voce e")) {

            responder(
                    "Eu sou o JARVIS Lite, seu assistente local. " +
                    "Nesta versão funciono offline."
            );

            return;
        }

        if (texto.contains("jarvis")
                && (texto.contains("está aí")
                || texto.contains("esta ai"))) {

            responder(
                    "À sua disposição. Sistemas locais operacionais."
            );

            return;
        }

        responder(
                "Comando recebido. Ainda não tenho uma função " +
                "local para esse comando."
        );
    }

    private String obterBateria() {

        BatteryManager manager =
                (BatteryManager)
                        getSystemService(
                                Context.BATTERY_SERVICE
                        );

        int nivel =
                manager.getIntProperty(
                        BatteryManager.BATTERY_PROPERTY_CAPACITY
                );

        return "Bateria em " +
                nivel +
                "%.";
    }

    private String obterStatus() {

        String hora =
                new SimpleDateFormat(
                        "HH:mm",
                        Locale.getDefault()
                ).format(new Date());

        return "Status: bateria em " +
                obterBateria() +
                " Hora: " +
                hora +
                ". Modo OFFLINE.";
    }

    private void responder(String resposta) {

        adicionarMensagem(
                "JARVIS",
                resposta
        );

        falar(resposta);
    }

    private void adicionarMensagem(
            String autor,
            String mensagem
    ) {

        if (chatText == null) {
            return;
        }

        String atual =
                chatText.getText().toString();

        if (!atual.isEmpty()) {
            atual += "\n\n";
        }

        atual +=
                autor +
                ": " +
                mensagem;

        chatText.setText(atual);
    }

    private int dp(int value) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return (int)
                (value * density + 0.5f);
    }

    @Override
    protected void onDestroy() {

        if (clockHandler != null) {
            clockHandler.removeCallbacksAndMessages(null);
        }

        if (speechRecognizer != null) {

            try {
                speechRecognizer.destroy();
            } catch (Exception e) {
                // Nada a fazer.
            }

            speechRecognizer = null;
        }

        if (tts != null) {

            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception e) {
                // Nada a fazer.
            }

            tts = null;
            ttsReady = false;
        }

        super.onDestroy();
    }
}
