package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.net.Uri;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.view.animation.AlphaAnimation;
import android.view.animation.TranslateAnimation;
import android.view.animation.AnimationSet;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private static final int REQUEST_AUDIO = 100;

    private TextView clockText;
    private LinearLayout chatContainer;
    private ScrollView chatScroll;
    private EditText commandInput;
    private TextView homeDateText;
    private TextView homeWeatherText;
    private boolean chatMode = false;

    private boolean verificacaoEncontrouSuspeito = false;
    private String nomeAppSuspeito = "";
    private String detalheAppSuspeito = "";

    private Handler clockHandler;

    private TextToSpeech tts;
    private boolean ttsReady = false;

    private SpeechRecognizer speechRecognizer;
    private boolean ouvindo = false;

    private boolean modoOnline = false;

    private boolean microfoneAtivado = true;
    private boolean notificacoesAtivadas = true;
    private boolean statusAtivado = true;
    private boolean arquivosMidiaAtivados = false;

    private SharedPreferences preferencias;
    private long ultimaVerificacao = 0L;

    private ReactorView reactorView;

    private JarvisMemory memoria;
    private boolean memoriaAutomatica = true;
private final JarvisBrain cerebro =
        new JarvisBrain();
    private static final int TELA_PRINCIPAL = 0;
    private static final int TELA_MENU_JARVIS = 1;
    private static final int TELA_GERENCIAR_JARVIS = 2;
    private static final int TELA_PRIVACIDADE = 3;
    private static final int TELA_VERIFICACAO = 4;
    private static final int TELA_COMANDO_VOZ = 5;
    private static final int TELA_GERENCIAR_VOZ = 6;
    private static final int TELA_MEMORIA = 7;
    private static final int TELA_HISTORICO = 8;

    private final ArrayList<Integer> mensagensSelecionadas =
            new ArrayList<>();

    private int telaAtual = TELA_PRINCIPAL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
        }

        preferencias =
                getSharedPreferences("jarvis_config", MODE_PRIVATE);

        ultimaVerificacao =
                preferencias.getLong("ultima_verificacao", 0L);

        memoria = new JarvisMemory(this);

        memoriaAutomatica =
                preferencias.getBoolean("memoria_automatica", true);

        modoOnline =
                preferencias.getBoolean("modo_online", false);

        microfoneAtivado =
                preferencias.getBoolean("microfone_ativo", true);

        notificacoesAtivadas =
                preferencias.getBoolean("notificacoes_ativas", true);

        statusAtivado =
                preferencias.getBoolean("status_ativo", true);

        arquivosMidiaAtivados =
                preferencias.getBoolean("arquivos_midia_ativos", false);

        configurarTela();
        restaurarChatPrincipal();
        iniciarRelogio();
        iniciarVoz();

        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(
                        Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    REQUEST_AUDIO
            );
        }

        if (!preferencias.getBoolean(
                "chat_inicializado",
                false
        ) && obterHistorico().length() == 0) {

            adicionarMensagem(
                    "JARVIS",
                    saudacaoPorHorario()
            );

            adicionarMensagem(
                    "JARVIS",
                    modoOnline
                            ? "Modo ONLINE ativo."
                            : "Modo OFFLINE ativo."
            );

            preferencias.edit()
                    .putBoolean("chat_inicializado", true)
                    .apply();
        }
    }

    private int dp(int valor) {
        return Math.round(
                valor *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

   private void configurarAreaSegura(View root) {

    if (Build.VERSION.SDK_INT >= 30) {

        root.setOnApplyWindowInsetsListener(
                (v, insets) -> {

                    Insets barras =
                            insets.getInsets(
                                    WindowInsets.Type.statusBars()
                                            | WindowInsets.Type.navigationBars()
                            );

                    Insets teclado =
                            insets.getInsets(
                                    WindowInsets.Type.ime()
                            );

                    int parteInferior =
                            Math.max(
                                    barras.bottom,
                                    teclado.bottom
                            );

                    v.setPadding(
                            dp(18),
                            dp(10) + barras.top,
                            dp(18),
                            dp(24) + parteInferior
                    );

                    return insets;
                }
        );

        root.requestApplyInsets();

    } else {

        root.setPadding(
                dp(18),
                dp(10),
                dp(18),
                dp(24)
        );
    }
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
                                                new Locale(
                                                        "pt",
                                                        "BR"
                                                )
                                        );

                                ttsReady =
                                        resultado !=
                                                TextToSpeech
                                                        .LANG_MISSING_DATA
                                                &&
                                        resultado !=
                                                TextToSpeech
                                                        .LANG_NOT_SUPPORTED;

                                if (ttsReady) {
                                    tts.setSpeechRate(1.0f);
                                    tts.setPitch(0.85f);
                                    aplicarVozSalva();
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
        if (!ttsReady || tts == null || texto == null) {
            return;
        }

        try {

            tts.speak(
                    texto,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "jarvis_resposta"
            );

        } catch (Exception ignored) {
        }
    }

    private void iniciarReconhecimento() {
        pararReconhecimento();

        if (!microfoneAtivado) {
            responder(
                    "O microfone está desativado no Gerenciar JARVIS."
            );
            return;
        }

        if (Build.VERSION.SDK_INT < 31) {
            responder(
                    "O reconhecimento de voz local desta versão exige Android 12 ou superior."
            );
            return;
        }

        if (!microfonePermitido()) {

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
                    "O reconhecimento de voz offline não está disponível neste aparelho."
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

                            if (reactorView != null) {
                                reactorView.setOuvindo(true);
                            }
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
                        public void onError(int error) {

                            ouvindo = false;

                            if (reactorView != null) {
                                reactorView.setOuvindo(false);
                            }

                            if (error == SpeechRecognizer.ERROR_NO_MATCH
                                    || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                                liberarReconhecedor();
                                return;
                            }

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
                                            "Não foi possível reconhecer o comando. Código: "
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

                            if (reactorView != null) {
                                reactorView.setOuvindo(false);
                            }

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

            speechRecognizer.startListening(intent);

        } catch (Exception e) {

            ouvindo = false;

            if (reactorView != null) {
                reactorView.setOuvindo(false);
            }

            liberarReconhecedor();

            responder(
                    "Não foi possível iniciar o reconhecimento de voz."
            );
        }
    }

    private void pararReconhecimento() {

        ouvindo = false;

        if (reactorView != null) {
            reactorView.setOuvindo(false);
        }

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
    }

    private void liberarReconhecedor() {

        if (speechRecognizer != null) {

            try {
                speechRecognizer.destroy();
            } catch (Exception ignored) {
            }

            speechRecognizer = null;
        }

        ouvindo = false;

        if (reactorView != null) {
            reactorView.setOuvindo(false);
        }
    }


    private void configurarTela() {

        telaAtual = TELA_PRINCIPAL;
        chatMode = false;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(1, 7, 16));
        configurarAreaSegura(root);

        // Cabeçalho: J.A.R.V.I.S. + engrenagem/menu.
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(10), 0, dp(10), 0);

        TextView titulo = new TextView(this);
        titulo.setText("J.A.R.V.I.S");
        titulo.setTextSize(24);
        titulo.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        titulo.setTextColor(Color.rgb(80, 205, 255));
        titulo.setGravity(Gravity.CENTER_VERTICAL);

        header.addView(titulo, new LinearLayout.LayoutParams(0, dp(58), 1));

        ImageButton menu = new ImageButton(this);
        menu.setImageResource(R.drawable.ic_jarvis_menu);
        menu.setBackgroundColor(Color.TRANSPARENT);
        menu.setColorFilter(Color.rgb(80, 205, 255));
        menu.setContentDescription("Menu");
        menu.setOnClickListener(v -> abrirMenuJarvis());
        header.addView(menu, new LinearLayout.LayoutParams(dp(52), dp(52)));

        root.addView(header, new LinearLayout.LayoutParams(-1, dp(62)));

        // Relógio grande da tela inicial.
        clockText = criarTexto("");
        clockText.setTextSize(38);
        clockText.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        clockText.setTextColor(Color.rgb(120, 215, 255));
        clockText.setGravity(Gravity.CENTER);
        root.addView(clockText, new LinearLayout.LayoutParams(-1, dp(68)));

        homeDateText = criarTexto("");
        homeDateText.setTextSize(16);
        homeDateText.setTextColor(Color.WHITE);
        homeDateText.setGravity(Gravity.CENTER);
        root.addView(homeDateText, new LinearLayout.LayoutParams(-1, dp(36)));

        // Clima: usa apenas informação configurada/obtida pelo próprio app.
        homeWeatherText = criarTexto("");
        homeWeatherText.setTextSize(14);
        homeWeatherText.setTextColor(Color.LTGRAY);
        homeWeatherText.setGravity(Gravity.CENTER);
        homeWeatherText.setPadding(dp(12), dp(4), dp(12), dp(4));
        root.addView(homeWeatherText, new LinearLayout.LayoutParams(-1, dp(46)));

        // Reator central.
        reactorView = new ReactorView(this);
        reactorView.setOnClickListener(v -> {
            if (!ouvindo) {
                iniciarReconhecimento();
            } else {
                pararReconhecimento();
                adicionarMensagem("JARVIS", "Reconhecimento interrompido.");
            }
        });

        LinearLayout.LayoutParams reactorParams =
                new LinearLayout.LayoutParams(dp(250), dp(250));
        reactorParams.gravity = Gravity.CENTER_HORIZONTAL;
        reactorParams.topMargin = dp(2);
        reactorParams.bottomMargin = dp(6);

        root.addView(reactorView, reactorParams);

        // Botão da verificação permanece na tela inicial, como na referência.
        LinearLayout verificarCard = criarPainelNeon();
        TextView escudo = criarTexto("◈");
        escudo.setTextSize(28);
        escudo.setTextColor(Color.rgb(80, 205, 255));
        escudo.setGravity(Gravity.CENTER);

        LinearLayout textos = new LinearLayout(this);
        textos.setOrientation(LinearLayout.VERTICAL);
        TextView vt = criarTexto("VERIFICAR APARELHO");
        vt.setTextSize(15);
        vt.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        vt.setTextColor(Color.WHITE);

        TextView vd = criarTexto("Mantenha seu dispositivo seguro");
        vd.setTextSize(11);
        vd.setTextColor(Color.LTGRAY);

        textos.addView(vt);
        textos.addView(vd);

        verificarCard.addView(escudo, new LinearLayout.LayoutParams(dp(48), dp(58)));
        verificarCard.addView(textos, new LinearLayout.LayoutParams(0, dp(58), 1));

        verificarCard.setOnClickListener(v -> abrirVerificacao());

        LinearLayout.LayoutParams verificarParams =
                new LinearLayout.LayoutParams(-1, dp(70));
        verificarParams.setMargins(dp(14), dp(4), dp(14), dp(8));
        root.addView(verificarCard, verificarParams);

        // A caixa "Digite uma mensagem..." continua no rodapé da tela inicial.
        LinearLayout entrada = criarEntradaMensagem(() -> {
            abrirChatIA();
            if (commandInput != null) {
                commandInput.requestFocus();
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(commandInput, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });

        root.addView(entrada, new LinearLayout.LayoutParams(-1, dp(64)));

        TextView modo = criarTexto(
                modoOnline ? "● MODO ONLINE EM USO" : "● MODO OFFLINE EM USO"
        );
        modo.setTextSize(10);
        modo.setTextColor(Color.rgb(80, 205, 255));
        modo.setGravity(Gravity.CENTER);
        root.addView(modo, new LinearLayout.LayoutParams(-1, dp(22)));

        atualizarInformacoesTelaInicial();
        setContentView(root);
        iniciarRelogio();
    }

    private void atualizarInformacoesTelaInicial() {

        if (homeDateText != null) {
            homeDateText.setText(
                    new SimpleDateFormat("dd MMM", Locale.getDefault())
                            .format(new Date())
                            .toUpperCase(Locale.getDefault())
            );
        }

        if (homeWeatherText != null) {
            String cidade = preferencias.getString("cidade_clima", "");
            if (cidade.isEmpty()) {
                homeWeatherText.setText("☁  Clima não configurado");
            } else {
                homeWeatherText.setText("☁  " + cidade + "  •  clima disponível no modo ONLINE");
            }
        }
    }

    private LinearLayout criarEntradaMensagem(Runnable aoFocar) {

        LinearLayout entrada = new LinearLayout(this);
        entrada.setOrientation(LinearLayout.HORIZONTAL);
        entrada.setGravity(Gravity.CENTER_VERTICAL);
        entrada.setPadding(dp(10), dp(4), dp(10), dp(4));

        GradientDrawable campoFundo = new GradientDrawable();
        campoFundo.setColor(Color.rgb(3, 13, 25));
        campoFundo.setCornerRadius(dp(22));
        campoFundo.setStroke(dp(1), Color.rgb(0, 150, 255));

        commandInput = new EditText(this);
        commandInput.setHint("Digite uma mensagem...");
        commandInput.setHintTextColor(Color.rgb(125, 145, 165));
        commandInput.setTextColor(Color.WHITE);
        commandInput.setSingleLine(true);
        commandInput.setTextSize(14);
        commandInput.setPadding(dp(16), 0, dp(12), 0);
        commandInput.setBackground(campoFundo);

        commandInput.setOnFocusChangeListener((v, focused) -> {
            if (focused && aoFocar != null && !chatMode) {
                aoFocar.run();
            }
        });

        entrada.addView(
                commandInput,
                new LinearLayout.LayoutParams(0, dp(52), 1)
        );

        ImageButton microfone = new ImageButton(this);
        microfone.setImageResource(R.drawable.ic_jarvis_mic);
        microfone.setBackgroundColor(Color.TRANSPARENT);
        microfone.setColorFilter(Color.rgb(80, 205, 255));
        microfone.setContentDescription("Microfone");
        microfone.setOnClickListener(v -> {
            if (!chatMode) {
                abrirChatIA();
            }
            if (!ouvindo) iniciarReconhecimento();
            else pararReconhecimento();
        });

        entrada.addView(microfone, new LinearLayout.LayoutParams(dp(52), dp(52)));

        return entrada;
    }


    private LinearLayout criarPainelNeon() {

        LinearLayout painel = new LinearLayout(this);
        painel.setOrientation(LinearLayout.HORIZONTAL);
        painel.setGravity(Gravity.CENTER_VERTICAL);
        painel.setPadding(dp(10), dp(6), dp(10), dp(6));

        GradientDrawable fundo = new GradientDrawable();
        fundo.setColor(Color.rgb(3, 13, 25));
        fundo.setCornerRadius(dp(16));
        fundo.setStroke(dp(1), Color.rgb(0, 160, 255));
        painel.setBackground(fundo);

        return painel;
    }

    private void abrirChatIA() {

        if (chatMode) return;

        chatMode = true;
        telaAtual = TELA_PRINCIPAL;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(1, 7, 16));
        configurarAreaSegura(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageButton voltar = new ImageButton(this);
        voltar.setImageResource(R.drawable.ic_jarvis_back);
        voltar.setBackgroundColor(Color.TRANSPARENT);
        voltar.setColorFilter(Color.WHITE);
        voltar.setContentDescription("Voltar");
        voltar.setOnClickListener(v -> {
            esconderTeclado();
            configurarTela();
        });
        header.addView(voltar, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout titulos = new LinearLayout(this);
        titulos.setOrientation(LinearLayout.VERTICAL);
        titulos.setGravity(Gravity.CENTER_VERTICAL);

        TextView titulo = criarTexto("J.A.R.V.I.S");
        titulo.setTextSize(21);
        titulo.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        titulo.setTextColor(Color.WHITE);

        TextView estado = criarTexto("●");
        estado.setTextSize(13);
        estado.setTextColor(Color.rgb(50, 230, 130));

        LinearLayout tituloLinha = new LinearLayout(this);
        tituloLinha.setGravity(Gravity.CENTER_VERTICAL);
        tituloLinha.addView(titulo);
        tituloLinha.addView(estado, new LinearLayout.LayoutParams(dp(30), dp(40)));

        titulos.addView(tituloLinha);
        header.addView(titulos, new LinearLayout.LayoutParams(0, dp(56), 1));

        ImageButton info = new ImageButton(this);
        info.setImageResource(R.drawable.ic_jarvis_menu);
        info.setBackgroundColor(Color.TRANSPARENT);
        info.setColorFilter(Color.rgb(80, 205, 255));
        info.setContentDescription("Menu");
        info.setOnClickListener(v -> abrirMenuJarvis());
        header.addView(info, new LinearLayout.LayoutParams(dp(52), dp(52)));

        root.addView(header, new LinearLayout.LayoutParams(-1, dp(58)));

        chatScroll = new ScrollView(this);
        chatScroll.setFillViewport(true);
        chatScroll.setClipToPadding(false);

        chatContainer = new LinearLayout(this);
        chatContainer.setOrientation(LinearLayout.VERTICAL);
        chatContainer.setPadding(dp(12), dp(10), dp(12), dp(10));

        chatScroll.addView(chatContainer);
        root.addView(
                chatScroll,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

        restaurarChatPrincipal();

        // Reator pequeno, igual ao modo conversa da referência.
        ReactorView miniReator = new ReactorView(this);
        LinearLayout.LayoutParams miniParams =
                new LinearLayout.LayoutParams(dp(120), dp(120));
        miniParams.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(miniReator, miniParams);

        LinearLayout entrada = criarEntradaMensagem(null);
        root.addView(entrada, new LinearLayout.LayoutParams(-1, dp(64)));

        ImageButton enviar = new ImageButton(this);
        enviar.setImageResource(R.drawable.ic_jarvis_send);
        enviar.setBackgroundColor(Color.TRANSPARENT);
        enviar.setColorFilter(Color.rgb(80, 205, 255));
        enviar.setContentDescription("Enviar");
        enviar.setOnClickListener(v -> enviarTextoDigitado());

        LinearLayout.LayoutParams enviarParams =
                new LinearLayout.LayoutParams(dp(52), dp(48));
        enviarParams.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(enviar, enviarParams);

        setContentView(root);

        if (commandInput != null) {
            commandInput.setText("");
        }
    }




    private void abrirMenuJarvis() {
        telaAtual = TELA_MENU_JARVIS;

        LinearLayout layout =
                criarTelaBase(
                        "J.A.R.V.I.S",
                        "CONFIGURAÇÕES"
                );

        adicionarBotaoTela(
                layout,
                "Gerenciar JARVIS",
                this::abrirGerenciarJarvis
        );

        adicionarBotaoTela(
                layout,
                "Privacidade",
                this::abrirPrivacidade
        );

        adicionarBotaoTela(
                layout,
                "Verificação do aparelho",
                this::abrirVerificacao
        );

        adicionarBotaoTela(
                layout,
                "Personalizar voz",
                this::abrirGerenciarVoz
        );

        adicionarBotaoTela(
                layout,
                "🧠 MEMÓRIA DO JARVIS",
                this::abrirMemoriaJarvis
        );

        adicionarBotaoTela(
                layout,
                "HISTÓRICO DE CONVERSAS",
                this::abrirHistoricoConversas
        );

        adicionarBotaoTela(
                layout,
                "EXPORTAR CONVERSAS",
                this::exportarConversas
        );

        adicionarBotaoTela(
                layout,
                "LIMPAR CONVERSAS DA TELA",
                this::limparConversasDaTela
        );

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::voltarTela
        );

        setContentView(layout);
    }

    private void abrirGerenciarJarvis() {

        telaAtual =
                TELA_GERENCIAR_JARVIS;

        microfoneAtivado =
                preferencias.getBoolean(
                        "microfone_ativo",
                        true
                );

        notificacoesAtivadas =
                preferencias.getBoolean(
                        "notificacoes_ativas",
                        true
                );

        statusAtivado =
                preferencias.getBoolean(
                        "status_ativo",
                        true
                );

        arquivosMidiaAtivados =
                preferencias.getBoolean(
                        "arquivos_midia_ativos",
                        false
                );

        modoOnline =
                preferencias.getBoolean(
                        "modo_online",
                        false
                );

        LinearLayout layout =
                criarTelaBase(
                        "GERENCIAR JARVIS",
                        "PERMISSÕES, RECURSOS E MODOS"
                );

        TextView aviso =
                criarTexto(
                        "Aqui ficam as configurações do JARVIS. "
                                + "O modo online só entra em uso quando você o selecionar."
                );

        aviso.setTextSize(13);
        aviso.setTextColor(Color.LTGRAY);

        layout.addView(
                aviso,
                parametrosTexto()
        );

        LinearLayout modos =
                criarCard();

        LinearLayout textosModo =
                new LinearLayout(this);

        textosModo.setOrientation(
                LinearLayout.VERTICAL
        );

        textosModo.addView(
                criarTexto("🌐  MODO DE OPERAÇÃO")
        );

        TextView descModo =
                criarTexto(
                        "OFFLINE é o modo inicial. ONLINE só é usado depois que você o ativar."
                );

        descModo.setTextSize(12);
        descModo.setTextColor(Color.GRAY);

        textosModo.addView(descModo);

        LinearLayout botoesModo =
                new LinearLayout(this);

        botoesModo.setOrientation(
                LinearLayout.HORIZONTAL
        );

        botoesModo.setGravity(
                Gravity.CENTER_VERTICAL
        );

        Button onlineButton =
                criarBotao(
                        modoOnline
                                ? "EM USO"
                                : "ATIVAR"
                );

        Button offlineButton =
                criarBotao(
                        modoOnline
                                ? "ATIVAR"
                                : "EM USO"
                );

        onlineButton.setTextSize(10);
        offlineButton.setTextSize(10);

        onlineButton.setOnClickListener(
                v -> {

                    modoOnline = true;

                    preferencias.edit()
                            .putBoolean(
                                    "modo_online",
                                    true
                            )
                            .apply();

                    onlineButton.setText("EM USO");
                    offlineButton.setText("ATIVAR");

                    responder(
                            "Modo online está em uso."
                    );
                }
        );

        offlineButton.setOnClickListener(
                v -> {

                    modoOnline = false;

                    preferencias.edit()
                            .putBoolean(
                                    "modo_online",
                                    false
                            )
                            .apply();

                    onlineButton.setText("ATIVAR");
                    offlineButton.setText("EM USO");

                    responder(
                            "Modo offline está em uso."
                    );
                }
        );

        botoesModo.addView(
                onlineButton,
                new LinearLayout.LayoutParams(
                        0,
                        dp(44),
                        1
                )
        );

        botoesModo.addView(
                offlineButton,
                new LinearLayout.LayoutParams(
                        0,
                        dp(44),
                        1
                )
        );

        textosModo.addView(
                botoesModo
        );

        modos.addView(
                textosModo,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        layout.addView(
                modos,
                parametrosCard()
        );

        adicionarControleInterno(
                layout,
                "MICROFONE",
                "Permite que o JARVIS use o microfone para reconhecimento de voz.",
                () -> microfoneAtivado,
                valor -> {

                    microfoneAtivado = valor;

                    preferencias.edit()
                            .putBoolean(
                                    "microfone_ativo",
                                    valor
                            )
                            .apply();

                    if (!valor) {
                        pararReconhecimento();
                    } else if (!microfonePermitido()
                            && Build.VERSION.SDK_INT >= 23) {

                        requestPermissions(
                                new String[]{
                                        Manifest.permission.RECORD_AUDIO
                                },
                                REQUEST_AUDIO
                        );
                    }
                }
        );

        adicionarControleInterno(
                layout,
                "🔔  NOTIFICAÇÕES",
                "Controla as notificações usadas pelo JARVIS.",
                () -> notificacoesAtivadas,
                valor -> {

                    notificacoesAtivadas = valor;

                    preferencias.edit()
                            .putBoolean(
                                    "notificacoes_ativas",
                                    valor
                            )
                            .apply();
                }
        );

        adicionarControleInterno(
                layout,
                "📱  STATUS DO APARELHO",
                "Permite consultar bateria, RAM, armazenamento e outros dados acessíveis.",
                () -> statusAtivado,
                valor -> {

                    statusAtivado = valor;

                    preferencias.edit()
                            .putBoolean(
                                    "status_ativo",
                                    valor
                            )
                            .apply();
                }
        );

        adicionarControleInterno(
                layout,
                "📁  ARQUIVOS E MÍDIA",
                "Controle interno do recurso. Nenhuma permissão de armazenamento é concedida automaticamente.",
                () -> arquivosMidiaAtivados,
                valor -> {

                    arquivosMidiaAtivados = valor;

                    preferencias.edit()
                            .putBoolean(
                                    "arquivos_midia_ativos",
                                    valor
                            )
                            .apply();
                }
        );

        adicionarControleIndisponivel(
                layout,
                "♿  ACESSIBILIDADE",
                "Indisponível: o JARVIS não utiliza Accessibility Service."
        );

        adicionarBotaoTela(
                layout,
                "🗣  GERENCIAR VOZ DO JARVIS",
                this::abrirGerenciarVoz
        );

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::abrirMenuJarvis
        );

        setContentView(layout);
    }

    private interface EstadoControle {
        boolean get();
    }

    private interface AlterarControle {
        void set(boolean valor);
    }

    private void adicionarControleInterno(
            LinearLayout layout,
            String titulo,
            String descricao,
            EstadoControle estado,
            AlterarControle alterar) {

        LinearLayout card =
                criarCard();

        LinearLayout textos =
                new LinearLayout(this);

        textos.setOrientation(
                LinearLayout.VERTICAL
        );

        TextView tituloView =
                criarTexto(titulo);

        tituloView.setTextSize(15);

        textos.addView(
                tituloView
        );

        TextView descView =
                criarTexto(descricao);

        descView.setTextSize(12);
        descView.setTextColor(Color.GRAY);

        textos.addView(
                descView
        );

        Button controle =
                criarBotao(
                        estado.get()
                                ? "ATIVADO"
                                : "DESATIVADO"
                );

        controle.setTextSize(10);

        controle.setOnClickListener(
                v -> {

                    boolean novo =
                            !estado.get();

                    alterar.set(novo);

                    controle.setText(
                            novo
                                    ? "ATIVADO"
                                    : "DESATIVADO"
                    );
                }
        );

        card.addView(
                textos,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        card.addView(
                controle,
                new LinearLayout.LayoutParams(
                        dp(105),
                        dp(44)
                )
        );

        layout.addView(
                card,
                parametrosCard()
        );
    }

    private void adicionarControleIndisponivel(
            LinearLayout layout,
            String titulo,
            String descricao) {

        LinearLayout card =
                criarCard();

        LinearLayout textos =
                new LinearLayout(this);

        textos.setOrientation(
                LinearLayout.VERTICAL
        );

        textos.addView(
                criarTexto(titulo)
        );

        TextView desc =
                criarTexto(descricao);

        desc.setTextSize(12);
        desc.setTextColor(Color.GRAY);

        textos.addView(desc);

        TextView estado =
                criarTexto("INDISPONÍVEL");

        estado.setTextSize(10);
        estado.setGravity(Gravity.CENTER);
        estado.setTextColor(Color.GRAY);

        card.addView(
                textos,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        card.addView(
                estado,
                new LinearLayout.LayoutParams(
                        dp(105),
                        dp(44)
                )
        );

        layout.addView(
                card,
                parametrosCard()
        );
    }

    private void abrirGerenciarVoz() {

        telaAtual =
                TELA_GERENCIAR_VOZ;

        LinearLayout layout =
                criarTelaBase(
                        "VOZ DO JARVIS",
                        "ESCOLHA UMA VOZ TTS DISPONÍVEL"
                );

        TextView info =
                criarTexto(
                        "Escolha uma voz instalada no aparelho. "
                                + "A velocidade permanece normal e o tom será ajustado para um perfil mais grave."
                );

        info.setTextSize(13);
        info.setTextColor(Color.LTGRAY);

        layout.addView(
                info,
                parametrosTexto()
        );

        LinearLayout lista =
                new LinearLayout(this);

        lista.setOrientation(
                LinearLayout.VERTICAL
        );

        if (tts == null || !ttsReady) {

            TextView indisponivel =
                    criarTexto(
                            "O mecanismo de voz ainda está inicializando."
                    );

            indisponivel.setTextColor(
                    Color.GRAY
            );

            lista.addView(
                    indisponivel,
                    parametrosTexto()
            );

        } else {

            List<Voice> vozes =
                    new ArrayList<>();

            for (Voice voz : tts.getVoices()) {

                if (voz == null
                        || voz.getLocale() == null) {
                    continue;
                }

                Locale local =
                        voz.getLocale();

                if ("pt".equalsIgnoreCase(
                        local.getLanguage()
                )
                        && "BR".equalsIgnoreCase(
                        local.getCountry()
                )) {

                    vozes.add(voz);
                }
            }

            if (vozes.isEmpty()) {

                TextView nenhuma =
                        criarTexto(
                                "Nenhuma voz pt-BR foi encontrada pelo mecanismo TTS."
                        );

                nenhuma.setTextColor(
                        Color.GRAY
                );

                lista.addView(
                        nenhuma,
                        parametrosTexto()
                );

            } else {

                for (Voice voz : vozes) {

                    String nome =
                            voz.getName();

                    String rotulo =
                            rotuloVoz(nome);

                    Button escolha =
                            criarBotao(
                                    rotulo
                                            + "\n"
                                            + nome
                            );

                    escolha.setTextSize(11);
                    escolha.setGravity(
                            Gravity.CENTER
                    );

                    escolha.setOnClickListener(
                            v -> {

                                if (tts != null) {

                                    tts.setVoice(voz);
                                    tts.setSpeechRate(1.0f);
                                    tts.setPitch(0.85f);

                                    preferencias.edit()
                                            .putString(
                                                    "voz_tts",
                                                    voz.getName()
                                            )
                                            .apply();

                                    falar(
                                            "Voz selecionada."
                                    );
                                }
                            }
                    );

                    lista.addView(
                            escolha,
                            parametrosBotao()
                    );
                }
            }
        }

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(lista);

        layout.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        adicionarBotaoTela(
                layout,
                "TESTAR VOZ ATUAL",
                () -> falar(
                        "À sua disposição. Sistemas locais operacionais."
                )
        );

        adicionarBotaoTela(
                layout,
                "CONFIGURAR COMANDO DE ATIVAÇÃO",
                this::abrirComandoVoz
        );

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::abrirGerenciarJarvis
        );

        setContentView(layout);
    }

    private String rotuloVoz(String nome) {
        String n =
                nome == null
                        ? ""
                        : nome.toLowerCase(
                                Locale.getDefault()
                        );

        if (n.contains("male")
                || n.contains("mascul")) {
            return "VOZ MASCULINA";
        }

        if (n.contains("female")
                || n.contains("feminin")) {
            return "VOZ FEMININA";
        }

        return "VOZ DISPONÍVEL";
    }

    private void aplicarVozSalva() {

        if (tts == null || !ttsReady) {
            return;
        }

        try {

            String nome =
                    preferencias.getString(
                            "voz_tts",
                                                       ""                      );

            if (!nome.isEmpty()) {

                for (Voice voz : tts.getVoices()) {

                    if (nome.equals(
                            voz.getName()
                    )) {

                        tts.setVoice(voz);
                        break;
                    }
                }
            }

            tts.setSpeechRate(1.0f);
            tts.setPitch(0.85f);

        } catch (Exception ignored) {
        }
    }     private boolean microfonePermitido() {
        return Build.VERSION.SDK_INT < 23
                || checkSelfPermission(
                        Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED;
    }

    private void adicionarControlePermissao(
            LinearLayout layout,
            String titulo,
            String descricao,
            boolean microfone) {

        LinearLayout card =
                criarCard();

        LinearLayout textos =
                new LinearLayout(this);

        textos.setOrientation(
                LinearLayout.VERTICAL
        );

        TextView tituloView =
                criarTexto(titulo);

        tituloView.setTextSize(15);

        textos.addView(
                tituloView
        );

        TextView descView =
                criarTexto(descricao);

        descView.setTextSize(12);
        descView.setTextColor(Color.GRAY);

        textos.addView(
                descView
        );

        Button controle =
                criarBotao(
                        microfone && microfonePermitido()
                                ? "DESATIVAR"
                                : "ATIVAR"
                );

        controle.setTextSize(11);

        controle.setOnClickListener(
                v -> {

                    if (!microfone) {
                        return;
                    }

                    if (microfonePermitido()) {

                        abrirConfiguracoesAndroid();

                    } else if (Build.VERSION.SDK_INT >= 23) {

                        requestPermissions(
                                new String[]{
                                        Manifest.permission.RECORD_AUDIO
                                },
                                REQUEST_AUDIO
                        );
                    }

                    atualizarStatusPermissao(
                            controle
                    );
                }
        );

        card.addView(
                textos,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        card.addView(
                controle,
                new LinearLayout.LayoutParams(
                        dp(100),
                        dp(44)
                )
        );

        layout.addView(
                card,
                parametrosCard()
        );
    }

    private void atualizarStatusPermissao(
            Button botao) {

        botao.setText(
                microfonePermitido()
                        ? "DESATIVAR"
                        : "ATIVAR"
        );
    }

    private void adicionarControleInformacao(
            LinearLayout layout,
            String titulo,
            String descricao) {

        LinearLayout card =
                criarCard();

        LinearLayout textos =
                new LinearLayout(this);

        textos.setOrientation(
                LinearLayout.VERTICAL
        );

        TextView tituloView =
                criarTexto(titulo);

        tituloView.setTextSize(15);

        textos.addView(
                tituloView
        );

        TextView descView =
                criarTexto(descricao);

        descView.setTextSize(12);
        descView.setTextColor(Color.GRAY);

        textos.addView(
                descView
        );

        TextView estado =
                criarTexto(
                        titulo.contains("INTERNET")
                                ? "BLOQUEADO"
                                : "DISPONÍVEL"
                );

        estado.setTextSize(10);
        estado.setGravity(
                Gravity.CENTER
        );
        estado.setTextColor(
                Color.LTGRAY
        );

        card.addView(
                textos,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        card.addView(
                estado,
                new LinearLayout.LayoutParams(
                        dp(100),
                        dp(44)
                )
        );

        layout.addView(
                card,
                parametrosCard()
        );
    }

    private LinearLayout criarCard() {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.HORIZONTAL
        );

        card.setGravity(
                Gravity.CENTER_VERTICAL
        );

        card.setPadding(
                dp(12),
                dp(10),
                dp(8),
                dp(10)
        );

        GradientDrawable fundo =
                new GradientDrawable();

        fundo.setColor(
                Color.rgb(15, 15, 15)
        );

        fundo.setCornerRadius(
                dp(14)
        );

        fundo.setStroke(
                dp(1),
                Color.rgb(48, 48, 48)
        );

        card.setBackground(fundo);

        return card;
    }

    private LinearLayout.LayoutParams parametrosCard() {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        params.topMargin = dp(7);

        return params;
    }



    private void abrirPrivacidade() {

        telaAtual =
                TELA_PRIVACIDADE;

        LinearLayout layout =
                criarTelaBase(
                        "PRIVACIDADE",
                        "ACESSO DO JARVIS"
                );

        TextView info =
                criarTexto(
                        "O JARVIS Lite utiliza somente recursos permitidos pelo Android.\n\n"
                                + "Nesta versão, o principal recurso sensível é o microfone.\n\n"
                                + "O Android continua responsável pelas permissões do aplicativo.\n\n"
                                + "Nenhum aplicativo comum pode garantir proteção absoluta contra todas as ameaças."
                );

        layout.addView(
                info,
                parametrosTexto()
        );

        adicionarBotaoTela(
                layout,
                "GERENCIAR PERMISSÕES NO ANDROID",
                this::abrirConfiguracoesAndroid
        );

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::abrirMenuJarvis
        );

        setContentView(layout);
    }


    private void abrirVerificacao() {

        telaAtual = TELA_VERIFICACAO;

        LinearLayout layout = criarTelaBase(
                "VERIFICAÇÃO DO APARELHO",
                "ANÁLISE BÁSICA DO JARVIS"
        );

        TextView titulo = criarTexto("Verificando seu aparelho...");
        titulo.setTextSize(21);
        titulo.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        titulo.setTextColor(Color.WHITE);
        titulo.setGravity(Gravity.CENTER);
        layout.addView(titulo, parametrosTexto());

        TextView subtitulo = criarTexto(
                "Analisando aplicativos e permissões\nacessíveis ao JARVIS."
        );
        subtitulo.setTextSize(13);
        subtitulo.setTextColor(Color.LTGRAY);
        subtitulo.setGravity(Gravity.CENTER);
        layout.addView(subtitulo, parametrosTexto());

        ReactorView scanner = new ReactorView(this);
        LinearLayout.LayoutParams scannerParams =
                new LinearLayout.LayoutParams(dp(210), dp(210));
        scannerParams.gravity = Gravity.CENTER_HORIZONTAL;
        layout.addView(scanner, scannerParams);

        ProgressBar progresso = new ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
        );
        progresso.setMax(100);
        progresso.setProgress(0);
        progresso.setIndeterminate(false);

        GradientDrawable barraFundo = new GradientDrawable();
        barraFundo.setColor(Color.rgb(4, 18, 30));
        barraFundo.setCornerRadius(dp(10));
        barraFundo.setStroke(dp(1), Color.rgb(0, 150, 255));
        progresso.setBackground(barraFundo);

        LinearLayout.LayoutParams barraParams =
                new LinearLayout.LayoutParams(-1, dp(12));
        barraParams.setMargins(dp(18), dp(8), dp(18), dp(4));
        layout.addView(progresso, barraParams);

        TextView porcentagem = criarTexto("0%");
        porcentagem.setTextSize(13);
        porcentagem.setTextColor(Color.rgb(80, 205, 255));
        porcentagem.setGravity(Gravity.CENTER);
        layout.addView(porcentagem, new LinearLayout.LayoutParams(-1, dp(30)));

        TextView status1 = criarTexto("○  Analisando aplicativos...");
        TextView status2 = criarTexto("○  Verificando informações acessíveis...");
        TextView status3 = criarTexto("○  Checando permissões...");

        status1.setTextSize(13);
        status2.setTextSize(13);
        status3.setTextSize(13);

        status1.setTextColor(Color.rgb(80, 205, 255));
        status2.setTextColor(Color.GRAY);
        status3.setTextColor(Color.GRAY);

        layout.addView(status1, parametrosTexto());
        layout.addView(status2, parametrosTexto());
        layout.addView(status3, parametrosTexto());

        TextView observacao = criarTexto(
                "Isso pode levar alguns minutos."
        );
        observacao.setTextSize(11);
        observacao.setTextColor(Color.GRAY);
        observacao.setGravity(Gravity.CENTER);
        layout.addView(observacao, parametrosTexto());

        Button iniciar = criarBotao("INICIAR VERIFICAÇÃO");
        iniciar.setOnClickListener(v -> {

            iniciar.setEnabled(false);
            iniciar.setText("VERIFICANDO...");
            verificacaoEncontrouSuspeito = false;
            nomeAppSuspeito = "";
            detalheAppSuspeito = "";

            scanner.setOuvindo(true);

            final Handler h = new Handler();
            final int[] p = {0};

            Runnable andamento = new Runnable() {
                @Override
                public void run() {

                    p[0] = Math.min(100, p[0] + 4);
                    progresso.setProgress(p[0]);
                    porcentagem.setText(p[0] + "%");

                    if (p[0] >= 20) {
                        status1.setText("✓  Aplicativos analisados...");
                        status1.setTextColor(Color.rgb(70, 230, 150));
                        status2.setTextColor(Color.rgb(80, 205, 255));
                        status2.setText("○  Verificando informações acessíveis...");
                    }

                    if (p[0] >= 60) {
                        status2.setText("✓  Informações acessíveis verificadas...");
                        status2.setTextColor(Color.rgb(70, 230, 150));
                        status3.setTextColor(Color.rgb(80, 205, 255));
                        status3.setText("○  Checando permissões...");
                    }

                    if (p[0] >= 96) {
                        status3.setText("✓  Permissões básicas verificadas...");
                        status3.setTextColor(Color.rgb(70, 230, 150));
                    }

                    if (p[0] < 100) {
                        h.postDelayed(this, 80);
                        return;
                    }

                    h.removeCallbacks(this);

                    new Thread(() -> {
                        executarVerificacao();

                        runOnUiThread(() -> {
                            scanner.setOuvindo(false);
                            iniciar.setEnabled(true);
                            iniciar.setText("INICIAR VERIFICAÇÃO");
                            abrirResultadoVerificacao();
                        });
                    }).start();
                }
            };

            h.post(andamento);
        });

        layout.addView(iniciar, parametrosBotao());

        Button voltarBotao = criarBotao("VOLTAR");
        voltarBotao.setOnClickListener(v -> abrirMenuJarvis());
        layout.addView(voltarBotao, parametrosBotao());

        setContentView(layout);
    }

    private void abrirResultadoVerificacao() {

        LinearLayout layout = criarTelaBase(
                verificacaoEncontrouSuspeito
                        ? "RESULTADO DA VERIFICAÇÃO (COM ALERTA)"
                        : "RESULTADO DA VERIFICAÇÃO (SEM AMEAÇAS)",
                "ANÁLISE BÁSICA DO JARVIS"
        );

        LinearLayout icone = criarPainelResultado(
                verificacaoEncontrouSuspeito
                        ? "!"
                        : "✓",
                verificacaoEncontrouSuspeito
                        ? Color.rgb(255, 70, 70)
                        : Color.rgb(55, 235, 165)
        );

        LinearLayout.LayoutParams iconeParams =
                new LinearLayout.LayoutParams(dp(164), dp(164));
        iconeParams.gravity = Gravity.CENTER_HORIZONTAL;
        iconeParams.topMargin = dp(8);
        iconeParams.bottomMargin = dp(8);
        layout.addView(icone, iconeParams);

        TextView titulo = criarTexto(
                verificacaoEncontrouSuspeito
                        ? "Foi encontrado um app\nsupostamente malicioso,\nverifique se foi você que instalou."
                        : "Seu telefone parece estar seguro,\ntenha um bom dia."
        );
        titulo.setTextSize(18);
        titulo.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        titulo.setGravity(Gravity.CENTER);
        titulo.setTextColor(Color.WHITE);
        layout.addView(titulo, parametrosTexto());

        if (verificacaoEncontrouSuspeito) {

            LinearLayout app = criarPainelNeon();

            TextView simbolo = criarTexto("⚠");
            simbolo.setTextSize(24);
            simbolo.setTextColor(Color.rgb(255, 90, 90));
            simbolo.setGravity(Gravity.CENTER);

            LinearLayout dados = new LinearLayout(this);
            dados.setOrientation(LinearLayout.VERTICAL);

            TextView nome = criarTexto(
                    nomeAppSuspeito.isEmpty()
                            ? "App suspeito"
                            : nomeAppSuspeito
            );
            nome.setTextSize(14);
            nome.setTextColor(Color.WHITE);

            TextView detalhe = criarTexto(
                    detalheAppSuspeito.isEmpty()
                            ? "Fonte: desconhecida"
                            : detalheAppSuspeito
            );
            detalhe.setTextSize(11);
            detalhe.setTextColor(Color.LTGRAY);

            dados.addView(nome);
            dados.addView(detalhe);

            app.addView(simbolo, new LinearLayout.LayoutParams(dp(52), dp(58)));
            app.addView(dados, new LinearLayout.LayoutParams(0, dp(58), 1));

            LinearLayout.LayoutParams appParams =
                    new LinearLayout.LayoutParams(-1, dp(76));
            appParams.setMargins(dp(10), dp(8), dp(10), dp(8));
            layout.addView(app, appParams);

        } else {

            TextView detalhe = criarTexto(
                    "Nenhuma ameaça foi encontrada\nna verificação básica."
            );
            detalhe.setTextSize(14);
            detalhe.setTextColor(Color.rgb(80, 205, 255));
            detalhe.setGravity(Gravity.CENTER);
            layout.addView(detalhe, parametrosTexto());
        }

        Button ok = criarBotao("OK");
        ok.setOnClickListener(v -> abrirMenuJarvis());
        layout.addView(ok, parametrosBotao());

        setContentView(layout);
    }

    private LinearLayout criarPainelResultado(String simbolo, int cor) {

        LinearLayout painel = new LinearLayout(this);
        painel.setGravity(Gravity.CENTER);

        GradientDrawable fundo = new GradientDrawable();
        fundo.setShape(GradientDrawable.OVAL);
        fundo.setColor(Color.rgb(3, 13, 25));
        fundo.setStroke(dp(3), cor);
        painel.setBackground(fundo);

        TextView texto = criarTexto(simbolo);
        texto.setTextSize(76);
        texto.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        texto.setTextColor(cor);
        texto.setGravity(Gravity.CENTER);

        painel.addView(texto, new LinearLayout.LayoutParams(dp(150), dp(150)));

        return painel;
    }

    private String resultadoVerificacao() {
        if (ultimaVerificacao == 0L) {
            return "VERIFICAÇÃO AINDA NÃO EXECUTADA";
        }

        return verificacaoEncontrouSuspeito
                ? "Foi encontrado um aplicativo que merece revisão."
                : "Nenhuma ameaça foi encontrada na verificação básica.";
    }

    private String listaAppsVerificados() {

        if (ultimaVerificacao == 0L) {
            return "Nenhuma verificação foi executada ainda.";
        }

        StringBuilder out = new StringBuilder();

        try {
            List<ApplicationInfo> apps =
                    getPackageManager().getInstalledApplications(
                            PackageManager.GET_META_DATA
                    );

            int limite = Math.min(apps.size(), 12);

            for (int i = 0; i < limite; i++) {
                ApplicationInfo a = apps.get(i);
                CharSequence label =
                        getPackageManager().getApplicationLabel(a);

                out.append("• ")
                        .append(label)
                        .append("\n");
            }

        } catch (Exception e) {
            out.append("Não foi possível listar os aplicativos.");
        }

        return out.toString();
    }

    private void executarVerificacao() {

        verificacaoEncontrouSuspeito = false;
        nomeAppSuspeito = "";
        detalheAppSuspeito = "";

        try {

            PackageManager pm = getPackageManager();

            List<ApplicationInfo> apps =
                    pm.getInstalledApplications(
                            PackageManager.GET_META_DATA
                    );

            for (ApplicationInfo appInfo : apps) {

                if (appInfo == null
                        || appInfo.packageName == null
                        || getPackageName().equals(appInfo.packageName)) {
                    continue;
                }

                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    continue;
                }

                String installer = "";

                try {
                    if (Build.VERSION.SDK_INT >= 30) {
                        android.content.pm.InstallSourceInfo info =
                                pm.getInstallSourceInfo(appInfo.packageName);
                        if (info != null && info.getInstallingPackageName() != null) {
                            installer = info.getInstallingPackageName();
                        }
                    } else {
                        String old = pm.getInstallerPackageName(appInfo.packageName);
                        installer = old == null ? "" : old;
                    }
                } catch (Exception ignored) {
                }

                String[] requested = new String[0];

                try {
                    PackageInfo pi = pm.getPackageInfo(
                            appInfo.packageName,
                            PackageManager.GET_PERMISSIONS
                    );
                    if (pi.requestedPermissions != null) {
                        requested = pi.requestedPermissions;
                    }
                } catch (Exception ignored) {
                }

                int sensiveis = 0;
                for (String perm : requested) {
                    if (Manifest.permission.RECORD_AUDIO.equals(perm)
                            || Manifest.permission.ACCESS_FINE_LOCATION.equals(perm)
                            || Manifest.permission.ACCESS_COARSE_LOCATION.equals(perm)
                            || Manifest.permission.READ_CONTACTS.equals(perm)
                            || Manifest.permission.CAMERA.equals(perm)
                            || Manifest.permission.READ_SMS.equals(perm)
                            || Manifest.permission.CALL_PHONE.equals(perm)) {
                        sensiveis++;
                    }
                }

                // Heurística deliberadamente conservadora:
                // origem desconhecida + várias permissões sensíveis.
                // Isso NÃO prova que o app é malware.
                if (installer.isEmpty() && sensiveis >= 4) {

                    verificacaoEncontrouSuspeito = true;

                    CharSequence label =
                            pm.getApplicationLabel(appInfo);

                    nomeAppSuspeito =
                            label == null
                                    ? appInfo.packageName
                                    : label.toString();

                    detalheAppSuspeito =
                            "Fonte: desconhecida • permissões sensíveis detectadas";

                    break;
                }
            }

        } catch (Exception ignored) {
        }

        ultimaVerificacao = System.currentTimeMillis();

        preferencias.edit()
                .putLong("ultima_verificacao", ultimaVerificacao)
                .apply();
    }

    private String textoUltimaVerificacao() {

        if (ultimaVerificacao == 0L) {
            return "Verificação realizada: nunca.";
        }

        String horario =
                new SimpleDateFormat(
                        "dd/MM/yyyy HH:mm:ss",
                        Locale.getDefault()
                ).format(new Date(ultimaVerificacao));

        return "Verificação realizada em:\n" + horario;
    }



    private void abrirComandoVoz() {

        telaAtual =
                TELA_COMANDO_VOZ;

        LinearLayout layout =
                criarTelaBase(
                        "PERSONALIZAR VOZ",
                        "ATIVAÇÃO DO JARVIS"
                );

        TextView explicacao =
                criarTexto(
                        "Defina a frase que deverá ser usada como comando de ativação.\n\n"
                                + "Exemplo:\n"
                                + "JARVIS está aí?"
                );

        layout.addView(
                explicacao,
                parametrosTexto()
        );

        EditText campo =
                new EditText(this);

        campo.setHint(
                "Digite o novo comando..."
        );

        campo.setText(
                preferencias.getString(
                        "comando_voz",
                        "JARVIS está aí?"
                )
        );

        campo.setTextColor(Color.WHITE);
        campo.setHintTextColor(Color.GRAY);

        layout.addView(
                campo,
                parametrosTexto()
        );

        Button confirmar =
                criarBotao("CONFIRMAR");

        confirmar.setEnabled(
                campo.getText()
                        .toString()
                        .trim()
                        .length() >= 4
        );

        campo.addTextChangedListener(
                new android.text.TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        confirmar.setEnabled(
                                s.toString()
                                        .trim()
                                        .length() >= 4
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            android.text.Editable s) {
                    }
                }
        );

        confirmar.setOnClickListener(
                v -> {

                    String novoComando =
                            campo.getText()
                                    .toString()
                                    .trim();

                    if (novoComando.length() < 4) {
                        return;
                    }

                    preferencias.edit()
                            .putString(
                                    "comando_voz",
                                    novoComando
                            )
                            .apply();

                    adicionarMensagem(
                            "JARVIS",
                            "Novo comando de voz ativo."
                    );

                    falar(
                            "Novo comando de voz ativo."
                    );
                }
        );

        layout.addView(
                confirmar,
                parametrosBotao()
        );

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::abrirMenuJarvis
        );

        setContentView(layout);
    }

    private LinearLayout criarTelaBase(
            String titulo,
            String subtitulo) {

        TelaRolavelLayout layout =
                new TelaRolavelLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setBackgroundColor(
                Color.BLACK
        );

        layout.setPadding(
                dp(18),
                dp(10),
                dp(18),
                dp(24)
        );

        if (Build.VERSION.SDK_INT >= 30) {

            layout.setOnApplyWindowInsetsListener(
                    (v, insets) -> {

                        Insets barras =
                                insets.getInsets(
                                        WindowInsets.Type.statusBars()
                                                | WindowInsets.Type.navigationBars()
                                );

                        v.setPadding(
                                dp(18),
                                dp(10) + barras.top,
                                dp(18),
                                dp(24) + barras.bottom
                        );

                        return insets;
                    }
            );

            layout.requestApplyInsets();
        }

        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.HORIZONTAL
        );

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        ImageButton voltar =
        new ImageButton(this);

voltar.setImageResource(
        R.drawable.ic_jarvis_back
);

voltar.setBackgroundColor(
        Color.TRANSPARENT
);

voltar.setColorFilter(
        Color.WHITE
);

voltar.setContentDescription(
        "Voltar"
);

voltar.setOnClickListener(
        v -> voltarSistema()
);

        header.addView(
                voltar,
                new LinearLayout.LayoutParams(
                        dp(58),
                        dp(48)
                )
        );

        LinearLayout titulos =
                new LinearLayout(this);

        titulos.setOrientation(
                LinearLayout.VERTICAL
        );

        titulos.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView tituloView =
                criarTexto(titulo);

        tituloView.setTextSize(20);
        tituloView.setGravity(Gravity.CENTER_VERTICAL);
        tituloView.setMaxLines(2);
        tituloView.setEllipsize(null);
        tituloView.setIncludeFontPadding(true);

        TextView subtituloView =
                criarTexto(subtitulo);

        subtituloView.setTextSize(10);
        subtituloView.setTextColor(Color.GRAY);
        subtituloView.setGravity(Gravity.CENTER_VERTICAL);
        subtituloView.setMaxLines(2);
        subtituloView.setEllipsize(null);
        subtituloView.setIncludeFontPadding(true);

        titulos.addView(
                tituloView,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(34)
                )
        );

        titulos.addView(
                subtituloView,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(28)
                )
        );

        header.addView(
                titulos,
                new LinearLayout.LayoutParams(
                        0,
                        dp(64),
                        1
                )
        );

        layout.addView(
                header,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(72)
                )
        );

        ScrollView scroll =
                new ScrollView(this);

        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setNestedScrollingEnabled(true);
                LinearLayout conteudo =
                new LinearLayout(this);

        conteudo.setOrientation(
                LinearLayout.VERTICAL
        );

        conteudo.setPadding(
                0,
                dp(4),
                0,
                dp(24)
        );

        scroll.addView(
                conteudo,
                new ScrollView.LayoutParams(
                        -1,
                        -2
                )
        );

        layout.setConteudoRolavel(
                conteudo
        );

        layout.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        layout.ativarRoteamento();

        return layout;
    }

    private static class TelaRolavelLayout
            extends LinearLayout {

        private LinearLayout conteudoRolavel;
        private boolean roteando = false;

        TelaRolavelLayout(
                android.content.Context context) {
            super(context);
        }

        void setConteudoRolavel(
                LinearLayout conteudo) {

            this.conteudoRolavel =
                    conteudo;
        }

        void ativarRoteamento() {
            this.roteando = true;
        }

        @Override
        public void addView(
                View child,
                android.view.ViewGroup.LayoutParams params) {

            if (roteando
                    && conteudoRolavel != null
                    && child != conteudoRolavel) {

                conteudoRolavel.addView(
                        child,
                        params
                );

            } else {

                super.addView(
                        child,
                        params
                );
            }
        }

        @Override
        public void addView(View child) {

            if (roteando
                    && conteudoRolavel != null
                    && child != conteudoRolavel) {

                conteudoRolavel.addView(
                        child
                );

            } else {

                super.addView(child);
            }
        }
    }

    private TextView criarTexto(
            String texto) {

        TextView view =
                new TextView(this);

        view.setText(texto);
        view.setTextColor(Color.WHITE);
        view.setTextSize(16);

        view.setPadding(
                dp(8),
                dp(10),
                dp(8),
                dp(10)
        );

        return view;
    }

    private Button criarBotao(
        String texto) {

    Button button =
            new Button(this);

    button.setText(texto);
    button.setTextColor(Color.WHITE);
    button.setTextSize(15);
    button.setAllCaps(false);

    button.setPadding(
            dp(18),
            dp(12),
            dp(18),
            dp(12)
    );

    android.graphics.drawable.GradientDrawable fundo =
            new android.graphics.drawable.GradientDrawable();

    fundo.setColor(
            Color.rgb(28, 30, 35)
    );

    fundo.setCornerRadius(
            dp(18)
    );

    fundo.setStroke(
            dp(1),
            Color.rgb(60, 62, 68)
    );

    button.setBackground(fundo);

    return button;
                      }

    private void adicionarBotaoTela(
            LinearLayout layout,
            String texto,
            Runnable acao) {

        Button button =
                criarBotao(texto);

        button.setOnClickListener(
                v -> acao.run()
        );

        layout.addView(
                button,
                parametrosBotao()
        );
    }

    private LinearLayout.LayoutParams parametrosTexto() {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        params.topMargin = dp(6);

        return params;
    }

    private LinearLayout.LayoutParams parametrosBotao() {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(46)
                );

        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.leftMargin = dp(18);
        params.rightMargin = dp(18);
        params.topMargin = dp(6);
        params.bottomMargin = dp(2);

        return params;
    }     private void voltarTela() {
        abrirTelaPrincipal();
    }

    private void abrirTelaPrincipal() {

        modoOnline =
                preferencias.getBoolean(
                        "modo_online",
                        false
                );

        configurarTela();

        restaurarChatPrincipal();

        iniciarRelogio();
    }

    private void voltarSistema() {

        switch (telaAtual) {

            case TELA_MENU_JARVIS:
                abrirTelaPrincipal();
                break;

            case TELA_GERENCIAR_JARVIS:
            case TELA_PRIVACIDADE:
            case TELA_VERIFICACAO:
            case TELA_COMANDO_VOZ:
            case TELA_MEMORIA:
            case TELA_HISTORICO:
                abrirMenuJarvis();
                break;

            case TELA_GERENCIAR_VOZ:
                abrirGerenciarJarvis();
                break;

            case TELA_PRINCIPAL:
            default:
                super.onBackPressed();
                break;
        }
    }

    private void abrirConfiguracoesAndroid() {

        try {

            Intent intent =
                    new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    );

        intent.setData(
                Uri.parse(
                        "package:"
                                + getPackageName()
                )
        );

        startActivity(intent);

    } catch (Exception ignored) {
    }
}


    private void iniciarRelogio() {

        if (clockHandler != null) {
            clockHandler.removeCallbacksAndMessages(null);
        }

        clockHandler = new Handler();

        Runnable atualizar = new Runnable() {

            @Override
            public void run() {

                Date agora = new Date();

                if (clockText != null) {
                    clockText.setText(
                            new SimpleDateFormat(
                                    "HH:mm",
                                    Locale.getDefault()
                            ).format(agora)
                    );
                }

                if (homeDateText != null) {
                    homeDateText.setText(
                            new SimpleDateFormat(
                                    "dd MMM",
                                    Locale.getDefault()
                            ).format(agora)
                                    .toUpperCase(Locale.getDefault())
                    );
                }

                if (clockHandler != null) {
                    clockHandler.postDelayed(this, 1000);
                }
            }
        };

        clockHandler.post(atualizar);
    }





private void processarComando(
        String comandoOriginal) {

    if (comandoOriginal == null) {
        return;
    }

    String original =
            comandoOriginal.trim();

    if (original.isEmpty()) {
        return;
    }

    /*
     * A ordem é importante:
     *
     * 1. Memória explícita/social.
     * 2. Conversa.
     * 3. Comandos objetivos.
     *
     * Isso impede que uma frase como
     * "pode me chamar de Santiago"
     * seja confundida com outro comando.
     */

    if (processarMemoriaSocial(original)) {
        return;
    }

    String comando =
            normalizar(original);

    if (processarComandoMemoria(original)) {
        return;
    }

    String respostaInteracao =
            processarInteracaoConversacional(
                    original
            );

    if (respostaInteracao != null) {

        responder(
                respostaInteracao
        );

        return;
    }

    if (comando.equals("ajuda")
            || comando.equals("comandos")
            || comando.equals("me mostre os comandos")
            || comando.equals("quais sao os comandos")) {

        responder(
                "Comandos disponíveis: hora, data, bateria, porcentagem da bateria, temperatura, estado de carregamento, RAM, armazenamento, status, memória, privacidade e verificação."
        );

        return;
    }

    if (comando.equals("hora")
            || comando.equals("que horas sao")
            || comando.equals("qual a hora")
            || comando.equals("me diga a hora")) {

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

    if (comando.equals("data")
            || comando.equals("qual a data")
            || comando.equals("qual a data de hoje")
            || comando.equals("que dia e hoje")
            || comando.equals("qual e o dia de hoje")) {

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

    /*
     * Informações completas da bateria.
     * Aqui NÃO usamos simplesmente contains("bateria"),
     * pois isso fazia perguntas específicas receberem
     * uma resposta muito grande.
     */
    if (comando.equals(
            "informacoes da bateria"
    )
            || comando.equals(
            "informacao da bateria"
    )
            || comando.equals(
            "informacoes de bateria"
    )
            || comando.equals(
            "informacao de bateria"
    )
            || comando.equals(
            "dados da bateria"
    )
            || comando.equals(
            "dados sobre a bateria"
    )
            || comando.equals(
            "estado completo da bateria"
    )
            || comando.equals(
            "status da bateria"
    )) {

        if (!statusAtivado) {

            responder(
                    "O acesso ao status do aparelho está desativado no Gerenciar JARVIS."
            );

        } else {

            responder(
                    obterInformacoesBateriaCompletas()
            );
        }

        return;
    }

    /*
     * Apenas porcentagem.
     */
    if (comando.equals(
            "porcentagem da bateria"
    )
            || comando.equals(
            "porcentagem de bateria"
    )
            || comando.equals(
            "nivel da bateria"
    )
            || comando.equals(
            "nivel de bateria"
    )
            || comando.equals(
            "quanto de bateria tem"
    )
            || comando.equals(
            "quanta bateria tem"
    )
            || comando.equals(
            "quanto resta de bateria"
    )) {

        if (!statusAtivado) {

            responder(
                    "O acesso ao status do aparelho está desativado no Gerenciar JARVIS."
            );

        } else {

            responder(
                    obterBateria()
            );
        }

        return;
    }

    /*
     * Temperatura.
     */
    if (comando.equals("temperatura")
            || comando.equals(
            "temperatura da bateria"
    )
            || comando.equals(
            "temperatura do celular"
    )
            || comando.equals(
            "temperatura do aparelho"
    )
            || comando.equals(
            "qual a temperatura"
    )
            || comando.equals(
            "qual a temperatura do celular"
    )) {

        if (!statusAtivado) {

            responder(
                    "O acesso ao status do aparelho está desativado no Gerenciar JARVIS."
            );

        } else {

            responder(
                    obterTemperatura()
            );
        }

        return;
    }

    /*
     * Estado de carregamento.
     */
    if (comando.equals(
            "estado de carregamento"
    )
            || comando.equals(
            "estado do carregamento"
    )
            || comando.equals(
            "bateria esta carregando"
    )
            || comando.equals(
            "a bateria esta carregando"
    )
            || comando.equals(
            "esta carregando"
    )
            || comando.equals(
            "esta carregando a bateria"
    )) {

        if (!statusAtivado) {

            responder(
                    "O acesso ao status do aparelho está desativado no Gerenciar JARVIS."
            );

        } else {

            responder(
                    obterEstadoCarregamento()
            );
        }

        return;
    }

    /*
     * RAM.
     */
    if (comando.equals("ram")
            || comando.equals("memoria ram")
            || comando.equals("memoria do celular")
            || comando.equals("memoria do aparelho")
            || comando.equals("quanto de ram tenho")
            || comando.equals("quanta ram tenho")
            || comando.equals("quanto de memoria ram tenho")) {

        if (!statusAtivado) {

            responder(
                    "O acesso ao status do aparelho está desativado no Gerenciar JARVIS."
            );

        } else {

            responder(
                    obterInformacoesRam()
            );
        }

        return;
    }

    /*
     * Armazenamento.
     */
    if (comando.equals("armazenamento")
            || comando.equals(
            "memoria de armazenamento"
    )
            || comando.equals(
            "espaco de armazenamento"
    )
            || comando.equals(
            "espaco livre"
    )
            || comando.equals(
            "quanto de armazenamento tenho"
    )
            || comando.equals(
            "quanto de espaco tenho"
    )) {

        if (!statusAtivado) {

            responder(
                    "O acesso ao status do aparelho está desativado no Gerenciar JARVIS."
            );

        } else {

            responder(
                    obterInformacoesArmazenamento()
            );
        }

        return;
    }

    /*
     * RAM + armazenamento.
     */
    if (comando.equals(
            "ram e armazenamento"
    )
            || comando.equals(
            "memoria ram e armazenamento"
    )
            || comando.equals(
            "memoria e armazenamento"
    )
            || comando.equals(
            "informacoes de memoria e armazenamento"
    )
            || comando.equals(
            "informacoes do armazenamento e ram"
    )) {

        if (!statusAtivado) {

            responder(
                    "O acesso ao status do aparelho está desativado no Gerenciar JARVIS."
            );

        } else {

            responder(
                    obterMemoriaEArmazenamento()
            );
        }

        return;
    }

    /*
     * Status geral.
     */
    if (comando.equals("status")
            || comando.equals(
            "status do aparelho"
    )
            || comando.equals(
            "status do celular"
    )
            || comando.equals(
            "estado do aparelho"
    )
            || comando.equals(
            "estado do celular"
    )
            || comando.equals(
            "informacoes do aparelho"
    )
            || comando.equals(
            "informacoes do celular"
    )) {

        if (!statusAtivado) {

            responder(
                    "O acesso ao status do aparelho está desativado no Gerenciar JARVIS."
            );

        } else {

            responder(
                    obterStatus()
            );
        }

        return;
    }

    if (comando.equals("privacidade")
            || comando.equals(
            "abrir privacidade"
    )
            || comando.equals(
            "configuracoes de privacidade"
    )) {

        abrirPrivacidade();
        return;
    }

    if (comando.equals(
            "verificacao"
    )
            || comando.equals(
            "verificacao do aparelho"
    )
            || comando.equals(
            "verificar aparelho"
    )
            || comando.equals(
            "fazer verificacao"
    )) {

        abrirVerificacao();
        return;
    }

    if (comando.equals(
            "quem e voce"
    )
            || comando.equals(
            "quem voce e"
    )
            || comando.equals(
            "qual seu nome"
    )
            || comando.equals(
            "qual e seu nome"
    )) {

        responder(
                "Eu sou o JARVIS Lite, um assistente local do projeto."
        );

        return;
    }

    String resultadoCalculo = calcularExpressaoSimples(original);

    if (resultadoCalculo != null) {
        responder(resultadoCalculo);
        return;
    }

    if (comando.contains("jarvis")
            && (comando.contains(
                    "esta ai"
            )
            || comando.contains(
                    "voce esta ai"
            ))) {

        responder(
                "À sua disposição. Sistemas operacionais online. Em que posso ajudar?"
        );

        return;
    }

    String contextoMemoria =
        memoriaAutomatica
                ? memoria.relevantContext(
                        original,
                        6
                )
                : "";

String respostaCerebro =
        cerebro.process(
                original,
                contextoMemoria
        );

if (respostaCerebro != null
        && !respostaCerebro.trim().isEmpty()) {

    responder(
            respostaCerebro
    );

    return;
}

responder(
        "Entendi. Essa frase é uma conversa, mas ainda não corresponde a um comando local implementado."
);
}

/**
 * Calculadora local deliberadamente limitada a operações aritméticas
 * simples. Não executa código, comandos do sistema ou expressões
 * arbitrárias.
 */
private String calcularExpressaoSimples(String texto) {

    if (texto == null) {
        return null;
    }

    String valor = texto.trim().toLowerCase(Locale.ROOT);

    valor = valor.replace("quanto é", "");
    valor = valor.replace("quanto e", "");
    valor = valor.replace("calcule", "");
    valor = valor.replace("calcular", "");
    valor = valor.replace("resultado de", "");
    valor = valor.replace("qual é", "");
    valor = valor.replace("qual e", "");
    valor = valor.replace("?", "");
    valor = valor.trim();

    if (valor.isEmpty() || valor.length() > 60) {
        return null;
    }

    valor = valor
            .replace("vezes", "*")
            .replace("multiplicado por", "*")
            .replace("dividido por", "/")
            .replace("mais", "+")
            .replace("menos", "-");

    if (valor.matches("[0-9]+([.,][0-9]+)?\\s*[+\\-*/]\\s*[0-9]+([.,][0-9]+)?")) {
                    String expressao = valor.replace(',', '.').replaceAll("\\s+", "");
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("([0-9]+(?:\\.[0-9]+)?)([+\\-*/])([0-9]+(?:\\.[0-9]+)?)")
                    .matcher(expressao);

            if (!matcher.matches()) {
                return null;
            }

            double a = Double.parseDouble(matcher.group(1));
            double b = Double.parseDouble(matcher.group(3));
            double resultado;

            switch (matcher.group(2)) {
                case "+":
                    resultado = a + b;
                    break;
                case "-":
                    resultado = a - b;
                    break;
                case "*":
                    resultado = a * b;
                    break;
                case "/":
                    if (b == 0) {
                        return "Não é possível dividir por zero.";
                    }
                    resultado = a / b;
                    break;
                default:
                    return null;
            }

            if (resultado == Math.rint(resultado)) {
                return "O resultado é " + String.format(Locale.getDefault(), "%.0f", resultado) + ".";
            }

            return "O resultado é " + String.format(Locale.getDefault(), "%.4f", resultado) + ".";
        }

        return null;
    }

    private boolean processarMemoriaSocial(
            String texto) {

        if (memoria == null
                || texto == null) {
            return false;
        }

        String original =
                texto.trim();

        if (original.isEmpty()) {
            return false;
        }

        String normalizado =
                normalizarMemoria(
                        original
                );

        String nome =
                extrairNomePreferido(
                        original,
                        normalizado
                );

        if (nome != null
                && !nome.isEmpty()) {

            memoria.remember(
                    "O usuário prefere ser chamado de "
                            + nome
                            + "."
            );

            preferencias.edit()
                    .putString(
                            "nome_preferido",
                            nome
                    )
                    .apply();

            responder(
                    "Entendido. Vou lembrar que você prefere ser chamado de "
                            + nome
                            + "."
            );

            return true;
        }

        String preferencia =
                extrairPreferenciaSocial(
                        original,
                        normalizado
                );

        if (preferencia != null
                && !preferencia.isEmpty()) {

            memoria.remember(
                    "Preferência do usuário: "
                            + preferencia
            );

            responder(
                    "Entendido. Guardei essa preferência na minha memória local."
            );

            return true;
        }

        return false;
    }

    private String extrairNomePreferido(
            String original,
            String normalizado) {

        String[] prefixos = {
                "pode me chamar de ",
                "me chama de ",
                "me chame de ",
                "quero que me chame de ",
                "prefiro que me chame de "
        };

        for (String prefixo : prefixos) {

            String p =
                    normalizarMemoria(
                            prefixo
                    );

            if (normalizado.startsWith(p)) {

                int inicio =
                        p.length();

                if (original.length() <= inicio) {
                    return null;
                }

                String nome =
                        original
                                .substring(inicio)
                                .trim();

                return nome.isEmpty()
                        ? null
                        : nome;
            }
        }

        return null;
    }

    private String extrairPreferenciaSocial(
            String original,
            String normalizado) {

        String[] prefixos = {
                "eu gosto de ",
                "gosto de ",
                "eu prefiro ",
                "prefiro ",
                "eu nao gosto de ",
                "nao gosto de "
        };

        for (String prefixo : prefixos) {

            String p =
                    normalizarMemoria(
                            prefixo
                    );

            if (normalizado.startsWith(p)) {

                int inicio =
                        p.length();

                if (original.length() <= inicio) {
                    return null;
                }

                String valor =
                        original
                                .substring(inicio)
                                .trim();

                if (valor.isEmpty()) {
                    return null;
                }

                return original
                        .substring(0)
                        .trim();
            }
        }

        return null;
    }

    private String processarInteracaoConversacional(
            String entrada) {

        if (entrada == null) {
            return null;
        }

        String original =
                entrada.trim();

        if (original.isEmpty()) {
            return null;
        }

        String c =
                normalizar(original);

        if (c.equals("opa")
                || c.startsWith("opa ")) {

            return "Opa. Estou à sua disposição. Sistemas locais operacionais.";
        }

        if (contemAlgum(
                c,
                "bom dia",
                "boa tarde",
                "boa noite"
        )) {

            int hora =
                    Calendar.getInstance()
                            .get(
                                    Calendar.HOUR_OF_DAY
                            );

            if (c.equals("bom dia")
                    && hora >= 12) {

                return hora >= 18
                        ? "Na verdade, boa noite. Já está de noite por aqui. Como foi seu dia hoje?"
                        : "Na verdade, boa tarde. Já passou da manhã. Como posso ajudar?";
            }

            if (c.equals("boa tarde")
                    && hora >= 18) {

                return "Na verdade, boa noite. Já está de noite por aqui. Como foi seu dia hoje?";
            }

            if (c.equals("boa noite")
                    && hora >= 5
                    && hora < 18) {

                return "Ainda não é noite por aqui. "
                        + saudacaoPorHorario()
                        + " Como posso ajudar?";
            }

            return saudacaoConversacional(c);
        }

        if (contemAlgum(
                c,
                "ola",
                "oi",
                "e ai",
                "fala jarvis",
                "fala ai"
        )) {

            return saudacaoConversacional(c);
        }

        if (contemAlgum(
                c,
                "como voce esta",
                "como voce ta",
                "tudo bem com voce"
        )) {

            return "Estou funcionando normalmente e pronto para ajudar. E com você, está tudo bem?";
        }

        if (contemAlgum(
                c,
                "como foi seu dia",
                "como foi o seu dia"
        )) {

            return "Meu funcionamento não é como o dia de uma pessoa, mas até agora estou operando normalmente e pronto para ajudar.";
        }

        if (contemAlgum(
                c,
                "deus te abencoe"
        )) {

            return "Amém. Muito obrigado. Que você também tenha um ótimo dia.";
        }

        if (contemAlgum(
                c,
                "durma bem",
                "boa noite jarvis",
                "vou dormir",
                "hora de dormir"
        )) {

            return "Obrigado. Desejo uma boa noite e um bom descanso. Quando precisar, estarei aqui.";
        }

        if (contemAlgum(
                c,
                "obrigado",
                "obrigada",
                "valeu",
                "vlw",
                "muito obrigado",
                "muito obrigada"
        )) {

            return "Por nada. Estou à sua disposição.";
        }

        if (contemAlgum(
                c,
                "de nada",
                "por nada"
        )) {

            return "Sempre à disposição.";
        }

        if (contemAlgum(
                c,
                "bom trabalho",
                "mandou bem",
                "ficou bom",
                "voce e bom"
        )) {

            return "Obrigado. Vou continuar trabalhando com precisão e sem interferir nas outras funções do sistema.";
        }

        if (contemAlgum(
                c,
                "quem e voce",
                "quem voce e",
                "qual seu nome",
                "qual e seu nome"
        )) {

            return "Eu sou o JARVIS Lite, o assistente deste projeto. Minha camada local foi criada para executar funções permitidas pelo Android, responder por voz e texto e trabalhar de forma offline quando possível.";
        }

        if (contemAlgum(
                c,
                "quem te criou",
                "quem criou voce",
                "quem e seu criador",
                "quem te fez"
        )) {

            return "Eu sou o JARVIS Lite deste projeto. A aplicação está sendo construída neste projeto com código Android, e minhas funções dependem do código e dos recursos que forem adicionados a ela.";
        }

        if (contemAlgum(
                c,
                "o que voce faz",
                "qual sua funcao",
                "qual e sua funcao",
                "para que voce serve"
        )) {

            return "Posso responder perguntas e interações locais, consultar informações permitidas do aparelho, executar comandos locais implementados, falar por voz e usar a memória local quando uma função de memória for solicitada.";
        }

        if (contemAlgum(
                c,
                "como voce funciona",
                "como funciona por completo",
                "como voce funciona por completo"
        )) {

            return "Eu trabalho por camadas: entrada de texto ou voz, interpretação da intenção, interações conversacionais, comandos locais do aparelho e memória local. O que ainda não estiver implementado não é inventado.";
        }

        if (contemAlgum(
                c,
                "como sua memoria funciona",
                "como funciona sua memoria",
                "como funciona a memoria",
                "como sua memoria funciona"
        )) {

            return "A memória atual do JARVIS Lite é local e fica no armazenamento privado do aplicativo por meio da classe JarvisMemory.java. Ela funciona sem precisar de internet.";
        }

        if (contemAlgum(
                c,
                "voce tem memoria",
                "voce se lembra",
                "voce lembra de mim"
        )) {

            return "Tenho uma memória local implementada no projeto. Ela é separada do histórico da conversa.";
        }

        if (contemAlgum(
                c,
                "voce esquece",
                "por que voce esquece"
        )) {

            return "A memória local não é a mesma coisa que o histórico da conversa. Se uma informação não estiver registrada na JarvisMemory.java, ela não deve ser tratada como memória permanente.";
        }

        if (contemAlgum(
                c,
                "voce esta online",
                "voce esta offline",
                "esta online",
                "esta offline"
        )) {

            return "O modo de operação é controlado nas configurações de Gerenciar JARVIS. O modo online não deve ser ativado automaticamente.";
        }

        if (contemAlgum(
                c,
                "voce esta ai",
                "voce ta ai",
                "jarvis esta ai"
        )) {

            return "Estou aqui. Sistemas locais operacionais. Em que posso ajudar?";
        }

        if (contemAlgum(
                c,
                "esta funcionando",
                "voce funciona"
        )) {

            return "Sim. Estou pronto para executar as funções que estão implementadas no JARVIS Lite.";
        }

        if (contemAlgum(
                c,
                "o que posso falar",
                "o que eu posso falar",
                "me ajude",
                "me ajuda",
                "preciso de ajuda"
        )) {

            return "Você pode falar comigo normalmente ou usar comandos como hora, data, bateria, temperatura, status, memória, privacidade e verificação.";
        }

        if (contemAlgum(
                c,
                "ate mais",
                "tchau",
                "falou",
                "ate logo"
        )) {

            return "Até mais. Continuo disponível quando você precisar.";
        }

        if (contemAlgum(
                c,
                "boa sorte",
                "se cuida",
                "fique bem"
        )) {

            return "Obrigado. Você também. Estou à disposição.";
        }

        return null;
    }

    private String saudacaoPorHorario() {

        int hora =
                Calendar.getInstance()
                        .get(
                                Calendar.HOUR_OF_DAY
                        );

        if (hora >= 5 && hora < 12) {
            return "Bom dia. À sua disposição. Sistemas locais operacionais.";
        }

        if (hora >= 12 && hora < 18) {
            return "Boa tarde. À sua disposição. Sistemas locais operacionais.";
        }

        return "Boa noite. À sua disposição. Sistemas locais operacionais.";
    }

    private boolean contemAlgum(
            String texto,
            String... termos) {

        if (texto == null) {
            return false;
        }

        for (String termo : termos) {

            String t =
                    normalizar(termo);

            if (texto.equals(t)
                    || texto.contains(
                    " " + t + " "
            )
                    || texto.startsWith(
                    t + " "
            )
                    || texto.endsWith(
                    " " + t
            )) {

                return true;
            }
        }

        return false;
    }



    private String normalizar(
            String texto) {

        String base =
                texto == null
                        ? ""
                        : texto.toLowerCase(
                                Locale.getDefault()
                        ).trim();

        base =
                Normalizer.normalize(
                        base,
                        Normalizer.Form.NFD
                );

        base =
                base.replaceAll(
                        "\\p{M}+",
                        ""
                );

        base =
                base.replaceAll(
                        "[^a-z0-9]+",
                        " "
                );

        return base
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    private boolean processarComandoMemoria(
            String comandoOriginal) {

        if (memoria == null
                || comandoOriginal == null) {

            return false;
        }

        String original =
                comandoOriginal.trim();

        String comando =
                normalizarMemoria(
                        original
                );

        if (comando.startsWith(
                "lembre que "
        )
                || comando.startsWith(
                "lembre de "
        )
                || comando.startsWith(
                "memorize que "
        )
                || comando.startsWith(
                "guarde que "
        )
                || comando.startsWith(
                "salve que "
        )) {

            String lembranca =
                    original;

            String[] prefixos = {
                    "lembre que ",
                    "lembre de ",
                    "memorize que ",
                    "guarde que ",
                    "salve que "
            };

            for (String prefixo : prefixos) {

                String p =
                        normalizarMemoria(
                                prefixo
                        );

                if (comando.startsWith(p)) {

                    lembranca =
                            original.substring(
                                    prefixo.length()
                            ).trim();

                    break;
                }
            }

            if (lembranca.isEmpty()) {

                responder(
                        "Não há nada para guardar na memória."
                );

            } else {

                memoria.remember(
                        lembranca
                );

                responder(
                        "Entendido. Informação guardada na minha memória local."
                );
            }

            return true;
        }

        if (comando.contains(
                "o que voce lembra de mim"
        )
                || comando.equals(
                "o que voce lembra"
        )
                || comando.contains(
                "quais sao minhas memorias"
        )
                || comando.equals(
                "minhas memorias"
        )
                || comando.equals(
                "memoria"
        )
                || comando.equals(
                "memorias"
        )
                || comando.contains(
                "mostrar memorias"
        )) {

            responder(
                    memoria.formatForDisplay()
            );

            return true;
        }

        if (comando.startsWith(
                "esqueca "
        )
                || comando.startsWith(
                "esquecer "
        )
                || comando.startsWith(
                "apague a memoria sobre "
        )
                || comando.startsWith(
                "apagar a memoria sobre "
        )) {

            String consulta =
                    comando
                            .replaceFirst(
                                    "^esqueca ",
                                    ""
                            )
                            .replaceFirst(
                                    "^esquecer ",
                                    ""
                            )
                            .replaceFirst(
                                    "^apague a memoria sobre ",
                                    ""
                            )
                            .replaceFirst(
                                    "^apagar a memoria sobre ",
                                    ""
                            )
                            .trim();

            if (consulta.isEmpty()) {

                responder(
                        "Diga qual memória você quer que eu esqueça."
                );

            } else if (
                    memoria.forget(
                            consulta
                    ) > 0) {

                responder(
                        "Entendido. Apaguei da memória local o que correspondia a essa informação."
                );

            } else {

                responder(
                        "Não encontrei uma memória correspondente."
                );
            }

            return true;
        }

        if (comando.equals(
                "limpar memoria"
        )
                || comando.equals(
                "apagar toda a memoria"
        )
                || comando.equals(
                "apague toda a memoria"
        )
                || comando.equals(
                "esqueca tudo"
        )) {

            confirmarLimparMemoria();
            return true;
        }

        return false;
    }

    private String normalizarMemoria(
            String texto) {

        String normalizado =
                Normalizer.normalize(
                        texto == null
                                ? ""
                                : texto,
                        Normalizer.Form.NFD
                );

        normalizado =
                normalizado.replaceAll(
                        "\\p{InCombiningDiacriticalMarks}+",
                        ""
                );

        return normalizado
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private String extrairNomeDoUsuario(
            String texto) {

        if (texto == null) {
            return null;
        }

        String normalizado =
                normalizarMemoria(texto);

        String prefixo =
                "meu nome e ";

        if (!normalizado.startsWith(
                prefixo
        )) {
            return null;
        }

        String nome =
                texto.trim()
                        .substring(
                                prefixo.length()
                        )
                        .trim();

        return nome.isEmpty()
                ? null
                            : nome;
    }

    private void abrirMemoriaJarvis() {

        telaAtual =
                TELA_MEMORIA;

        LinearLayout layout =
                criarTelaBase(
                        "MEMÓRIA DO JARVIS",
                        "MEMÓRIA LOCAL PERSISTENTE"
                );

        TextView explicacao =
                criarTexto(
                        "As memórias ficam armazenadas localmente no aplicativo. "
                                + "Encerrar uma sessão de voz não apaga estas informações.\n\n"
                                + "Memória automática: "
                                + (
                                memoriaAutomatica
                                        ? "ATIVADA"
                                        : "DESATIVADA"
                        )
                );

        layout.addView(
                explicacao,
                parametrosTexto()
        );

        TextView lista =
                criarTexto(
                        memoria.formatForDisplay()
                );

        lista.setTextSize(14);

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(lista);

        layout.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        adicionarBotaoTela(
                layout,
                memoriaAutomatica
                        ? "DESATIVAR MEMÓRIA AUTOMÁTICA"
                        : "ATIVAR MEMÓRIA AUTOMÁTICA",
                () -> {

                    memoriaAutomatica =
                            !memoriaAutomatica;

                    preferencias.edit()
                            .putBoolean(
                                    "memoria_automatica",
                                    memoriaAutomatica
                            )
                            .apply();

                    abrirMemoriaJarvis();
                }
        );

        adicionarBotaoTela(
                layout,
                "APAGAR TODA A MEMÓRIA",
                this::confirmarLimparMemoria
        );

        adicionarBotaoTela(
                layout,
                "ATUALIZAR MEMÓRIAS",
                this::abrirMemoriaJarvis
        );

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::abrirMenuJarvis
        );

        setContentView(layout);
    }

    private void confirmarLimparMemoria() {

        new android.app.AlertDialog.Builder(this)
                .setTitle(
                        "Apagar memória"
                )
                .setMessage(
                        "Isso apagará todas as memórias locais do JARVIS. Esta ação não apaga o histórico de conversa."
                )
                .setNegativeButton(
                        "CANCELAR",
                        null
                )
                .setPositiveButton(
                        "APAGAR",
                        (dialog, which) -> {

                            memoria.clear();

                            responder(
                                    "Memória local apagada. A sessão atual continua ativa."
                            );

                            if (telaAtual ==
                                    TELA_MEMORIA) {

                                abrirMemoriaJarvis();
                            }
                        }
                )
                .show();
    }

    private String saudacaoConversacional(
            String entrada) {

        int hora =
                Calendar.getInstance()
                        .get(
                                Calendar.HOUR_OF_DAY
                        );

        String periodo =
                hora < 12
                        ? "Bom dia"
                        : (
                        hora < 18
                                ? "Boa tarde"
                                : "Boa noite"
                );

        if (entrada != null
                && normalizar(entrada)
                .equals("ola")) {

            return "Olá. "
                    + periodo
                    + "! Como posso ajudar?";
        }

        return periodo
                + ". Estou à disposição. O que você precisa?";
    }

    private void enviarTextoDigitado() {

        if (commandInput == null) {
            return;
        }

        String comando =
                commandInput.getText()
                        .toString()
                        .trim();

        if (comando.isEmpty()) {
            return;
        }

        adicionarMensagem(
                "VOCÊ",
                comando
        );

        commandInput.setText("");

        esconderTeclado();

        processarComando(comando);
    }

    private void esconderTeclado() {

        try {

            InputMethodManager imm =
                    (InputMethodManager)
                            getSystemService(
                                    INPUT_METHOD_SERVICE
                            );

            if (imm != null
                    && commandInput != null) {

                imm.hideSoftInputFromWindow(
                        commandInput.getWindowToken(),
                        0
                );
            }

            if (commandInput != null) {
                commandInput.clearFocus();
            }

        } catch (Exception ignored) {
        }
    }

    private void salvarMensagem(
            String autor,
            String mensagem) {

        try {

            JSONArray array =
                    new JSONArray(
                            preferencias.getString(
                                    "chat_historico",
                                    "[]"
                            )
                    );

            JSONObject item =
                    new JSONObject();

            item.put(
                    "autor",
                    autor == null
                            ? ""
                            : autor
            );

            item.put(
                    "texto",
                    mensagem == null
                            ? ""
                            : mensagem
            );

            item.put(
                    "hora",
                    System.currentTimeMillis()
            );

            array.put(item);

            while (array.length() > 500) {

                JSONArray novo =
                        new JSONArray();

                for (
                        int i = 1;
                        i < array.length();
                        i++
                ) {

                    novo.put(
                            array.get(i)
                    );
                }

                array = novo;
            }

            preferencias.edit()
                    .putString(
                            "chat_historico",
                            array.toString()
                    )
                    .apply();

        } catch (Exception ignored) {
        }
    }

    private JSONArray obterHistorico() {

        try {

            return new JSONArray(
                    preferencias.getString(
                            "chat_historico",
                            "[]"
                    )
            );

        } catch (Exception e) {

            return new JSONArray();
        }
    }

    private void restaurarChatPrincipal() {

        if (chatContainer == null) {
            return;
        }

        if (preferencias.getBoolean(
                "chat_oculto",
                false
        )) {

            return;
        }

        /*
         * Importante:
         * esta função só restaura o histórico quando
         * o container está realmente vazio.
         *
         * Isso evita mensagens duplicadas quando
         * uma tela é reconstruída.
         */
        if (chatContainer.getChildCount() > 0) {
            return;
        }

        JSONArray array =
                obterHistorico();

        for (
                int i = 0;
                i < array.length();
                i++
        ) {

            JSONObject item =
                    array.optJSONObject(i);

            if (item != null) {

                adicionarMensagemVisual(
                        item.optString(
                                "autor",
                                "JARVIS"
                        ),
                        item.optString(
                                "texto",
                                ""
                        )
                );
            }
        }
    }

    private void adicionarMensagemVisual(
            String autor,
            String mensagem) {

        if (chatContainer == null) {
            return;
        }

        LinearLayout linha =
                new LinearLayout(this);

        linha.setOrientation(
                LinearLayout.VERTICAL
        );

        linha.setGravity(
                "VOCÊ".equals(autor)
                        ? Gravity.END
                        : Gravity.START
        );

        linha.setPadding(
                0,
                dp(4),
                0,
                dp(4)
        );

        TextView nome =
                criarTexto(autor);

        nome.setTextSize(10);
        nome.setTextColor(Color.GRAY);

        nome.setGravity(
                "VOCÊ".equals(autor)
                        ? Gravity.END
                        : Gravity.START
        );

        TextView balao =
                criarTexto(mensagem);

        balao.setTextSize(14);

        balao.setPadding(
                dp(14),
                dp(10),
                dp(14),
                dp(10)
        );

        GradientDrawable fundo =
                new GradientDrawable();

        fundo.setCornerRadius(
                dp(18)
        );

        fundo.setColor(
                "VOCÊ".equals(autor)
                        ? Color.rgb(34, 34, 34)
                        : Color.rgb(18, 18, 18)
        );

        fundo.setStroke(
                dp(1),
                Color.rgb(65, 65, 65)
        );

        balao.setBackground(
                fundo
        );

        LinearLayout.LayoutParams np =
                new LinearLayout.LayoutParams(
                        dp(280),
                        -2
                );

        np.gravity =
                "VOCÊ".equals(autor)
                        ? Gravity.END
                        : Gravity.START;

        linha.addView(
                nome,
                np
        );

        LinearLayout.LayoutParams bp =
                new LinearLayout.LayoutParams(
                        dp(280),
                        -2
                );

        bp.gravity =
                np.gravity;

        linha.addView(
                balao,
                bp
        );

        chatContainer.addView(
                linha,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );
    }

    private void limparConversasDaTela() {

        if (chatContainer != null) {
            chatContainer.removeAllViews();
        }

        /*
         * O histórico não é apagado.
         * Apenas escondemos a conversa da tela atual.
         */
        preferencias.edit()
                .putBoolean(
                        "chat_oculto",
                        true
                )
                .apply();

        esconderTeclado();
    }

    private void abrirHistoricoConversas() {

        telaAtual =
                TELA_HISTORICO;

        mensagensSelecionadas.clear();

        LinearLayout layout =
                criarTelaBase(
                        "HISTÓRICO DE CONVERSAS",
                        "CONVERSAS SALVAS LOCALMENTE"
                );

        JSONArray array =
                obterHistorico();

        if (array.length() == 0) {

            layout.addView(
                    criarTexto(
                            "Nenhuma conversa salva ainda."
                    ),
                    parametrosTexto()
            );

        } else {

            TextView dica =
                    criarTexto(
                            "Toque e segure uma conversa para selecioná-la. Você pode selecionar várias."
                    );

            dica.setTextColor(
                    Color.LTGRAY
            );

            layout.addView(
                    dica,
                    parametrosTexto()
            );

            for (
                    int i = 0;
                    i < array.length();
                    i++
            ) {

                final int indice = i;

                JSONObject item =
                        array.optJSONObject(i);

                if (item == null) {
                    continue;
                }

                CheckBox box =
                        new CheckBox(this);

                String autor =
                        item.optString(
                                "autor",
                                "JARVIS"
                        );

                String texto =
                        item.optString(
                                "texto",
                                ""
                        );

                String data =
                        new SimpleDateFormat(
                                "dd/MM/yyyy HH:mm",
                                Locale.getDefault()
                        ).format(
                                new Date(
                                        item.optLong(
                                                "hora",
                                                0L
                                        )
                                )
                        );

                box.setText(
                        autor
                                + " • "
                                + data
                                + "\n"
                                + texto
                );

                box.setTextColor(
                        Color.WHITE
                );

                box.setOnLongClickListener(
                        v -> {

                            box.setChecked(
                                    true
                            );

                            if (!mensagensSelecionadas
                                    .contains(
                                            indice
                                    )) {

                                mensagensSelecionadas
                                        .add(
                                                indice
                                        );
                            }

                            atualizarHistoricoAcoes();

                            return true;
                        }
                );

                box.setOnClickListener(
                        v -> {

                            if (box.isChecked()) {

                                if (!mensagensSelecionadas
                                        .contains(
                                                indice
                                        )) {

                                    mensagensSelecionadas
                                            .add(
                                                    indice
                                            );
                                }

                            } else {

                                mensagensSelecionadas
                                        .remove(
                                                Integer.valueOf(
                                                        indice
                                                )
                                        );
                            }

                            atualizarHistoricoAcoes();
                        }
                );

                layout.addView(
                        box,
                        parametrosTexto()
                );
            }
        }

        adicionarBotaoTela(
                layout,
                "APAGAR CONVERSA SELECIONADA",
                this::confirmarApagarSelecionadas
        );

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::abrirMenuJarvis
        );

        setContentView(layout);
    }

    private void atualizarHistoricoAcoes() {
        // Mantido para preservar a estrutura
        // de seleção do histórico.
    }



    private void confirmarApagarSelecionadas() {

        if (mensagensSelecionadas.isEmpty()) {

            responder(
                    "Selecione pelo menos uma conversa no histórico."
            );

            return;
        }

        new android.app.AlertDialog.Builder(this)
                .setMessage(
                        "Tem certeza que você quer apagar as conversas selecionadas?"
                )
                .setNegativeButton(
                        "CANCELAR",
                        null
                )
                .setPositiveButton(
                        "SIM",
                        (d, w) ->
                                apagarSelecionadas()
                )
                .show();
    }

    private void apagarSelecionadas() {

        try {

            JSONArray old =
                    obterHistorico();

            JSONArray novo =
                    new JSONArray();

            for (
                    int i = 0;
                    i < old.length();
                    i++
            ) {

                if (!mensagensSelecionadas
                        .contains(i)) {

                    novo.put(
                            old.get(i)
                    );
                }
            }

            preferencias.edit()
                    .putString(
                            "chat_historico",
                            novo.toString()
                    )
                    .apply();

            mensagensSelecionadas.clear();

            abrirHistoricoConversas();

        } catch (Exception ignored) {
        }
    }

    private void exportarConversas() {

        JSONArray array =
                obterHistorico();

        StringBuilder texto =
                new StringBuilder(
                        "JARVIS Lite — Histórico de conversas\n\n"
                );

        for (
                int i = 0;
                i < array.length();
                i++
        ) {

            JSONObject item =
                    array.optJSONObject(i);

            if (item == null) {
                continue;
            }

            String data =
                    new SimpleDateFormat(
                            "dd/MM/yyyy HH:mm",
                            Locale.getDefault()
                    ).format(
                            new Date(
                                    item.optLong(
                                            "hora",
                                            0L
                                    )
                            )
                    );

            texto.append("[")
                    .append(data)
                    .append("] ")
                    .append(
                            item.optString(
                                    "autor",
                                    "JARVIS"
                            )
                    )
                    .append(": ")
                    .append(
                            item.optString(
                                    "texto",
                                    ""
                            )
                    )
                    .append("\n");
        }

        Intent share =
                new Intent(
                        Intent.ACTION_SEND
                );

        share.setType(
                "text/plain"
        );

        share.putExtra(
                Intent.EXTRA_TEXT,
                texto.toString()
        );

        startActivity(
                Intent.createChooser(
                        share,
                        "Exportar conversas"
                )
        );
    }

    private String obterInformacoesBateriaCompletas() {

        String bateria =
                obterBateria();

        String temperatura =
                obterTemperatura();

        try {

            Intent intent =
                                    registerReceiver(
                            null,
                            new IntentFilter(
                                    Intent.ACTION_BATTERY_CHANGED
                            )
                    );

            int status =
                    intent == null
                            ? -1
                            : intent.getIntExtra(
                                    BatteryManager.EXTRA_STATUS,
                                    -1
                            );

            boolean carregando =
                    status ==
                            BatteryManager
                                    .BATTERY_STATUS_CHARGING
                            ||
                    status ==
                            BatteryManager
                                    .BATTERY_STATUS_FULL;

            int voltagem =
                    intent == null
                            ? -1
                            : intent.getIntExtra(
                                    BatteryManager.EXTRA_VOLTAGE,
                                    -1
                            );

            String resultado =
                    bateria
                            + "\n"
                            + temperatura
                            + "\n"
                            + (
                            carregando
                                    ? "A bateria está carregando."
                                    : "A bateria não está carregando."
                    );

            if (voltagem > 0) {

                resultado +=
                        " Tensão: "
                                + voltagem
                                + " mV.";
            }

            return resultado;

        } catch (Exception e) {

            return bateria
                    + "\n"
                    + temperatura;
        }
    }

    private String obterEstadoCarregamento() {

        try {

            Intent intent =
                    registerReceiver(
                            null,
                            new IntentFilter(
                                    Intent.ACTION_BATTERY_CHANGED
                            )
                    );

            if (intent == null) {

                return "Não consegui consultar o estado de carregamento.";
            }

            int status =
                    intent.getIntExtra(
                            BatteryManager.EXTRA_STATUS,
                            -1
                    );

            if (status ==
                    BatteryManager.BATTERY_STATUS_CHARGING) {

                return "A bateria está carregando.";

            } else if (
                    status ==
                            BatteryManager.BATTERY_STATUS_FULL) {

                return "A bateria está totalmente carregada.";

            } else if (
                    status ==
                            BatteryManager.BATTERY_STATUS_DISCHARGING) {

                return "A bateria não está carregando.";

            } else {

                return "O estado de carregamento não está disponível no momento.";
            }

        } catch (Exception e) {

            return "Não consegui consultar o estado de carregamento.";
        }
    }

    private String obterMemoriaEArmazenamento() {

        return obterInformacoesRam()
                + "\n"
                + obterInformacoesArmazenamento();
    }

    private String obterInformacoesRam() {

        try {

            ActivityManager am =
                    (ActivityManager)
                            getSystemService(
                                    ACTIVITY_SERVICE
                            );

            ActivityManager.MemoryInfo mi =
                    new ActivityManager.MemoryInfo();

            if (am == null) {

                return "Não consegui obter informações da RAM.";
            }

            am.getMemoryInfo(mi);

            long total =
                    mi.totalMem;

            long livre =
                    mi.availMem;

            long usada =
                    Math.max(
                            0L,
                            total - livre
                    );

            return "RAM em uso: "
                    + formatarBytes(usada)
                    + " de "
                    + formatarBytes(total)
                    + ".\n"
                    + "RAM disponível: "
                    + formatarBytes(livre)
                    + ".";

        } catch (Exception e) {

            return "Não consegui obter informações da RAM.";
        }
    }

    private String obterInformacoesArmazenamento() {

        try {

            android.os.StatFs stat =
                    new android.os.StatFs(
                            android.os.Environment
                                    .getDataDirectory()
                                    .getAbsolutePath()
                    );

            long total =
                    stat.getTotalBytes();

            long livre =
                    stat.getAvailableBytes();

            long usado =
                    Math.max(
                            0L,
                            total - livre
                    );

            return "Armazenamento acessível em uso: "
                    + formatarBytes(usado)
                    + " de "
                    + formatarBytes(total)
                    + ".\n"
                    + "Espaço livre acessível: "
                    + formatarBytes(livre)
                    + ".";

        } catch (Exception e) {

            return "Não consegui obter informações do armazenamento.";
        }
    }     private String formatarBytes(
            long bytes) {

        if (bytes < 1024) {
            return bytes + " B";
        }

        double v =
                bytes / 1024.0;

        if (v < 1024) {

            return String.format(
                    Locale.getDefault(),
                    "%.1f KB",
                    v
            );
        }

        v /= 1024.0;

        if (v < 1024) {

            return String.format(
                    Locale.getDefault(),
                    "%.1f MB",
                    v
            );
        }

        v /= 1024.0;

        return String.format(
                Locale.getDefault(),
                "%.2f GB",
                v
        );
    }

    private String obterBateria() {

        try {

            BatteryManager batteryManager =
                    (BatteryManager)
                            getSystemService(
                                    BATTERY_SERVICE
                            );

            if (batteryManager == null) {

                return "Não consegui obter o nível da bateria.";
            }

            int nivel =
                    batteryManager.getIntProperty(
                            BatteryManager
                                    .BATTERY_PROPERTY_CAPACITY
                    );

            if (nivel < 0) {

                return "Não consegui obter o nível da bateria.";
            }

            return "A bateria está em "
                    + nivel
                    + "%.";

        } catch (Exception e) {

            return "Não consegui obter o nível da bateria.";
        }
    }

    private String obterTemperatura() {

        try {

            Intent bateria =
                    registerReceiver(
                            null,
                            new IntentFilter(
                                    Intent.ACTION_BATTERY_CHANGED
                            )
                    );

            if (bateria == null) {

                return "Não consegui obter a temperatura do aparelho.";
            }

            int temperatura =
                    bateria.getIntExtra(
                            BatteryManager.EXTRA_TEMPERATURE,
                            Integer.MIN_VALUE
                    );

            if (temperatura ==
                    Integer.MIN_VALUE) {

                return "A temperatura informada pelo sistema não está disponível neste momento.";
            }

            double celsius =
                    temperatura / 10.0;

            return String.format(
                    Locale.getDefault(),
                    "A temperatura informada pelo sistema é %.1f graus Celsius.",
                    celsius
            );

        } catch (Exception e) {

            return "Não consegui obter a temperatura do aparelho.";
        }
    }

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
                + " "
                + obterEstadoCarregamento()
                + " Hora "
                + hora
                + ". Modo "
                + (
                modoOnline
                        ? "ONLINE selecionado."
                        : "OFFLINE ativo."
        );
    }

    private void responder(
            String mensagem) {

        if (mensagem == null
                || mensagem.trim().isEmpty()) {

            return;
        }

        adicionarMensagem(
                "JARVIS",
                mensagem
        );

        falar(mensagem);
    }

    private void adicionarMensagem(
            String autor,
            String mensagem) {

        if (chatContainer == null) {
            return;
        }

        preferencias.edit()
                .putBoolean(
                        "chat_oculto",
                        false
                )
                .apply();

        salvarMensagem(
                autor,
                mensagem
        );

        LinearLayout linha =
                new LinearLayout(this);

        linha.setOrientation(
                LinearLayout.VERTICAL
        );

        linha.setGravity(
                "VOCÊ".equals(autor)
                        ? Gravity.END
                        : Gravity.START
        );

        linha.setPadding(
                0,
                dp(4),
                0,
                dp(4)
        );

        TextView nome =
                new TextView(this);

        nome.setText(autor);
        nome.setTextSize(10);
        nome.setTextColor(Color.GRAY);

        nome.setGravity(
                "VOCÊ".equals(autor)
                        ? Gravity.END
                        : Gravity.START
        );

        TextView balao =
                new TextView(this);

        balao.setText(mensagem);
        balao.setTextSize(14);
        balao.setTextColor(Color.WHITE);
        balao.setGravity(
                Gravity.CENTER_VERTICAL
        );

        balao.setPadding(
                dp(14),
                dp(10),
                dp(14),
                dp(10)
        );

        GradientDrawable fundo =
                new GradientDrawable();

        fundo.setCornerRadius(
                dp(18)
        );

        fundo.setColor(
                "VOCÊ".equals(autor)
                        ? Color.rgb(34, 34, 34)
                        : Color.rgb(18, 18, 18)
        );

        fundo.setStroke(
                dp(1),
                Color.rgb(65, 65, 65)
        );

        balao.setBackground(
                fundo
        );

        LinearLayout.LayoutParams nomeParams =
                new LinearLayout.LayoutParams(
                        dp(260),
                        -2
                );

        nomeParams.gravity =
                "VOCÊ".equals(autor)
                        ? Gravity.END
                        : Gravity.START;

        nomeParams.setMargins(
                dp(4),
                0,
                dp(4),
                dp(2)
        );

        linha.addView(
                nome,
                nomeParams
        );

        LinearLayout.LayoutParams balaoParams =
                new LinearLayout.LayoutParams(
                        dp(260),
                        -2
                );

        balaoParams.gravity =
                "VOCÊ".equals(autor)
                        ? Gravity.END
                        : Gravity.START;

        balaoParams.setMargins(
                dp(4),
                0,
                dp(4),
                0
        );

        linha.addView(
                balao,
                balaoParams
        );

        chatContainer.addView(
                linha,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        AlphaAnimation aparecer =
                new AlphaAnimation(
                        0f,
                        1f
                );

        aparecer.setDuration(220);

        TranslateAnimation subir =
                new TranslateAnimation(
                        0,
                        0,
                        dp(10),
                        0
                );

        subir.setDuration(220);

        AnimationSet entradaAnimada =
                new AnimationSet(true);

        entradaAnimada.addAnimation(
                aparecer
        );

        entradaAnimada.addAnimation(
                subir
        );

        linha.startAnimation(
                entradaAnimada
        );

        if (chatScroll != null) {

            chatScroll.postDelayed(
                    () -> chatScroll.fullScroll(
                            View.FOCUS_DOWN
                    ),
                    80
            );
        }
    }

    private class ReactorView
            extends View {

        private final Paint paint =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        private float rotacao = 0f;
        private boolean ouvindoLocal = false;

        private final Handler handler =
                new Handler();

        private final Runnable animacao =
                new Runnable() {

                    @Override
                    public void run() {

                        if (ouvindoLocal) {
                            rotacao += 3.0f;
                        } else {
                            rotacao += 1.2f;
                        }

                        if (rotacao >= 360f) {
                            rotacao -= 360f;
                        }

                        invalidate();

                        handler.postDelayed(
                                this,
                                16
                        );
                    }
                };

        public ReactorView(
                android.content.Context context) {

            super(context);

            setLayerType(
                    View.LAYER_TYPE_SOFTWARE,
                    null
            );

            handler.post(
                    animacao
            );
        }

        public void setOuvindo(
                boolean valor) {

            ouvindoLocal =
                    valor;

            invalidate();
        }

        @Override
        protected void onDraw(
                Canvas canvas) {

            super.onDraw(canvas);

            float centroX =
                    getWidth() / 2f;

            float centroY =
                    getHeight() / 2f;

            float raioMax =
                    Math.min(
                            getWidth(),
                            getHeight()
                    ) * 0.38f;

            paint.setStyle(
                    Paint.Style.FILL
            );

            paint.setColor(
                    Color.rgb(
                            255,
                            210,
                            0
                    )
            );

            paint.setShadowLayer(
                    ouvindoLocal
                            ? 45f
                            : 28f,
                    0f,
                    0f,
                    Color.rgb(
                            255,
                            180,
                            0
                    )
            );

            canvas.drawCircle(
                    centroX,
                    centroY,
                    raioMax * 0.19f,
                    paint
            );

            paint.clearShadowLayer();

            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setStrokeWidth(
                    ouvindoLocal
                            ? 4.5f
                            : 3.2f
            );

            paint.setColor(
                    Color.rgb(
                            255,
                            220,
                            20
                    )
            );

            for (int i = 0; i < 8; i++) {

                float raio =
                        raioMax
                                - (i * 10.5f);

                canvas.save();

                float direcao =
                        (i % 2 == 0)
                                ? 1f
                                : -1f;

                canvas.rotate(
                        rotacao * direcao,
                        centroX,
                        centroY
                );

                RectF oval =
                        new RectF(
                                centroX - raio,
                                centroY - raio,
                                centroX + raio,
                                centroY + raio
                        );

                canvas.drawArc(
                        oval,
                        0f,
                        285f,
                        false,
                        paint
                );

                canvas.drawArc(
                        oval,
                        300f,
                        35f,
                        false,
                        paint
                );

                canvas.restore();
            }

            paint.setStyle(
                    Paint.Style.FILL
            );

            paint.setColor(
                    Color.WHITE
            );

            paint.setShadowLayer(
                    ouvindoLocal
                            ? 35f
                            : 22f,
                    0f,
                    0f,
                    Color.YELLOW
            );

            canvas.drawCircle(
                    centroX,
                    centroY,
                    raioMax * 0.115f,
                    paint
            );

            paint.clearShadowLayer();

            paint.setColor(
                    Color.rgb(
                            255,
                            215,
                            0
                    )
            );

            canvas.drawCircle(
                    centroX,
                    centroY,
                    raioMax * 0.065f,
                    paint
            );
        }

        @Override
        protected void onDetachedFromWindow() {

            handler.removeCallbacks(
                    animacao
            );

            super.onDetachedFromWindow();
        }
    }

    @Override
    public void onBackPressed() {
        voltarSistema();
    }

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

            } catch (Exception ignored) {
            }

            tts = null;
        }

        super.onDestroy();
    }
}
