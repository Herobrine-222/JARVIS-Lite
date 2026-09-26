package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQUEST_AUDIO = 100;

    private TextToSpeech tts;
    private boolean ttsReady = false;

    private TextView modeText;
    private TextView clockText;
    private TextView chatText;
    private EditText commandInput;

    private SharedPreferences preferences;
    private Handler clockHandler;

    private boolean onlineMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * JARVIS Lite V2
         * Offline por padrão.
         * Nenhuma permissão de INTERNET nesta etapa.
         */

        preferences = getSharedPreferences("jarvis_state", MODE_PRIVATE);

        // OFFLINE é sempre o modo inicial.
        onlineMode = false;
        preferences.edit().putBoolean("online_mode", false).apply();

        configurarTela();
        configurarTTS();
        iniciarRelogio();

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
                "Modo OFFLINE ativo. Digite um comando."
        );
    }

    private void configurarTela() {

        Window window = getWindow();

        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = window.getInsetsController();

            if (controller != null) {
                controller.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        }

        int padding = dp(16);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(padding, padding, padding, padding);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("J.A.R.V.I.S");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(8), 0, dp(4));

        TextView subtitle = new TextView(this);
        subtitle.setText("NÚCLEO OFFLINE • ETAPA 2");
        subtitle.setTextSize(13);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, dp(12));

        clockText = new TextView(this);
        clockText.setTextSize(18);
        clockText.setGravity(Gravity.CENTER);
        clockText.setPadding(0, 0, 0, dp(10));

        TextView reactor = new TextView(this);
        reactor.setText("◉");
        reactor.setTextSize(90);
        reactor.setGravity(Gravity.CENTER);
        reactor.setPadding(0, 0, 0, dp(4));

        modeText = new TextView(this);
        modeText.setText("🔴 MODO OFFLINE ATIVO");
        modeText.setTextSize(16);
        modeText.setGravity(Gravity.CENTER);
        modeText.setPadding(0, dp(4), 0, dp(10));

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
        commandInput.setMinLines(1);
        commandInput.setMaxLines(4);
        commandInput.setPadding(
                dp(12),
                dp(8),
                dp(12),
                dp(8)
        );

        Button executeButton = new Button(this);
        executeButton.setText("EXECUTAR COMANDO");

        executeButton.setOnClickListener(v -> {

            String command = commandInput.getText()
                    .toString()
                    .trim();

            if (command.isEmpty()) {
                return;
            }

            adicionarMensagem("VOCÊ", command);
            processarComando(command);

            commandInput.setText("");
        });

        Button onlineButton = new Button(this);
        onlineButton.setText("MODO ONLINE");

        onlineButton.setOnClickListener(v -> {

            /*
             * Nesta V2 o módulo online ainda não existe.
             * Portanto este botão não abre Internet.
             */

            responder(
                    "O módulo online ainda não está disponível nesta etapa. " +
                    "O JARVIS permanece totalmente offline."
            );
        });

        Button micButton = new Button(this);
        micButton.setText("GERENCIAR MICROFONE");

        micButton.setOnClickListener(v -> {

            Intent intent = new Intent(
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
        root.addView(modeText);
        root.addView(scrollView, scrollParams);
        root.addView(commandInput);
        root.addView(executeButton);
        root.addView(onlineButton);
        root.addView(micButton);

        setContentView(root);
    }

    private void configurarTTS() {

        tts = new TextToSpeech(
                this,
                status -> {

                    if (status == TextToSpeech.SUCCESS) {

                        int result = tts.setLanguage(
                                new Locale("pt", "BR")
                        );

                        ttsReady =
                                result != TextToSpeech.LANG_MISSING_DATA
                                && result != TextToSpeech.LANG_NOT_SUPPORTED;
                    }
                }
        );
    }

    private void iniciarRelogio() {

        clockHandler = new Handler();

        Runnable clockRunnable = new Runnable() {

            @Override
            public void run() {

                if (clockText != null) {

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

                    clockText.setText(
                            hora + "\n" + data
                    );
                }

                clockHandler.postDelayed(this, 1000);
            }
        };

        clockHandler.post(clockRunnable);
    }

    private void processarComando(String command) {

        String texto = command.toLowerCase(
                Locale.getDefault()
        );

        if (texto.contains("ajuda")
                || texto.contains("comandos")
                || texto.equals("help")) {

            responder(
                    "Comandos disponíveis: hora, data, bateria, " +
                    "status do telefone, quem é você e ajuda."
            );

            return;
        }

        if (texto.contains("que horas")
                || texto.equals("hora")
                || texto.contains("horas são")) {

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

        if (texto.contains("qual a data")
                || texto.contains("que dia")
                || texto.equals("data")) {

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
                || texto.contains("estado do telefone")
                || texto.contains("estado do celular")) {

            responder(
                    obterStatus()
            );

            return;
        }

        if (texto.contains("quem é você")
                || texto.contains("quem voce e")
                || texto.contains("o que você é")
                || texto.contains("o que voce e")) {

            responder(
                    "Eu sou o JARVIS Lite, seu assistente local. " +
                    "Nesta etapa funciono offline e executo comandos básicos."
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

        if (texto.equals("oi")
                || texto.equals("olá")
                || texto.equals("ola")
                || texto.contains("bom dia")
                || texto.contains("boa tarde")
                || texto.contains("boa noite")) {

            responder(
                    "À sua disposição. Como posso ajudar?"
            );

            return;
        }

        responder(
                "Comando recebido, mas ainda não tenho uma função " +
                "local para executá-lo nesta etapa."
        );
    }

    private String obterBateria() {

        BatteryManager batteryManager =
                (BatteryManager) getSystemService(
                        Context.BATTERY_SERVICE
                );

        int nivel =
                batteryManager.getIntProperty(
                        BatteryManager.BATTERY_PROPERTY_CAPACITY
                );

        Intent batteryIntent =
                registerReceiver(
                        null,
                        new android.content.IntentFilter(
                                Intent.ACTION_BATTERY_CHANGED
                        )
                );

        boolean carregando = false;

        if (batteryIntent != null) {

            int status =
                    batteryIntent.getIntExtra(
                            BatteryManager.EXTRA_STATUS,
                            -1
                    );

            carregando =
                    status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;
        }

        if (carregando) {

            return "Bateria em " + nivel +
                    "%. O aparelho está carregando.";
        }

        return "Bateria em " + nivel +
                "%. O aparelho não está carregando.";
    }

    private String obterStatus() {

        String hora =
                new SimpleDateFormat(
                        "HH:mm",
                        Locale.getDefault()
                ).format(new Date());

        String bateria = obterBateria();

        return "Status do telefone: " +
                bateria +
                " Hora: " +
                hora +
                ". Modo OFFLINE ativo.";
    }

    private void responder(String resposta) {

        adicionarMensagem(
                "JARVIS",
                resposta
        );

        if (ttsReady && tts != null) {

            tts.speak(
                    resposta,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "jarvis_response"
            );
        }
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

        atual += autor + ": " + mensagem;

        chatText.setText(atual);
    }

    private int dp(int value) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return (int) (value * density + 0.5f);
    }

    @Override
    protected void onDestroy() {

        if (clockHandler != null) {
            clockHandler.removeCallbacksAndMessages(null);
        }

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }
 }
