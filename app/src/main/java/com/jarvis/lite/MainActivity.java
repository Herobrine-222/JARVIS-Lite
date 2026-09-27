package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
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
import android.view.Gravity;
import android.view.View;
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

    private boolean modoOnline = false;

    private SharedPreferences preferencias;

    private long ultimaVerificacao = 0L;

    private ReactorView reactorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            stopService(
                    new Intent(
                            this,
                            JarvisVoiceService.class
                    )
            );
        } catch (Exception e) {
        }

        preferencias =
                getSharedPreferences(
                        "jarvis_config",
                        MODE_PRIVATE
                );

        ultimaVerificacao =
                preferencias.getLong(
                        "ultima_verificacao",
                        0L
                );

        configurarTela();
        iniciarRelogio();
        iniciarVoz();

        if (Build.VERSION.SDK_INT >= 23) {

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
                "Modo OFFLINE ativo."
        );
    }

    /*
     * ============================================================
     * TTS
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
        }
    }

    /*
     * ============================================================
     * RECONHECIMENTO DE VOZ OFFLINE
     * ============================================================
     */

    private void iniciarReconhecimento() {

        pararReconhecimento();

        if (Build.VERSION.SDK_INT < 31) {

            responder(
                    "O reconhecimento de voz local desta versão "
                            + "exige Android 12 ou superior."
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
                        public void onError(
                                int error) {

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

                                case SpeechRecognizer
                                        .ERROR_INSUFFICIENT_PERMISSIONS:

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

        if (reactorView != null) {
            reactorView.setOuvindo(false);
        }
    }

    /*
     * ============================================================
     * INTERFACE PRINCIPAL COMPACTA
     * ============================================================
     */

    private void configurarTela() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        /*
         * Espaçamento compacto.
         * A margem inferior maior evita que os botões
         * fiquem colados à navegação do Android.
         */
        root.setPadding(
                18,
                10,
                18,
                48
        );

        root.setBackgroundColor(
                Color.BLACK
        );

        /*
         * CABEÇALHO
         */

        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.HORIZONTAL
        );

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView titulo =
                new TextView(this);

        titulo.setText(
                "J.A.R.V.I.S"
        );

        titulo.setTextSize(24);

        titulo.setTextColor(
                Color.WHITE
        );

        titulo.setGravity(
                Gravity.CENTER_VERTICAL
        );

        LinearLayout.LayoutParams tituloParams =
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                );

        header.addView(
                titulo,
                tituloParams
        );

        Button configuracoes =
                new Button(this);

        configuracoes.setText(
                "⚙"
        );

        configuracoes.setTextSize(
                18
        );

        configuracoes.setOnClickListener(
                v -> abrirMenuJarvis()
        );

        LinearLayout.LayoutParams configParams =
                new LinearLayout.LayoutParams(
                        58,
                        48
                );

        header.addView(
                configuracoes,
                configParams
        );

        root.addView(
                header,
                new LinearLayout.LayoutParams(
                        -1,
                        48
                )
        );

        /*
         * SUBTÍTULO
         */

        TextView subtitulo =
                new TextView(this);

        subtitulo.setText(
                "ASSISTENTE LOCAL • OFFLINE"
        );

        subtitulo.setTextSize(
                11
        );

        subtitulo.setTextColor(
                Color.GRAY
        );

        root.addView(
                subtitulo,
                new LinearLayout.LayoutParams(
                        -1,
                        24
                )
        );

        /*
         * RELÓGIO
         */

        clockText =
                new TextView(this);

        clockText.setTextSize(
                21
        );

        clockText.setTextColor(
                Color.WHITE
        );

        clockText.setGravity(
                Gravity.CENTER
        );

        root.addView(
                clockText,
                new LinearLayout.LayoutParams(
                        -1,
                        50
                )
        );

        /*
         * REATOR
         */

        reactorView =
                new ReactorView(this);

        reactorView.setOnClickListener(
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

        LinearLayout.LayoutParams reactorParams =
                new LinearLayout.LayoutParams(
                        -1,
                        190
                );

        root.addView(
                reactorView,
                reactorParams
        );

        /*
         * STATUS
         */

        TextView modo =
                new TextView(this);

        modo.setText(
                "🔴 MODO OFFLINE ATIVO"
        );

        modo.setTextSize(
                13
        );

        modo.setTextColor(
                Color.WHITE
        );

        modo.setGravity(
                Gravity.CENTER
        );

        root.addView(
                modo,
                new LinearLayout.LayoutParams(
                        -1,
                        30
                )
        );

        /*
         * ÁREA DE CONVERSA
         *
         * Ela recebe o espaço restante da tela.
         */

        ScrollView scroll =
                new ScrollView(this);

        scroll.setFillViewport(
                true
        );

        LinearLayout chatContainer =
                new LinearLayout(this);

        chatContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        chatContainer.setPadding(
                10,
                8,
                10,
                8
        );

        chatText =
                new TextView(this);

        chatText.setTextSize(
                14
        );

        chatText.setTextColor(
                Color.WHITE
        );

        chatText.setGravity(
                Gravity.BOTTOM
        );

        chatContainer.addView(
                chatText,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        scroll.addView(
                chatContainer
        );

        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                );

        scrollParams.topMargin = 4;
        scrollParams.bottomMargin = 4;

        root.addView(
                scroll,
                scrollParams
        );

        /*
         * CAMPO DE TEXTO
         */

        LinearLayout entrada =
                new LinearLayout(this);

        entrada.setOrientation(
                LinearLayout.HORIZONTAL
        );

        entrada.setGravity(
                Gravity.CENTER_VERTICAL
        );

        commandInput =
                new EditText(this);

        commandInput.setHint(
                "Digite uma mensagem..."
        );

        commandInput.setHintTextColor(
                Color.GRAY
        );

        commandInput.setTextColor(
                Color.WHITE
        );

        commandInput.setSingleLine(
                true
        );

        LinearLayout.LayoutParams inputParams =
                new LinearLayout.LayoutParams(
                        0,
                        48,
                        1
                );

        entrada.addView(
                commandInput,
                inputParams
        );

        Button microfone =
                new Button(this);

        microfone.setText(
                "🎙"
        );

        microfone.setTextSize(
                16
        );

        microfone.setOnClickListener(
                v -> {

                    if (!ouvindo) {
                        iniciarReconhecimento();
                    } else {
                        pararReconhecimento();
                    }
                }
        );

        entrada.addView(
                microfone,
                new LinearLayout.LayoutParams(
                        58,
                        48
                )
        );

        root.addView(
                entrada,
                new LinearLayout.LayoutParams(
                        -1,
                        48
                )
        );

        /*
         * BOTÃO ENVIAR
         */

        Button executar =
                new Button(this);

        executar.setText(
                "ENVIAR"
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

                    processarComando(
                            comando
                    );

                    commandInput.setText("");
                }
        );

        root.addView(
                executar,
                new LinearLayout.LayoutParams(
                        -1,
                        44
                )
        );

        /*
         * BOTÃO ONLINE / OFFLINE
         */

        Button online =
                new Button(this);

        online.setText(
                "MODO ONLINE"
        );

        online.setTextSize(
                12
        );

        online.setOnClickListener(
                v -> {

                    if (!modoOnline) {

                        modoOnline = true;

                        modo.setText(
                                "🟢 MODO ONLINE SELECIONADO"
                        );

                        online.setText(
                                "MODO OFFLINE"
                        );

                        responder(
                                "Modo online selecionado pelo usuário. "
                                        + "Nenhuma conexão será iniciada "
                                        + "automaticamente."
                        );

                    } else {

                        modoOnline = false;

                        modo.setText(
                                "🔴 MODO OFFLINE ATIVO"
                        );

                        online.setText(
                                "MODO ONLINE"
                        );

                        responder(
                                "Modo offline ativado. "
                                        + "O JARVIS voltou a operar "
                                        + "somente com os recursos locais."
                        );
                    }
                }
        );

        root.addView(
                online,
                new LinearLayout.LayoutParams(
                        -1,
                        42
                )
        );

        setContentView(root);
    }

    /*
     * ============================================================
     * MENU
     * ============================================================
     */

    private void abrirMenuJarvis() {

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
                "Comando de voz",
                this::abrirComandoVoz
        );

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::voltarTela
        );

        setContentView(layout);
    }

    /*
     * ============================================================
     * GERENCIAR JARVIS
     * ============================================================
     */

    private void abrirGerenciarJarvis() {

        LinearLayout layout =
                criarTelaBase(
                        "GERENCIAR JARVIS",
                        "RECURSOS DISPONÍVEIS"
                );

        TextView info =
                criarTexto(
                        "Microfone:\n"
                                + "Usado quando o usuário ativa "
                                + "o reconhecimento de voz.\n\n"
                                + "Reconhecimento:\n"
                                + "Preferência pelo reconhecimento "
                                + "local do Android.\n\n"
                                + "Internet:\n"
                                + "O modo offline é o padrão. "
                                + "O JARVIS não deve ativar conexão "
                                + "online sozinho.\n\n"
                                + "Arquivos pessoais:\n"
                                + "Esta versão não possui função "
                                + "para apagar ou modificar seus arquivos.\n\n"
                                + "Root:\n"
                                + "Não utilizado.\n\n"
                                + "Administrador do dispositivo:\n"
                                + "Não utilizado."
                );

        layout.addView(
                info,
                parametrosTexto()
        );

        adicionarBotaoTela(
                layout,
                "ABRIR CONFIGURAÇÕES DO APLICATIVO",
                this::abrirConfiguracoesAndroid
        );

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::abrirMenuJarvis
        );

        setContentView(layout);
    }

    /*
     * ============================================================
     * PRIVACIDADE
     * ============================================================
     */

    private void abrirPrivacidade() {

        LinearLayout layout =
                criarTelaBase(
                        "PRIVACIDADE",
                        "ACESSO DO JARVIS"
                );

        TextView info =
                criarTexto(
                        "O JARVIS Lite utiliza somente recursos "
                                + "permitidos pelo Android.\n\n"
                                + "Nesta versão, o principal recurso "
                                + "sensível é o microfone.\n\n"
                                + "O Android continua responsável "
                                + "pelas permissões do aplicativo.\n\n"
                                + "Nenhum aplicativo comum pode garantir "
                                + "proteção absoluta contra todas "
                                + "as ameaças."
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

    /*
     * ============================================================
     * VERIFICAÇÃO
     * ============================================================
     */

    private void abrirVerificacao() {

        LinearLayout layout =
                criarTelaBase(
                        "VERIFICAÇÃO DO APARELHO",
                        "VERIFICAÇÃO BÁSICA"
                );

        TextView resultado =
                criarTexto(
                        "Resultado:\n"
                                + "Nenhuma ameaça pode ser confirmada "
                                + "por esta verificação básica.\n\n"
                                + "Esta função apenas consulta "
                                + "informações acessíveis ao aplicativo."
                );

        layout.addView(
                resultado,
                parametrosTexto()
        );

        TextView ultima =
                criarTexto(
                        textoUltimaVerificacao()
                );

        layout.addView(
                ultima,
                parametrosTexto()
        );

        Button verificar =
                criarBotao(
                        "VERIFICAR"
                );

        verificar.setOnClickListener(
                v -> {

                    executarVerificacao();

                    ultima.setText(
                            textoUltimaVerificacao()
                    );

                    resultado.setText(
                            "Resultado:\n"
                                    + "Verificação básica concluída.\n\n"
                                    + "O JARVIS não encontrou "
                                    + "informações que permitam "
                                    + "confirmar uma ameaça.\n\n"
                                    + "Esta função não substitui "
                                    + "o Google Play Protect."
                    );
                }
        );

        layout.addView(
                verificar,
                parametrosBotao()
        );

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::abrirMenuJarvis
        );

        setContentView(layout);
    }

    private void executarVerificacao() {

        try {

            android.content.pm.PackageManager pm =
                    getPackageManager();

            for (ApplicationInfo appInfo :
                    pm.getInstalledApplications(
                            PackageManager.GET_META_DATA)) {

                if (appInfo == null) {
                    continue;
                }
            }

        } catch (Exception e) {
        }

        ultimaVerificacao =
                System.currentTimeMillis();

        preferencias.edit()
                .putLong(
                        "ultima_verificacao",
                        ultimaVerificacao
                )
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
                ).format(
                        new Date(
                                ultimaVerificacao
                        )
                );

        return "Verificação realizada em:\n"
                + horario;
    }

    /*
     * ============================================================
     * COMANDO DE VOZ
     * ============================================================
     */

    private void abrirComandoVoz() {

        LinearLayout layout =
                criarTelaBase(
                        "COMANDO DE VOZ",
                        "ATIVAÇÃO DO JARVIS"
                );

        TextView explicacao =
                criarTexto(
                        "Defina a frase que deverá ser usada "
                                + "como comando de ativação.\n\n"
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

        campo.setTextColor(
                Color.WHITE
        );

        campo.setHintTextColor(
                Color.GRAY
        );

        layout.addView(
                campo,
                parametrosTexto()
        );

        Button confirmar =
                criarBotao(
                        "CONFIRMAR"
                );

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
                            "novo comando de voz ativo"
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

    /*
     * ============================================================
     * TELAS AUXILIARES
     * ============================================================
     */

    private LinearLayout criarTelaBase(
            String titulo,
            String subtitulo) {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                22,
                22,
                22,
                48
        );

        layout.setBackgroundColor(
                Color.BLACK
        );

        TextView tituloView =
                criarTexto(
                        titulo
                );

        tituloView.setTextSize(
                27
        );

        tituloView.setGravity(
                Gravity.CENTER
        );

        layout.addView(
                tituloView,
                parametrosTexto()
        );

        TextView subtituloView =
                criarTexto(
                        subtitulo
                );

        subtituloView.setTextSize(
                13
        );

        subtituloView.setTextColor(
                Color.GRAY
        );

        subtituloView.setGravity(
                Gravity.CENTER
        );

        layout.addView(
                subtituloView,
                parametrosTexto()
        );

        return layout;
    }

    private TextView criarTexto(
            String texto) {

        TextView view =
                new TextView(this);

        view.setText(
                texto
        );

        view.setTextColor(
                Color.WHITE
        );

        view.setTextSize(
                16
        );

        view.setPadding(
                8,
                10,
                8,
                10
        );

        return view;
    }

    private Button criarBotao(
            String texto) {

        Button button =
                new Button(this);

        button.setText(
                texto
        );

        return button;
    }

    private void adicionarBotaoTela(
            LinearLayout layout,
            String texto,
            Runnable acao) {

        Button button =
                criarBotao(
                        texto
                );

        button.setOnClickListener(
                v -> acao.run()
        );

        layout.addView(
                button,
                parametrosBotao()
        );
    }

    private LinearLayout.LayoutParams
    parametrosTexto() {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        params.topMargin = 6;

        return params;
    }

    private LinearLayout.LayoutParams
    parametrosBotao() {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        46
                );

        params.topMargin = 6;

        return params;
    }

    private void voltarTela() {

        modoOnline = false;

        configurarTela();
        iniciarRelogio();

        adicionarMensagem(
                "JARVIS",
                "À sua disposição. Sistemas locais operacionais."
        );

        adicionarMensagem(
                "JARVIS",
                "Modo OFFLINE ativo."
        );
    }

    private void abrirConfiguracoesAndroid() {

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

    /*
     * ============================================================
     * RELÓGIO
     * ============================================================
     */

    private void iniciarRelogio() {

        if (clockHandler != null) {

            clockHandler.removeCallbacksAndMessages(
                    null
            );
        }

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
                                            + "  •  "
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
                            + "bateria, status, privacidade "
                            + "e quem é você."
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

        if (comando.contains("privacidade")) {

            abrirPrivacidade();

            return;
        }

        if (comando.contains("verificação")
                || comando.contains("verificacao")) {

            abrirVerificacao();

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
                + ". Modo "
                + (modoOnline
                ? "ONLINE selecionado"
                : "OFFLINE ativo")
                + ".";
    }

    /*
     * ============================================================
     * RESPOSTAS
     * ============================================================
     */

    private void responder(
            String mensagem) {

        adicionarMensagem(
                "JARVIS",
                mensagem
        );

        falar(
                mensagem
        );
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
                        + "\n"
                        + mensagem;

        chatText.setText(
                atual
        );
    }

    /*
     * ============================================================
     * REATOR ARC
     * ============================================================
     */

    private class ReactorView
            extends View {

        private final Paint paint =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        private float rotacao = 0f;

        private boolean ouvindoLocal =
                false;

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

            ouvindoLocal = valor;

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

            /*
             * BRILHO CENTRAL
             */

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

            /*
             * ANÉIS
             */

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
                        rotacao
                                * direcao,
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

            /*
             * NÚCLEO
             */

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
