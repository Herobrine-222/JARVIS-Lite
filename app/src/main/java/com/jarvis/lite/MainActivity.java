package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
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
    private static final int REQUEST_AUDIO_REFERENCIA = 102;
    private static final int REQUEST_AUDIO_CONSENTIMENTO = 103;

    private TextView clockText;
    private LinearLayout chatContainer;
    private ScrollView chatScroll;
    private EditText commandInput;
    private TextView homeDateText;
    private TextView homeWeatherText;
    private boolean chatMode = false;

    private boolean verificacaoEncontrouSuspeito = false;
    private boolean verificacaoConcluidaComSucesso = false;
    private boolean verificacaoTentada = false;
    private String nomeAppSuspeito = "";
    private String detalheAppSuspeito = "";

    private Handler clockHandler;

    private TextToSpeech tts;
    private boolean ttsReady = false;

    /* Digitação = texto. Microfone = texto + voz. */
    private boolean responderComVozAtual = false;

    /* Áudios para referência/teste da futura voz personalizada. */
    private Uri audioReferenciaUri;
    private Uri audioConsentimentoUri;
    private MediaPlayer audioReferenciaPlayer;
    private MediaPlayer audioConsentimentoPlayer;

    private volatile boolean verificacaoEmAndamento = false;
    private Handler verificacaoHandler;
    private volatile int verificacaoRunToken = 0;

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
        verificacaoConcluidaComSucesso =
                preferencias.getBoolean("verificacao_concluida_com_sucesso", false);
        verificacaoTentada = preferencias.getBoolean(
                "verificacao_tentada",
                ultimaVerificacao > 0L
        );
        verificacaoEncontrouSuspeito =
                preferencias.getBoolean("verificacao_encontrou_suspeito", false);
        nomeAppSuspeito = preferencias.getString("verificacao_app_suspeito", "");
        detalheAppSuspeito = preferencias.getString("verificacao_detalhe_app_suspeito", "");

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

        String referenciaSalva = preferencias.getString("audio_referencia_uri", "");
        if (!referenciaSalva.isEmpty()) {
            try { audioReferenciaUri = Uri.parse(referenciaSalva); }
            catch (Exception ignored) { audioReferenciaUri = null; }
        }

        String consentimentoSalvo = preferencias.getString("audio_consentimento_uri", "");
        if (!consentimentoSalvo.isEmpty()) {
            try { audioConsentimentoUri = Uri.parse(consentimentoSalvo); }
            catch (Exception ignored) { audioConsentimentoUri = null; }
        }

        configurarTela();
        restaurarChatPrincipal();
        iniciarRelogio();
        iniciarVoz();

        if (microfoneAtivado
                && Build.VERSION.SDK_INT >= 23
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
                            ? "Modo ONLINE selecionado. A integração externa ainda não está conectada nesta versão."
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
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                Insets barras = insets.getInsets(
                        WindowInsets.Type.statusBars()
                                | WindowInsets.Type.navigationBars()
                );

                // O teclado já é tratado por SOFT_INPUT_ADJUST_RESIZE.
                // Somar os insets do teclado ao padding pode empurrar o campo
                // de mensagem para fora da tela quando o IME é aberto.
                v.setPadding(
                        dp(10),
                        dp(8) + barras.top,
                        dp(10),
                        dp(12) + barras.bottom
                );
                return insets;
            });
            root.requestApplyInsets();
        } else {
            root.setPadding(dp(10), dp(8), dp(10), dp(12));
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
        responderComVozAtual = false;

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

                            responderComVozAtual = true;
                            responder(mensagem);
                            responderComVozAtual = false;
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

                                responderComVozAtual = true;
                                responder(
                                        "Não consegui entender o comando."
                                );
                                responderComVozAtual = false;

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

                            responderComVozAtual = true;
                            processarComando(comando);
                            responderComVozAtual = false;
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
        // Não manter referências a uma conversa que saiu da tela.
        chatContainer = null;
        chatScroll = null;
        commandInput = null;

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
        root.addView(clockText, new LinearLayout.LayoutParams(-1, dp(72)));

        homeDateText = criarTexto("");
        homeDateText.setTextSize(16);
        homeDateText.setTextColor(Color.WHITE);
        homeDateText.setGravity(Gravity.CENTER);
        homeDateText.setIncludeFontPadding(true);
        homeDateText.setMaxLines(2);
        homeDateText.setHorizontallyScrolling(false);
        root.addView(homeDateText, new LinearLayout.LayoutParams(-1, dp(48)));

        // Clima: usa apenas informação configurada/obtida pelo próprio app.
        homeWeatherText = criarTexto("");
        homeWeatherText.setTextSize(14);
        homeWeatherText.setTextColor(Color.LTGRAY);
        homeWeatherText.setGravity(Gravity.CENTER);
        homeWeatherText.setMaxLines(2);
        homeWeatherText.setHorizontallyScrolling(false);
        homeWeatherText.setPadding(dp(8), dp(4), dp(8), dp(4));
        root.addView(homeWeatherText, new LinearLayout.LayoutParams(-1, dp(56)));

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
        reactorParams.topMargin = dp(4);
        reactorParams.bottomMargin = dp(8);

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

        root.addView(entrada, parametrosEntradaMensagem());

        TextView modo = criarTexto(
                modoOnline ? "● MODO ONLINE SELECIONADO" : "● MODO OFFLINE ATIVO"
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
                    new SimpleDateFormat("dd 'DE' MMMM", new Locale("pt", "BR"))
                            .format(new Date())
                            .toUpperCase(new Locale("pt", "BR"))
            );
        }

        if (homeWeatherText != null) {
            String cidade = preferencias.getString("cidade_clima", "");
            if (cidade.isEmpty()) {
                homeWeatherText.setText(
                        modoOnline
                                ? "☁  Clima não configurado"
                                : "☁  Clima indisponível no modo OFFLINE"
                );
            } else {
                homeWeatherText.setText(
                        "☁  Cidade configurada: " + cidade
                                + "  •  consulta de clima ainda não integrada"
                );
            }
        }
    }

    private LinearLayout criarEntradaMensagem(Runnable aoFocar) {
        LinearLayout entrada = new LinearLayout(this);
        entrada.setOrientation(LinearLayout.VERTICAL);
        entrada.setGravity(Gravity.START);
        entrada.setPadding(dp(12), dp(5), dp(8), dp(4));
        entrada.setMinimumHeight(dp(92));

        GradientDrawable campoFundo = new GradientDrawable();
        campoFundo.setColor(Color.rgb(3, 13, 25));
        campoFundo.setCornerRadius(dp(22));
        campoFundo.setStroke(dp(1), Color.rgb(0, 150, 255));
        entrada.setBackground(campoFundo);

        commandInput = new EditText(this);
        commandInput.setHint("Digite uma mensagem...");
        commandInput.setHintTextColor(Color.rgb(125, 145, 165));
        commandInput.setTextColor(Color.WHITE);
        commandInput.setTextSize(15);
        commandInput.setSingleLine(false);
        commandInput.setMinLines(1);
        commandInput.setMaxLines(5);
        commandInput.setHorizontallyScrolling(false);
        commandInput.setGravity(Gravity.TOP | Gravity.START);
        commandInput.setIncludeFontPadding(true);
        commandInput.setPadding(dp(4), dp(7), dp(4), dp(4));
        commandInput.setBackgroundColor(Color.TRANSPARENT);
        commandInput.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | android.text.InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
        );
        commandInput.setImeOptions(
                android.view.inputmethod.EditorInfo.IME_ACTION_SEND
                        | android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI
        );

        commandInput.setOnFocusChangeListener((v, focused) -> {
            if (focused && aoFocar != null && !chatMode) {
                aoFocar.run();
            }
        });

        // O botão de envio permanece disponível no balão. O ENTER dentro
        // de um campo multilinha deve poder inserir uma nova linha.
        commandInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND
                    || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                enviarTextoDigitado();
                return true;
            }
            return false;
        });

        entrada.addView(
                commandInput,
                new LinearLayout.LayoutParams(-1, -2)
        );

        LinearLayout acoes = new LinearLayout(this);
        acoes.setOrientation(LinearLayout.HORIZONTAL);
        acoes.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        acoes.setPadding(0, dp(2), 0, 0);

        ImageButton microfone = criarBotaoEntrada(
                R.drawable.ic_jarvis_mic,
                "Microfone"
        );
        microfone.setOnClickListener(v -> {
            if (!chatMode) {
                abrirChatIA();
            }
            if (!ouvindo) iniciarReconhecimento();
            else pararReconhecimento();
        });

        ImageButton enviar = criarBotaoEntrada(
                R.drawable.ic_jarvis_send,
                "Enviar mensagem"
        );
        enviar.setOnClickListener(v -> enviarTextoDigitado());

        LinearLayout.LayoutParams acaoParams =
                new LinearLayout.LayoutParams(dp(42), dp(42));
        acaoParams.leftMargin = dp(6);
        acoes.addView(microfone, new LinearLayout.LayoutParams(acaoParams));
        acoes.addView(enviar, new LinearLayout.LayoutParams(acaoParams));
        entrada.addView(
                acoes,
                new LinearLayout.LayoutParams(-1, dp(42))
        );

        return entrada;
    }

    private ImageButton criarBotaoEntrada(int recurso, String descricao) {
        ImageButton botao = new ImageButton(this);
        botao.setImageResource(recurso);
        botao.setColorFilter(Color.rgb(80, 205, 255));
        botao.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        botao.setPadding(dp(10), dp(10), dp(10), dp(10));
        botao.setContentDescription(descricao);

        GradientDrawable fundo = new GradientDrawable();
        fundo.setShape(GradientDrawable.OVAL);
        fundo.setColor(Color.rgb(5, 24, 40));
        fundo.setStroke(dp(1), Color.rgb(0, 115, 175));
        botao.setBackground(fundo);
        botao.setMinimumWidth(dp(42));
        botao.setMinimumHeight(dp(42));
        return botao;
    }

    private LinearLayout.LayoutParams parametrosEntradaMensagem() {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        // A tela raiz mantém uma margem segura; este recuo negativo deixa
        // o balão visualmente quase de ponta a ponta, com pequena margem.
        params.leftMargin = -dp(4);
        params.rightMargin = -dp(4);
        params.topMargin = dp(4);
        params.bottomMargin = dp(4);
        return params;
    }

    private int larguraMaximaBalaoMensagem() {
        int larguraTela = getResources().getDisplayMetrics().widthPixels;
        int larguraResponsiva = Math.round(larguraTela * 0.84f);
        return Math.max(dp(1), Math.min(dp(520), larguraResponsiva));
    }

    private boolean mensagemInvalida(String autor, String mensagem) {
        return mensagem == null || mensagem.trim().isEmpty();
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
        reactorView = miniReator;
        LinearLayout.LayoutParams miniParams =
                new LinearLayout.LayoutParams(dp(120), dp(120));
        miniParams.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(miniReator, miniParams);

        LinearLayout entrada = criarEntradaMensagem(null);
        root.addView(entrada, parametrosEntradaMensagem());

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

        telaAtual = TELA_GERENCIAR_VOZ;

        TelaRolavelLayout layout = criarTelaBase(
                "VOZ DO JARVIS",
                "ÁUDIO MP3 E VOZ PERSONALIZADA"
        );

        TextView info = criarTexto(
                "Guarde um áudio MP3 como referência e, opcionalmente, um áudio de consentimento para uma futura voz "
                        + "personalizada/clonada. Um MP3 isolado só pode ser reproduzido; ele não gera frases novas."
        );
        info.setTextSize(13);
        info.setTextColor(Color.LTGRAY);
        info.setIncludeFontPadding(true);
        layout.addView(info, parametrosTexto());

        TextView referencia = criarTexto(
                audioReferenciaUri == null
                        ? "ÁUDIO DE REFERÊNCIA: NÃO CONFIGURADO"
                        : "ÁUDIO DE REFERÊNCIA: CONFIGURADO"
        );
        referencia.setTextSize(13);
        referencia.setTextColor(audioReferenciaUri == null ? Color.GRAY : Color.rgb(80,205,255));
        layout.addView(referencia, parametrosTexto());

        adicionarBotaoTela(layout, "IMPORTAR ÁUDIO MP3 DE REFERÊNCIA", () -> selecionarAudio(false));
        adicionarBotaoTela(layout, "TESTAR ÁUDIO DE REFERÊNCIA", this::reproduzirReferencia);

        TextView consent = criarTexto(
                audioConsentimentoUri == null
                        ? "ÁUDIO DE CONSENTIMENTO: NÃO CONFIGURADO"
                        : "ÁUDIO DE CONSENTIMENTO: CONFIGURADO"
        );
        consent.setTextSize(13);
        consent.setTextColor(audioConsentimentoUri == null ? Color.GRAY : Color.rgb(80,205,255));
        layout.addView(consent, parametrosTexto());

        adicionarBotaoTela(layout, "IMPORTAR ÁUDIO DE CONSENTIMENTO", () -> selecionarAudio(true));
        adicionarBotaoTela(layout, "TESTAR ÁUDIO DE CONSENTIMENTO", this::reproduzirConsentimento);

        String vozId = preferencias.getString("voz_clonada_id", "");
        TextView cloneEstado = criarTexto(
                vozId.isEmpty()
                        ? "VOZ PERSONALIZADA/CLONADA: NÃO CONFIGURADA"
                        : "VOZ PERSONALIZADA/CLONADA: ID CONFIGURADO"
        );
        cloneEstado.setTextSize(13);
        cloneEstado.setTextColor(vozId.isEmpty() ? Color.GRAY : Color.rgb(80,205,255));
        layout.addView(cloneEstado, parametrosTexto());

        EditText vozIdInput = new EditText(this);
        vozIdInput.setHint("ID da voz personalizada (opcional)");
        vozIdInput.setHintTextColor(Color.GRAY);
        vozIdInput.setTextColor(Color.WHITE);
        vozIdInput.setSingleLine(true);
        vozIdInput.setText(vozId);
        vozIdInput.setTextSize(14);
        vozIdInput.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable vozIdBg = new GradientDrawable();
        vozIdBg.setColor(Color.rgb(3,13,25));
        vozIdBg.setCornerRadius(dp(16));
        vozIdBg.setStroke(dp(1), Color.rgb(0,150,255));
        vozIdInput.setBackground(vozIdBg);
        layout.addView(vozIdInput, new LinearLayout.LayoutParams(-1, dp(60)));

        TextView cloneInfo = criarTexto(
                "O campo do ID não finge criar uma clonagem local. Ele apenas deixa o JARVIS pronto para conectar uma voz "
                        + "personalizada obtida de um serviço autorizado."
        );
        cloneInfo.setTextSize(12);
        cloneInfo.setTextColor(Color.GRAY);
        cloneInfo.setIncludeFontPadding(true);
        layout.addView(cloneInfo, parametrosTexto());

        adicionarBotaoTela(layout, "SALVAR ID DA VOZ PERSONALIZADA", () -> {
            preferencias.edit()
                    .putString("voz_clonada_id", vozIdInput.getText().toString().trim())
                    .apply();
            adicionarMensagem("JARVIS", "Identificador da voz personalizado salvo.");
        });

        adicionarBotaoTela(layout, "TESTAR TTS DO ANDROID",
                () -> falar("À sua disposição. Sistemas locais operacionais."));
        adicionarBotaoTela(layout, "CONFIGURAR COMANDO DE ATIVAÇÃO", this::abrirComandoVoz);
        adicionarBotaoTela(layout, "VOLTAR", this::abrirGerenciarJarvis);

        setContentView(layout);
    }

    private void selecionarAudio(boolean consentimento) {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("audio/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, consentimento ? REQUEST_AUDIO_CONSENTIMENTO : REQUEST_AUDIO_REFERENCIA);
        } catch (Exception ignored) {
            adicionarMensagem("JARVIS", "Não foi possível abrir o seletor de áudio.");
        }
    }

    private void reproduzirReferencia() {
        if (audioReferenciaUri == null) {
            adicionarMensagem("JARVIS", "Nenhum áudio de referência foi configurado.");
            return;
        }
        reproduzirAudio(audioReferenciaUri, false);
    }

    private void reproduzirConsentimento() {
        if (audioConsentimentoUri == null) {
            adicionarMensagem("JARVIS", "Nenhum áudio de consentimento foi configurado.");
            return;
        }
        reproduzirAudio(audioConsentimentoUri, true);
    }

    private void reproduzirAudio(Uri uri, boolean consentimento) {
        try {
            MediaPlayer antigo = consentimento ? audioConsentimentoPlayer : audioReferenciaPlayer;
            if (antigo != null) {
                try { antigo.stop(); } catch (Exception ignored) {}
                try { antigo.release(); } catch (Exception ignored) {}
            }

            MediaPlayer player = MediaPlayer.create(this, uri);
            if (player == null) {
                adicionarMensagem("JARVIS", "Não consegui reproduzir o áudio selecionado.");
                return;
            }

            if (consentimento) audioConsentimentoPlayer = player;
            else audioReferenciaPlayer = player;

            player.setOnCompletionListener(mp -> {
                try { mp.release(); } catch (Exception ignored) {}
                if (consentimento) {
                    if (audioConsentimentoPlayer == mp) audioConsentimentoPlayer = null;
                } else if (audioReferenciaPlayer == mp) {
                    audioReferenciaPlayer = null;
                }
            });

            player.setOnErrorListener((mp, what, extra) -> {
                try { mp.release(); } catch (Exception ignored) {}
                if (consentimento) {
                    if (audioConsentimentoPlayer == mp) audioConsentimentoPlayer = null;
                } else if (audioReferenciaPlayer == mp) {
                    audioReferenciaPlayer = null;
                }
                return true;
            });

            player.start();
        } catch (Exception e) {
            adicionarMensagem("JARVIS", "Não foi possível reproduzir o áudio selecionado.");
        }
    }

    private String rotuloVoz(String nome) {
        String n = nome == null ? "" : nome.toLowerCase(Locale.getDefault());
        if (n.contains("male") || n.contains("mascul")) return "VOZ MASCULINA";
        if (n.contains("female") || n.contains("feminin")) return "VOZ FEMININA";
        return "VOZ TTS DISPONÍVEL";
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
        verificacaoEmAndamento = false;
        if (verificacaoHandler != null) {
            verificacaoHandler.removeCallbacksAndMessages(null);
        }

        TelaRolavelLayout layout = criarTelaBase(
                "VERIFICAÇÃO DO APARELHO",
                "ANÁLISE BÁSICA DO JARVIS"
        );

        TextView titulo = criarTexto("Verificação pronta");
        titulo.setTextSize(22);
        titulo.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        titulo.setTextColor(Color.WHITE);
        titulo.setGravity(Gravity.CENTER);
        titulo.setIncludeFontPadding(true);
        titulo.setPadding(dp(4), dp(3), dp(4), dp(3));
        layout.addView(titulo, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView subtitulo = criarTexto(
                "Analisaremos aplicativos instalados e informações de permissões acessíveis ao JARVIS. "
                        + "Esta é uma verificação básica; não é um antivírus e não examina todos os arquivos protegidos do Android."
        );
        subtitulo.setTextSize(13);
        subtitulo.setTextColor(Color.LTGRAY);
        subtitulo.setGravity(Gravity.CENTER);
        subtitulo.setIncludeFontPadding(true);
        subtitulo.setMaxLines(5);
        subtitulo.setHorizontallyScrolling(false);
        layout.addView(subtitulo, parametrosTexto());

        ReactorView scanner = new ReactorView(this);
        LinearLayout.LayoutParams scannerParams =
                new LinearLayout.LayoutParams(-1, dp(150));
        scannerParams.gravity = Gravity.CENTER_HORIZONTAL;
        layout.addView(scanner, scannerParams);

        android.widget.FrameLayout barra = new android.widget.FrameLayout(this);
        GradientDrawable barraBg = new GradientDrawable();
        barraBg.setColor(Color.rgb(4, 18, 30));
        barraBg.setCornerRadius(dp(10));
        barraBg.setStroke(dp(1), Color.rgb(0, 150, 255));
        barra.setBackground(barraBg);
        barra.setPadding(dp(2), dp(2), dp(2), dp(2));

        View preenchimento = new View(this);
        GradientDrawable fillBg = new GradientDrawable();
        fillBg.setColor(Color.rgb(0, 150, 255));
        fillBg.setCornerRadius(dp(8));
        preenchimento.setBackground(fillBg);
        android.widget.FrameLayout.LayoutParams fillParams =
                new android.widget.FrameLayout.LayoutParams(0, -1);
        fillParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        barra.addView(preenchimento, fillParams);

        LinearLayout.LayoutParams barraParams =
                new LinearLayout.LayoutParams(-1, dp(24));
        barraParams.setMargins(dp(10), dp(10), dp(10), dp(2));
        layout.addView(barra, barraParams);

        TextView porcentagem = criarTexto("0%");
        porcentagem.setTextSize(16);
        porcentagem.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        porcentagem.setTextColor(Color.rgb(80, 205, 255));
        porcentagem.setGravity(Gravity.CENTER);
        porcentagem.setIncludeFontPadding(true);
        layout.addView(porcentagem, new LinearLayout.LayoutParams(-1, dp(36)));

        TextView status1 = criarTexto("○  Consultando aplicativos acessíveis...");
        TextView status2 = criarTexto("○  Consultando permissões declaradas...");
        TextView status3 = criarTexto("○  Aguardando conclusão da análise...");
        status1.setTextSize(12);
        status2.setTextSize(12);
        status3.setTextSize(12);
        status1.setTextColor(Color.rgb(80, 205, 255));
        status2.setTextColor(Color.GRAY);
        status3.setTextColor(Color.GRAY);
        layout.addView(status1, parametrosTexto());
        layout.addView(status2, parametrosTexto());
        layout.addView(status3, parametrosTexto());

        TextView observacao = criarTexto(
                "O percentual é uma estimativa visual; o resultado só aparece após a consulta local terminar."
        );
        observacao.setTextSize(11);
        observacao.setTextColor(Color.GRAY);
        observacao.setGravity(Gravity.CENTER);
        observacao.setMaxLines(3);
        layout.addView(observacao, parametrosTexto());

        Button iniciar = criarBotao("INICIAR VERIFICAÇÃO");
        Button voltar = criarBotao("VOLTAR");

        iniciar.setOnClickListener(v -> {
            if (verificacaoEmAndamento) return;

            verificacaoEmAndamento = true;
            verificacaoTentada = true;
            preferencias.edit().putBoolean("verificacao_tentada", true).apply();
            final int token = ++verificacaoRunToken;
            // O último resultado concluído é preservado caso o usuário cancele
            // esta nova tentativa. A execução atual só o substitui ao terminar.
            iniciar.setVisibility(View.GONE);
            scanner.setOuvindo(true);
            verificacaoHandler = new Handler();

            final int[] progresso = {0};
            final boolean[] analiseConcluida = {false};

            // A inspeção começa junto com a animação. O indicador não chega
            // a 100% nem abre o resultado antes de a consulta real terminar.
            Thread tarefaAnalise = new Thread(() -> {
                try {
                    executarVerificacao(token);
                } finally {
                    runOnUiThread(() -> {
                        if (verificacaoEmAndamento
                                && token == verificacaoRunToken) {
                            analiseConcluida[0] = true;
                        }
                    });
                }
            }, "jarvis-verificacao-basica");
            tarefaAnalise.start();

            Runnable andamento = new Runnable() {
                @Override
                public void run() {
                    if (!verificacaoEmAndamento
                            || token != verificacaoRunToken) {
                        return;
                    }

                    if (analiseConcluida[0]) {
                        progresso[0] = Math.min(100, progresso[0] + 10);
                    } else {
                        // Estimativa visual limitada a 90% enquanto a
                        // consulta do PackageManager ainda está em andamento.
                        progresso[0] = Math.min(90, progresso[0] + 5);
                    }

                    int larguraInterna = Math.max(0, barra.getWidth() - dp(4));
                    int largura = Math.round(larguraInterna * (progresso[0] / 100.0f));
                    android.widget.FrameLayout.LayoutParams lp =
                            (android.widget.FrameLayout.LayoutParams)
                                    preenchimento.getLayoutParams();
                    lp.width = Math.max(0, largura);
                    preenchimento.setLayoutParams(lp);
                    porcentagem.setText(progresso[0] + "%");

                    if (progresso[0] >= 35) {
                        status2.setTextColor(Color.rgb(80, 205, 255));
                    }
                    if (progresso[0] >= 70) {
                        status3.setTextColor(Color.rgb(80, 205, 255));
                    }

                    if (analiseConcluida[0] && progresso[0] >= 100) {
                        if (verificacaoConcluidaComSucesso) {
                            status1.setText("✓  Aplicativos acessíveis consultados.");
                            status2.setText("✓  Permissões declaradas consultadas.");
                            status3.setText("✓  Análise básica concluída.");
                            status1.setTextColor(Color.rgb(70, 230, 150));
                            status2.setTextColor(Color.rgb(70, 230, 150));
                            status3.setTextColor(Color.rgb(70, 230, 150));
                        } else {
                            status1.setText("⚠  Não foi possível consultar os aplicativos.");
                            status2.setText("⚠  A análise não foi concluída.");
                            status3.setText("⚠  Nenhum resultado de segurança foi confirmado.");
                            status1.setTextColor(Color.rgb(255, 190, 70));
                            status2.setTextColor(Color.rgb(255, 190, 70));
                            status3.setTextColor(Color.rgb(255, 190, 70));
                        }

                        verificacaoEmAndamento = false;
                        scanner.setOuvindo(false);
                        if (verificacaoHandler != null) {
                            verificacaoHandler.removeCallbacksAndMessages(null);
                        }
                        abrirResultadoVerificacao();
                        return;
                    }

                    if (verificacaoHandler != null) {
                        verificacaoHandler.postDelayed(this, 85);
                    }
                }
            };
            verificacaoHandler.post(andamento);
        });

        voltar.setOnClickListener(v -> {
            if (verificacaoEmAndamento) {
                verificacaoEmAndamento = false;
                verificacaoRunToken++;
                scanner.setOuvindo(false);
                if (verificacaoHandler != null) {
                    verificacaoHandler.removeCallbacksAndMessages(null);
                }
            }
            abrirMenuJarvis();
        });

        layout.addView(iniciar, parametrosBotao());
        layout.addView(voltar, parametrosBotao());
        setContentView(layout);
    }

    private void abrirResultadoVerificacao() {
        boolean sucesso = verificacaoConcluidaComSucesso;
        boolean alerta = sucesso && verificacaoEncontrouSuspeito;

        LinearLayout layout = criarTelaBase(
                "RESULTADO DA ANÁLISE",
                sucesso ? "VERIFICAÇÃO BÁSICA DO JARVIS" : "ANÁLISE NÃO CONCLUÍDA"
        );

        int corIndicador = !sucesso
                ? Color.rgb(255, 190, 70)
                : alerta
                        ? Color.rgb(255, 90, 90)
                        : Color.rgb(55, 235, 165);
        String simboloIndicador = !sucesso ? "!" : alerta ? "!" : "✓";

        LinearLayout icone = criarPainelResultado(simboloIndicador, corIndicador);
        LinearLayout.LayoutParams iconeParams =
                new LinearLayout.LayoutParams(dp(190), dp(190));
        iconeParams.gravity = Gravity.CENTER_HORIZONTAL;
        iconeParams.topMargin = dp(8);
        iconeParams.bottomMargin = dp(4);
        layout.addView(icone, iconeParams);

        String textoTitulo;
        if (!sucesso) {
            textoTitulo = "Não foi possível concluir a análise básica.";
        } else if (alerta) {
            textoTitulo = "Foi encontrado um aplicativo que merece revisão.";
        } else {
            textoTitulo = "Nenhum aplicativo atendeu aos critérios simples de alerta.";
        }

        TextView titulo = criarTexto(textoTitulo);
        titulo.setTextSize(17);
        titulo.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        titulo.setIncludeFontPadding(true);
        titulo.setMaxLines(4);
        titulo.setHorizontallyScrolling(false);
        titulo.setGravity(Gravity.CENTER);
        titulo.setTextColor(Color.WHITE);
        layout.addView(titulo, parametrosTexto());

        if (!sucesso) {
            TextView detalheFalha = criarTexto(
                    "O Android ou o sistema não disponibilizou as informações necessárias. "
                            + "Tente novamente mais tarde. Nenhum resultado de segurança foi confirmado."
            );
            detalheFalha.setTextSize(13);
            detalheFalha.setTextColor(Color.rgb(255, 190, 70));
            detalheFalha.setGravity(Gravity.CENTER);
            detalheFalha.setMaxLines(6);
            layout.addView(detalheFalha, parametrosTexto());
        } else if (alerta) {
            LinearLayout app = criarPainelNeon();

            TextView simbolo = criarTexto("⚠");
            simbolo.setTextSize(24);
            simbolo.setTextColor(Color.rgb(255, 90, 90));
            simbolo.setGravity(Gravity.CENTER);

            LinearLayout dados = new LinearLayout(this);
            dados.setOrientation(LinearLayout.VERTICAL);

            TextView nome = criarTexto(
                    nomeAppSuspeito.isEmpty() ? "Aplicativo para revisão" : nomeAppSuspeito
            );
            nome.setTextSize(14);
            nome.setTextColor(Color.WHITE);
            nome.setMaxLines(3);

            TextView detalhe = criarTexto(
                    detalheAppSuspeito.isEmpty()
                            ? "Fonte de instalação desconhecida"
                            : detalheAppSuspeito
            );
            detalhe.setTextSize(11);
            detalhe.setTextColor(Color.LTGRAY);
            detalhe.setMaxLines(4);

            dados.addView(nome, new LinearLayout.LayoutParams(-1, -2));
            dados.addView(detalhe, new LinearLayout.LayoutParams(-1, -2));
            app.addView(simbolo, new LinearLayout.LayoutParams(dp(52), -1));
            app.addView(dados, new LinearLayout.LayoutParams(0, -2, 1));

            LinearLayout.LayoutParams appParams =
                    new LinearLayout.LayoutParams(-1, -2);
            appParams.setMargins(dp(4), dp(8), dp(4), dp(8));
            layout.addView(app, appParams);
        } else {
            TextView detalhe = criarTexto(
                    "Nenhum aplicativo correspondeu aos critérios simples usados nesta análise. "
                            + "Isso não garante que o aparelho esteja livre de ameaças."
            );
            detalhe.setTextSize(13);
            detalhe.setTextColor(Color.rgb(80, 205, 255));
            detalhe.setGravity(Gravity.CENTER);
            detalhe.setMaxLines(6);
            layout.addView(detalhe, parametrosTexto());
        }

        TextView aviso = criarTexto(
                "Esta é uma análise limitada às informações acessíveis ao aplicativo; não é um antivírus."
        );
        aviso.setTextSize(11);
        aviso.setTextColor(Color.GRAY);
        aviso.setGravity(Gravity.CENTER);
        aviso.setMaxLines(4);
        layout.addView(aviso, parametrosTexto());

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
        if (!verificacaoTentada && ultimaVerificacao == 0L) {
            return "VERIFICAÇÃO AINDA NÃO EXECUTADA";
        }
        if (!verificacaoConcluidaComSucesso) {
            return "A análise básica não foi concluída; não há resultado confirmado.";
        }
        return verificacaoEncontrouSuspeito
                ? "Um aplicativo correspondeu aos critérios simples de atenção."
                : "Nenhum aplicativo correspondeu aos critérios simples de atenção.";
    }

    private String listaAppsVerificados() {

        if (!verificacaoTentada && ultimaVerificacao == 0L) {
            return "Nenhuma verificação foi executada ainda.";
        }
        if (!verificacaoConcluidaComSucesso) {
            return "A última análise não foi concluída com sucesso. Não há uma lista confirmada desta execução.";
        }

        StringBuilder out = new StringBuilder("Apps acessíveis listados pelo Android (até 12):\n");

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

    private void executarVerificacao(int token) {
        boolean encontrouSuspeito = false;
        String nomeSuspeito = "";
        String detalheSuspeito = "";

        try {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(
                    PackageManager.GET_META_DATA
            );
            if (apps == null) {
                registrarFalhaVerificacao(token);
                return;
            }

            for (ApplicationInfo appInfo : apps) {
                // Se o usuário saiu da tela, esta execução não deve modificar
                // o resultado de uma eventual verificação mais recente.
                if (token != verificacaoRunToken || !verificacaoEmAndamento) {
                    return;
                }

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
                    // Uma fonte desconhecida, isoladamente, não é prova de risco.
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
                    // Alguns pacotes não disponibilizam detalhes de permissão.
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

                // Heurística conservadora: origem desconhecida mais várias
                // permissões sensíveis. Não é uma classificação de malware.
                if (installer.isEmpty() && sensiveis >= 4) {
                    encontrouSuspeito = true;
                    CharSequence label = pm.getApplicationLabel(appInfo);
                    nomeSuspeito = label == null
                            ? appInfo.packageName
                            : label.toString();
                    detalheSuspeito =
                            "Fonte desconhecida • várias permissões sensíveis declaradas";
                    break;
                }
            }
        } catch (Exception e) {
            registrarFalhaVerificacao(token);
            return;
        }

        // Verificação final antes de publicar resultado/persistir dados.
        if (token != verificacaoRunToken || !verificacaoEmAndamento) {
            return;
        }

        verificacaoEncontrouSuspeito = encontrouSuspeito;
        nomeAppSuspeito = nomeSuspeito;
        detalheAppSuspeito = detalheSuspeito;
        verificacaoConcluidaComSucesso = true;
        ultimaVerificacao = System.currentTimeMillis();

        preferencias.edit()
                .putLong("ultima_verificacao", ultimaVerificacao)
                .putBoolean("verificacao_concluida_com_sucesso", true)
                .putBoolean("verificacao_encontrou_suspeito", verificacaoEncontrouSuspeito)
                .putString("verificacao_app_suspeito", nomeAppSuspeito)
                .putString("verificacao_detalhe_app_suspeito", detalheAppSuspeito)
                .apply();
    }

    private void registrarFalhaVerificacao(int token) {
        if (token != verificacaoRunToken || !verificacaoEmAndamento) {
            return;
        }
        verificacaoConcluidaComSucesso = false;
        if (preferencias != null) {
            preferencias.edit()
                    .putBoolean("verificacao_concluida_com_sucesso", false)
                    .apply();
        }
    }

    private String textoUltimaVerificacao() {

        if (ultimaVerificacao == 0L) {
            return verificacaoTentada
                    ? "Nenhuma verificação foi concluída com sucesso ainda."
                    : "Verificação realizada: nunca.";
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

    private TelaRolavelLayout criarTelaBase(
            String titulo,
            String subtitulo) {

        chatMode = false;
        chatContainer = null;
        chatScroll = null;
        commandInput = null;

        TelaRolavelLayout layout = new TelaRolavelLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.BLACK);
        layout.setPadding(dp(10), dp(8), dp(10), dp(14));

        if (Build.VERSION.SDK_INT >= 30) {
            layout.setOnApplyWindowInsetsListener((v, insets) -> {
                Insets barras = insets.getInsets(
                        WindowInsets.Type.statusBars()
                                | WindowInsets.Type.navigationBars()
                );
                v.setPadding(
                        dp(10),
                        dp(8) + barras.top,
                        dp(10),
                        dp(14) + barras.bottom
                );
                return insets;
            });
            layout.requestApplyInsets();
        }

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageButton voltar = new ImageButton(this);
        voltar.setImageResource(R.drawable.ic_jarvis_back);
        voltar.setBackgroundColor(Color.TRANSPARENT);
        voltar.setColorFilter(Color.WHITE);
        voltar.setContentDescription("Voltar");
        voltar.setOnClickListener(v -> voltarSistema());
        header.addView(voltar, new LinearLayout.LayoutParams(dp(54), dp(54)));

        LinearLayout titulos = new LinearLayout(this);
        titulos.setOrientation(LinearLayout.VERTICAL);
        titulos.setGravity(Gravity.CENTER_VERTICAL);

        TextView tituloView = criarTexto(titulo);
        tituloView.setTextSize(
                titulo != null && titulo.length() > 28 ? 16
                        : titulo != null && titulo.length() > 21 ? 18 : 20
        );
        tituloView.setIncludeFontPadding(true);
        tituloView.setPadding(dp(2), dp(1), dp(2), dp(1));
        tituloView.setMaxLines(2);
        tituloView.setHorizontallyScrolling(false);
        tituloView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tituloView.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        titulos.addView(
                tituloView,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

        TextView subtituloView = criarTexto(subtitulo);
        subtituloView.setTextSize(10);
        subtituloView.setTextColor(Color.GRAY);
        subtituloView.setIncludeFontPadding(true);
        // criarTexto() adiciona padding vertical para textos comuns; aqui
        // removemos esse excesso para impedir o corte do subtítulo do cabeçalho.
        subtituloView.setPadding(dp(2), 0, dp(2), 0);
        subtituloView.setSingleLine(true);
        subtituloView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titulos.addView(
                subtituloView,
                new LinearLayout.LayoutParams(-1, dp(24))
        );

        header.addView(
                titulos,
                new LinearLayout.LayoutParams(0, dp(68), 1)
        );
        layout.addView(
                header,
                new LinearLayout.LayoutParams(-1, dp(74))
        );

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setNestedScrollingEnabled(true);

        LinearLayout conteudo = new LinearLayout(this);
        conteudo.setOrientation(LinearLayout.VERTICAL);
        conteudo.setPadding(0, dp(4), 0, dp(30));
        scroll.addView(conteudo, new ScrollView.LayoutParams(-1, -2));

        layout.setConteudoRolavel(conteudo);
        layout.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
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
                dp(9),
                dp(8),
                dp(9)
        );
        view.setIncludeFontPadding(true);
        view.setMaxLines(8);
        view.setHorizontallyScrolling(false);

        return view;
    }

    private Button criarBotao(String texto) {
        Button button = new Button(this);
        button.setText(texto);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(14), dp(6), dp(14), dp(6));
        button.setMinHeight(dp(54));
        button.setMinWidth(0);
        button.setSingleLine(false);
        button.setMaxLines(2);
        button.setHorizontallyScrolling(false);

        GradientDrawable fundo = new GradientDrawable();
        fundo.setColor(Color.rgb(28, 30, 35));
        fundo.setCornerRadius(dp(16));
        fundo.setStroke(dp(1), Color.rgb(60, 62, 68));
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
                new LinearLayout.LayoutParams(-1, dp(58));
        params.topMargin = dp(8);
        params.leftMargin = dp(4);
        params.rightMargin = dp(4);
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
                                    "dd 'DE' MMMM",
                                    new Locale("pt", "BR")
                            ).format(agora)
                                    .toUpperCase(new Locale("pt", "BR"))
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

    String valor = texto
            .trim()
            .toLowerCase(Locale.ROOT);

    if (valor.isEmpty() || valor.length() > 500) {
        return null;
    }

    /*
     * Permite perguntas naturais simples sem transformar
     * a calculadora em um interpretador de linguagem livre.
     */
    String[] prefixos = {
            "quanto é o resultado de ",
            "quanto e o resultado de ",
            "qual é o resultado de ",
            "qual e o resultado de ",
            "resultado de ",
            "quanto é ",
            "quanto e ",
            "calcule ",
            "calcular ",
            "qual é ",
            "qual e "
    };

    for (String prefixo : prefixos) {
        if (valor.startsWith(prefixo)) {
            valor = valor.substring(prefixo.length()).trim();
            break;
        }
    }

    valor = valor
            .replace("vezes", "*")
            .replace("multiplicado por", "*")
            .replace("dividido por", "/")
            .replace("mais", "+")
            .replace("menos", "-")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("=", "")
            .replace("?", "")
            .trim();

    if (valor.isEmpty()) {
        return null;
    }

    /*
     * Sem número e sem operador, não é matemática.
     */
    if (!valor.matches(".*[0-9].*")) {
        return null;
    }

    if (!valor.matches(".*[+\\-*/%x].*")) {
        return null;
    }

    try {

        JarvisMathEngine.Fraction resultado =
                JarvisMathEngine.calculate(valor);

        String representacao;

        try {
            /*
             * Quando a fração possui decimal finito,
             * mostramos o decimal exato. Ex.: 1/2 -> 0.5.
             */
            representacao =
                    resultado.toExactDecimal()
                            .replace('.', ',');
        } catch (ArithmeticException naoEhDecimalFinito) {
            /*
             * Caso como 2/3: mantemos a fração exata.
             */
            representacao = resultado.toString();
        }

        return
                "O resultado é "
                        + representacao
                        + ".";

    } catch (ArithmeticException e) {

        if (e.getMessage() != null
                && e.getMessage().toLowerCase(Locale.ROOT)
                .contains("zero")) {

            return "Não é possível dividir por zero.";
        }

        return
                "Não consegui calcular essa expressão com exatidão.";

    } catch (IllegalArgumentException e) {

        return null;

    } catch (Exception e) {

        return null;
    }
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

        responderComVozAtual = false;
        processarComando(comando);
        responderComVozAtual = false;
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

        if (chatScroll != null) {
            chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
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
        balao.setMaxLines(Integer.MAX_VALUE);
        balao.setHorizontallyScrolling(false);
        balao.setEllipsize(null);

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

        int larguraBalao = larguraMaximaBalaoMensagem();
        LinearLayout.LayoutParams np =
                new LinearLayout.LayoutParams(
                        larguraBalao,
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
                        larguraBalao,
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

        if (responderComVozAtual) {
            falar(mensagem);
        }
    }

    private void adicionarMensagem(
            String autor,
            String mensagem) {

        if (mensagemInvalida(autor, mensagem)) {
            return;
        }

        // Salvar e renderizar são operações separadas: no modo reator/tela
        // inicial pode não existir um chat visível, mas a conversa não deve sumir.
        if (preferencias != null) {
            preferencias.edit()
                    .putBoolean("chat_oculto", false)
                    .apply();
            salvarMensagem(autor, mensagem);
        }

        if (chatContainer == null) {
            return;
        }

        LinearLayout linha = new LinearLayout(this);
        linha.setOrientation(LinearLayout.VERTICAL);
        linha.setGravity("VOCÊ".equals(autor) ? Gravity.END : Gravity.START);
        linha.setPadding(0, dp(4), 0, dp(4));

        TextView nome = new TextView(this);
        nome.setText(autor == null ? "" : autor);
        nome.setTextSize(10);
        nome.setTextColor(Color.GRAY);
        nome.setMaxLines(2);
        nome.setGravity("VOCÊ".equals(autor) ? Gravity.END : Gravity.START);

        TextView balao = new TextView(this);
        balao.setText(mensagem);
        balao.setTextSize(14);
        balao.setTextColor(Color.WHITE);
        balao.setMaxLines(Integer.MAX_VALUE);
        balao.setHorizontallyScrolling(false);
        balao.setEllipsize(null);
        balao.setGravity(Gravity.CENTER_VERTICAL);
        balao.setPadding(dp(14), dp(10), dp(14), dp(10));

        GradientDrawable fundo = new GradientDrawable();
        fundo.setCornerRadius(dp(18));
        fundo.setColor(
                "VOCÊ".equals(autor)
                        ? Color.rgb(34, 34, 34)
                        : Color.rgb(18, 18, 18)
        );
        fundo.setStroke(dp(1), Color.rgb(65, 65, 65));
        balao.setBackground(fundo);

        int larguraBalao = larguraMaximaBalaoMensagem();
        LinearLayout.LayoutParams nomeParams =
                new LinearLayout.LayoutParams(larguraBalao, -2);
        nomeParams.gravity = "VOCÊ".equals(autor) ? Gravity.END : Gravity.START;
        nomeParams.setMargins(dp(4), 0, dp(4), dp(2));
        linha.addView(nome, nomeParams);

        LinearLayout.LayoutParams balaoParams =
                new LinearLayout.LayoutParams(larguraBalao, -2);
        balaoParams.gravity = nomeParams.gravity;
        balaoParams.setMargins(dp(4), 0, dp(4), 0);
        linha.addView(balao, balaoParams);

        chatContainer.addView(
                linha,
                new LinearLayout.LayoutParams(-1, -2)
        );

        AlphaAnimation aparecer = new AlphaAnimation(0f, 1f);
        aparecer.setDuration(220);
        TranslateAnimation subir = new TranslateAnimation(0, 0, dp(10), 0);
        subir.setDuration(220);
        AnimationSet entradaAnimada = new AnimationSet(true);
        entradaAnimada.addAnimation(aparecer);
        entradaAnimada.addAnimation(subir);
        linha.startAnimation(entradaAnimada);

        if (chatScroll != null) {
            chatScroll.postDelayed(
                    () -> chatScroll.fullScroll(View.FOCUS_DOWN),
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
                        15f,
                        285f,
                        false,
                        paint
                );

                canvas.drawArc(
                        oval,
                        320f,
                        25f,
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        if (requestCode != REQUEST_AUDIO_REFERENCIA && requestCode != REQUEST_AUDIO_CONSENTIMENTO) return;

        Uri uri = data.getData();
        try {
            int flags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            if (flags != 0) getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (Exception ignored) {
        }

        if (requestCode == REQUEST_AUDIO_REFERENCIA) {
            audioReferenciaUri = uri;
            preferencias.edit().putString("audio_referencia_uri", uri.toString()).apply();
            adicionarMensagem("JARVIS", "Áudio de referência configurado com sucesso.");
        } else {
            audioConsentimentoUri = uri;
            preferencias.edit().putString("audio_consentimento_uri", uri.toString()).apply();
            adicionarMensagem("JARVIS", "Áudio de consentimento configurado com sucesso.");
        }
    }

    @Override
    public void onBackPressed() {
        if (verificacaoEmAndamento) {
            verificacaoEmAndamento = false;
            verificacaoRunToken++;
            if (verificacaoHandler != null) verificacaoHandler.removeCallbacksAndMessages(null);
        }
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

        verificacaoEmAndamento = false;
        verificacaoRunToken++;
        if (verificacaoHandler != null) verificacaoHandler.removeCallbacksAndMessages(null);

        if (audioReferenciaPlayer != null) {
            try { audioReferenciaPlayer.stop(); } catch (Exception ignored) {}
            try { audioReferenciaPlayer.release(); } catch (Exception ignored) {}
            audioReferenciaPlayer = null;
        }

        if (audioConsentimentoPlayer != null) {
            try { audioConsentimentoPlayer.stop(); } catch (Exception ignored) {}
            try { audioConsentimentoPlayer.release(); } catch (Exception ignored) {}
            audioConsentimentoPlayer = null;
        }

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
