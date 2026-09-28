package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.view.animation.AlphaAnimation;
import android.view.animation.TranslateAnimation;
import android.view.animation.AnimationSet;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQUEST_AUDIO = 100;

    private TextView clockText;
    private LinearLayout chatContainer;
    private ScrollView chatScroll;
    private EditText commandInput;

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

    // Memória local persistente do JARVIS.
    private JarvisMemory memoria;
    private boolean memoriaAutomatica = true;

    // Tela atual usada pela navegação do botão/gesto Voltar do Android.
    private static final int TELA_PRINCIPAL = 0;
    private static final int TELA_MENU_JARVIS = 1;
    private static final int TELA_GERENCIAR_JARVIS = 2;
    private static final int TELA_PRIVACIDADE = 3;
    private static final int TELA_VERIFICACAO = 4;
    private static final int TELA_COMANDO_VOZ = 5;
    private static final int TELA_GERENCIAR_VOZ = 6;
    private static final int TELA_MEMORIA = 7;
    private int telaAtual = TELA_PRINCIPAL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
        }

        try {
            stopService(new Intent(this, JarvisVoiceService.class));
        } catch (Exception e) {
        }

        preferencias = getSharedPreferences("jarvis_config", MODE_PRIVATE);
        ultimaVerificacao = preferencias.getLong("ultima_verificacao", 0L);

        memoria = new JarvisMemory(this);
        memoriaAutomatica = preferencias.getBoolean("memoria_automatica", true);

        configurarTela();
        iniciarRelogio();
        iniciarVoz();

        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_AUDIO
            );
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

    private int dp(int valor) {
        return Math.round(
                valor * getResources().getDisplayMetrics().density
        );
    }

    private void configurarAreaSegura(View root) {
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                Insets barras = insets.getInsets(
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
            });

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
                                int resultado = tts.setLanguage(
                                        new Locale("pt", "BR")
                                );

                                ttsReady =
                                        resultado != TextToSpeech.LANG_MISSING_DATA
                                                && resultado != TextToSpeech.LANG_NOT_SUPPORTED;

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

    private void iniciarReconhecimento() {
        pararReconhecimento();

        if (Build.VERSION.SDK_INT < 31) {
            responder(
                    "O reconhecimento de voz local desta versão exige Android 12 ou superior."
            );
            return;
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            responder(
                    "A permissão do microfone ainda não foi concedida."
            );

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_AUDIO
            );

            return;
        }

        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            responder(
                    "O reconhecimento de voz offline não está disponível neste aparelho."
            );
            return;
        }

        try {
            speechRecognizer =
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(this);

            speechRecognizer.setRecognitionListener(
                    new RecognitionListener() {

                        @Override
                        public void onReadyForSpeech(Bundle params) {
                            ouvindo = true;
                            adicionarMensagem("JARVIS", "Estou ouvindo...");

                            if (reactorView != null) {
                                reactorView.setOuvindo(true);
                            }
                        }

                        @Override public void onBeginningOfSpeech() {}
                        @Override public void onRmsChanged(float rmsdB) {}
                        @Override public void onBufferReceived(byte[] buffer) {}

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
                                    mensagem = "Não consegui acessar o áudio.";
                                    break;

                                case SpeechRecognizer.ERROR_NO_MATCH:
                                    mensagem = "Não consegui entender o que foi dito.";
                                    break;

                                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                                    mensagem = "Não detectei nenhuma fala.";
                                    break;

                                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                                    mensagem = "O reconhecimento de voz estava ocupado.";
                                    break;

                                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                                    mensagem = "A permissão do microfone não está disponível.";
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
                        public void onResults(Bundle results) {
                            ouvindo = false;

                            if (reactorView != null) {
                                reactorView.setOuvindo(false);
                            }

                            ArrayList<String> resultados =
                                    results.getStringArrayList(
                                            SpeechRecognizer.RESULTS_RECOGNITION
                                    );

                            if (resultados == null || resultados.isEmpty()) {
                                responder("Não consegui entender o comando.");
                                liberarReconhecedor();
                                return;
                            }

                            String comando = resultados.get(0);

                            adicionarMensagem("VOCÊ", comando);

                            liberarReconhecedor();

                            processarComando(comando);
                        }

                        @Override public void onPartialResults(Bundle partialResults) {}
                        @Override public void onEvent(int eventType, Bundle params) {}
                    }
            );

            Intent intent =
                    new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

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

    private void configurarTela() {
        telaAtual = TELA_PRINCIPAL;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        configurarAreaSegura(root);

        // Nova interface: menu de três linhas no canto superior esquerdo.
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button menu = new Button(this);
        menu.setText("☰");
        menu.setTextSize(22);
        menu.setTextColor(Color.WHITE);
        menu.setOnClickListener(v -> abrirMenuJarvis());
        header.addView(menu, new LinearLayout.LayoutParams(dp(58), dp(48)));

        TextView titulo = new TextView(this);
        titulo.setText("J.A.R.V.I.S");
        titulo.setTextSize(24);
        titulo.setTextColor(Color.WHITE);
        titulo.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(titulo, new LinearLayout.LayoutParams(0, dp(48), 1));

        root.addView(header, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView subtitulo = new TextView(this);
        subtitulo.setText("ASSISTENTE LOCAL • OFFLINE");
        subtitulo.setTextSize(11);
        subtitulo.setTextColor(Color.GRAY);
        subtitulo.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(subtitulo, new LinearLayout.LayoutParams(-1, dp(24)));

        clockText = new TextView(this);
        clockText.setTextSize(21);
        clockText.setTextColor(Color.WHITE);
        clockText.setGravity(Gravity.CENTER);
        root.addView(clockText, new LinearLayout.LayoutParams(-1, dp(50)));

        reactorView = new ReactorView(this);
        reactorView.setOnClickListener(v -> {
            if (!ouvindo) {
                iniciarReconhecimento();
            } else {
                pararReconhecimento();
                adicionarMensagem("JARVIS", "Reconhecimento interrompido.");
            }
        });
        root.addView(reactorView, new LinearLayout.LayoutParams(-1, dp(190)));

        TextView modo = new TextView(this);
        modo.setText(modoOnline ? "● MODO ONLINE EM USO" : "● MODO OFFLINE EM USO");
        modo.setTextSize(13);
        modo.setTextColor(Color.WHITE);
        modo.setGravity(Gravity.CENTER);
        root.addView(modo, new LinearLayout.LayoutParams(-1, dp(30)));

        chatScroll = new ScrollView(this);
        chatScroll.setFillViewport(true);
        chatScroll.setClipToPadding(false);

        chatContainer = new LinearLayout(this);
        chatContainer.setOrientation(LinearLayout.VERTICAL);
        chatContainer.setPadding(dp(8), dp(8), dp(8), dp(12));
        chatScroll.addView(chatContainer);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1);
        scrollParams.topMargin = dp(4);
        scrollParams.bottomMargin = dp(4);
        root.addView(chatScroll, scrollParams);

        LinearLayout entrada = new LinearLayout(this);
        entrada.setOrientation(LinearLayout.HORIZONTAL);
        entrada.setGravity(Gravity.CENTER_VERTICAL);

        commandInput = new EditText(this);
        commandInput.setHint("Digite uma mensagem...");
        commandInput.setHintTextColor(Color.GRAY);
        commandInput.setTextColor(Color.WHITE);
        commandInput.setSingleLine(true);
        entrada.addView(commandInput, new LinearLayout.LayoutParams(0, dp(48), 1));

        Button microfone = new Button(this);
        microfone.setText("🎙");
        microfone.setTextSize(16);
        microfone.setOnClickListener(v -> {
            if (!ouvindo) iniciarReconhecimento();
            else pararReconhecimento();
        });
        entrada.addView(microfone, new LinearLayout.LayoutParams(dp(58), dp(48)));
        root.addView(entrada, new LinearLayout.LayoutParams(-1, dp(48)));

        Button executar = new Button(this);
        executar.setText("ENVIAR");
        executar.setOnClickListener(v -> {
            String comando = commandInput.getText().toString().trim();
            if (comando.isEmpty()) return;
            adicionarMensagem("VOCÊ", comando);
            processarComando(comando);
            commandInput.setText("");
        });
        root.addView(executar, new LinearLayout.LayoutParams(-1, dp(44)));

        setContentView(root);
    }

    private void abrirMenuJarvis() {
        telaAtual = TELA_MENU_JARVIS;
        LinearLayout layout =
                criarTelaBase("J.A.R.V.I.S", "CONFIGURAÇÕES");

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
                "🧠 MEMÓRIA DO JARVIS",
                this::abrirMemoriaJarvis
        );

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::voltarTela
        );

        setContentView(layout);
    }

    private void abrirGerenciarJarvis() {
        telaAtual = TELA_GERENCIAR_JARVIS;
        microfoneAtivado = preferencias.getBoolean("microfone_ativo", true);
        notificacoesAtivadas = preferencias.getBoolean("notificacoes_ativas", true);
        statusAtivado = preferencias.getBoolean("status_ativo", true);
        arquivosMidiaAtivados = preferencias.getBoolean("arquivos_midia_ativos", false);
        modoOnline = preferencias.getBoolean("modo_online", false);

        LinearLayout layout = criarTelaBase(
                "GERENCIAR JARVIS",
                "PERMISSÕES, RECURSOS E MODOS"
        );

        TextView aviso = criarTexto(
                "Aqui ficam as configurações do JARVIS. Desça a tela para ver todas as opções."
                        + " O modo online só entra em uso quando você o selecionar."
        );
        aviso.setTextSize(13);
        aviso.setTextColor(Color.LTGRAY);
        layout.addView(aviso, parametrosTexto());

        // Modo de operação: apenas um pode ficar EM USO por vez.
        LinearLayout modos = criarCard();
        LinearLayout textosModo = new LinearLayout(this);
        textosModo.setOrientation(LinearLayout.VERTICAL);
        textosModo.addView(criarTexto("🌐  MODO DE OPERAÇÃO"));
        TextView descModo = criarTexto(
                "OFFLINE é o modo inicial. ONLINE só é usado depois que você ativá-lo."
        );
        descModo.setTextSize(12);
        descModo.setTextColor(Color.GRAY);
        textosModo.addView(descModo);

        LinearLayout botoesModo = new LinearLayout(this);
        botoesModo.setOrientation(LinearLayout.HORIZONTAL);
        botoesModo.setGravity(Gravity.CENTER_VERTICAL);

        Button onlineButton = criarBotao(modoOnline ? "EM USO" : "ATIVAR");
        Button offlineButton = criarBotao(modoOnline ? "ATIVAR" : "EM USO");
        onlineButton.setTextSize(10);
        offlineButton.setTextSize(10);

        onlineButton.setOnClickListener(v -> {
            modoOnline = true;
            preferencias.edit().putBoolean("modo_online", true).apply();
            onlineButton.setText("EM USO");
            offlineButton.setText("ATIVAR");
            responder("Modo online está em uso.");
        });

        offlineButton.setOnClickListener(v -> {
            modoOnline = false;
            preferencias.edit().putBoolean("modo_online", false).apply();
            onlineButton.setText("ATIVAR");
            offlineButton.setText("EM USO");
            responder("Modo offline está em uso.");
        });

        botoesModo.addView(onlineButton, new LinearLayout.LayoutParams(0, dp(44), 1));
        botoesModo.addView(offlineButton, new LinearLayout.LayoutParams(0, dp(44), 1));
        textosModo.addView(botoesModo);
        modos.addView(textosModo, new LinearLayout.LayoutParams(-1, -2));
        layout.addView(modos, parametrosCard());

        adicionarControleInterno(layout, "🎙  MICROFONE",
                "Permite que o JARVIS use o microfone para reconhecimento de voz.",
                () -> microfoneAtivado,
                valor -> {
                    microfoneAtivado = valor;
                    preferencias.edit().putBoolean("microfone_ativo", valor).apply();
                    if (!valor) pararReconhecimento();
                    else if (!microfonePermitido() && Build.VERSION.SDK_INT >= 23) {
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
                    }
                });

        adicionarControleInterno(layout, "🔔  NOTIFICAÇÕES",
                "Controla as notificações usadas pelo JARVIS.",
                () -> notificacoesAtivadas,
                valor -> {
                    notificacoesAtivadas = valor;
                    preferencias.edit().putBoolean("notificacoes_ativas", valor).apply();
                });

        adicionarControleInterno(layout, "📱  STATUS DO APARELHO",
                "Permite consultar bateria, RAM, armazenamento e outros dados acessíveis.",
                () -> statusAtivado,
                valor -> {
                    statusAtivado = valor;
                    preferencias.edit().putBoolean("status_ativo", valor).apply();
                });

        adicionarControleInterno(layout, "📁  ARQUIVOS E MÍDIA",
                "Controle interno do recurso. Nenhuma permissão de armazenamento é concedida automaticamente.",
                () -> arquivosMidiaAtivados,
                valor -> {
                    arquivosMidiaAtivados = valor;
                    preferencias.edit().putBoolean("arquivos_midia_ativos", valor).apply();
                });

        adicionarControleIndisponivel(layout, "♿  ACESSIBILIDADE",
                "Indisponível: o JARVIS não utiliza Accessibility Service.");

        adicionarBotaoTela(layout, "🗣  GERENCIAR VOZ DO JARVIS", this::abrirGerenciarVoz);
        adicionarBotaoTela(layout, "VOLTAR", this::abrirMenuJarvis);

        setContentView(layout);
    }

    private interface EstadoControle { boolean get(); }
    private interface AlterarControle { void set(boolean valor); }

    private void adicionarControleInterno(LinearLayout layout, String titulo, String descricao,
                                          EstadoControle estado, AlterarControle alterar) {
        LinearLayout card = criarCard();
        LinearLayout textos = new LinearLayout(this);
        textos.setOrientation(LinearLayout.VERTICAL);

        TextView tituloView = criarTexto(titulo);
        tituloView.setTextSize(15);
        textos.addView(tituloView);

        TextView descView = criarTexto(descricao);
        descView.setTextSize(12);
        descView.setTextColor(Color.GRAY);
        textos.addView(descView);

        Button controle = criarBotao(estado.get() ? "ATIVADO" : "DESATIVADO");
        controle.setTextSize(10);
        controle.setOnClickListener(v -> {
            boolean novo = !estado.get();
            alterar.set(novo);
            controle.setText(novo ? "ATIVADO" : "DESATIVADO");
        });

        card.addView(textos, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(controle, new LinearLayout.LayoutParams(dp(105), dp(44)));
        layout.addView(card, parametrosCard());
    }

    private void adicionarControleIndisponivel(LinearLayout layout, String titulo, String descricao) {
        LinearLayout card = criarCard();
        LinearLayout textos = new LinearLayout(this);
        textos.setOrientation(LinearLayout.VERTICAL);
        textos.addView(criarTexto(titulo));
        TextView desc = criarTexto(descricao);
        desc.setTextSize(12);
        desc.setTextColor(Color.GRAY);
        textos.addView(desc);
        TextView estado = criarTexto("INDISPONÍVEL");
        estado.setTextSize(10);
        estado.setGravity(Gravity.CENTER);
        estado.setTextColor(Color.GRAY);
        card.addView(textos, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(estado, new LinearLayout.LayoutParams(dp(105), dp(44)));
        layout.addView(card, parametrosCard());
    }

    private void abrirGerenciarVoz() {
        telaAtual = TELA_GERENCIAR_VOZ;
        LinearLayout layout = criarTelaBase("VOZ DO JARVIS", "ESCOLHA UMA VOZ TTS DISPONÍVEL");

        TextView info = criarTexto("Escolha uma voz instalada no aparelho. A velocidade permanece normal e o tom será ajustado para um perfil mais grave.");
        info.setTextSize(13);
        info.setTextColor(Color.LTGRAY);
        layout.addView(info, parametrosTexto());

        LinearLayout lista = new LinearLayout(this);
        lista.setOrientation(LinearLayout.VERTICAL);

        if (tts == null || !ttsReady) {
            TextView indisponivel = criarTexto("O mecanismo de voz ainda está inicializando.");
            indisponivel.setTextColor(Color.GRAY);
            lista.addView(indisponivel, parametrosTexto());
        } else {
            List<Voice> vozes = new java.util.ArrayList<>();
            for (Voice voz : tts.getVoices()) {
                if (voz == null || voz.getLocale() == null) continue;
                Locale local = voz.getLocale();
                if ("pt".equalsIgnoreCase(local.getLanguage())
                        && "BR".equalsIgnoreCase(local.getCountry())) {
                    vozes.add(voz);
                }
            }

            String salva = preferencias.getString("voz_tts", "");
            if (vozes.isEmpty()) {
                TextView nenhuma = criarTexto("Nenhuma voz pt-BR foi encontrada pelo mecanismo TTS.");
                nenhuma.setTextColor(Color.GRAY);
                lista.addView(nenhuma, parametrosTexto());
            } else {
                for (Voice voz : vozes) {
                    String nome = voz.getName();
                    String rotulo = rotuloVoz(nome);
                    Button escolha = criarBotao(rotulo + "\n" + nome);
                    escolha.setTextSize(11);
                    escolha.setGravity(Gravity.CENTER);
                    escolha.setOnClickListener(v -> {
                        if (tts != null) {
                            tts.setVoice(voz);
                            tts.setSpeechRate(1.0f);
                            tts.setPitch(0.85f);
                            preferencias.edit().putString("voz_tts", voz.getName()).apply();
                            falar("Voz selecionada.");
                        }
                    });
                    lista.addView(escolha, parametrosBotao());
                }
            }
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(lista);
        layout.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        adicionarBotaoTela(layout, "TESTAR VOZ ATUAL", () -> falar("À sua disposição. Sistemas locais operacionais."));
        adicionarBotaoTela(layout, "VOLTAR", this::abrirGerenciarJarvis);
        setContentView(layout);
    }

    private String rotuloVoz(String nome) {
        String n = nome.toLowerCase(Locale.getDefault());
        if (n.contains("male") || n.contains("mascul")) return "🎙 VOZ MASCULINA";
        if (n.contains("female") || n.contains("feminin")) return "🎙 VOZ FEMININA";
        return "🎙 VOZ DISPONÍVEL";
    }

    private void aplicarVozSalva() {
        if (tts == null || !ttsReady) return;
        try {
            String nome = preferencias.getString("voz_tts", "");
            if (!nome.isEmpty()) {
                for (Voice voz : tts.getVoices()) {
                    if (nome.equals(voz.getName())) {
                        tts.setVoice(voz);
                        break;
                    }
                }
            }
            tts.setSpeechRate(1.0f);
            tts.setPitch(0.85f);
        } catch (Exception e) {
        }
    }

    private void adicionarControlePermissao(
            LinearLayout layout,
            String titulo,
            String descricao,
            boolean microfone) {

        LinearLayout card = criarCard();

        LinearLayout textos = new LinearLayout(this);
        textos.setOrientation(LinearLayout.VERTICAL);

        TextView tituloView = criarTexto(titulo);
        tituloView.setTextSize(15);
        tituloView.setPadding(0, 0, 0, dp(2));
        textos.addView(tituloView);

        TextView descView = criarTexto(descricao);
        descView.setTextSize(12);
        descView.setTextColor(Color.GRAY);
        descView.setPadding(0, 0, 0, 0);
        textos.addView(descView);

        Button controle = criarBotao(
                microfone && microfonePermitido()
                        ? "DESATIVAR"
                        : "ATIVAR"
        );
        controle.setTextSize(11);

        controle.setOnClickListener(v -> {
            if (!microfone) return;

            if (microfonePermitido()) {
                abrirConfiguracoesAndroid();
            } else if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_AUDIO
                );
            }

            atualizarStatusPermissao(controle);
        });

        card.addView(textos, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(controle, new LinearLayout.LayoutParams(dp(100), dp(44)));

        layout.addView(card, parametrosCard());
    }

    private void atualizarStatusPermissao(Button botao) {
        botao.setText(
                microfonePermitido()
                        ? "DESATIVAR"
                        : "ATIVAR"
        );
    }

    private boolean microfonePermitido() {
        return Build.VERSION.SDK_INT < 23
                || checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void adicionarControleInformacao(
            LinearLayout layout,
            String titulo,
            String descricao) {

        LinearLayout card = criarCard();

        LinearLayout textos = new LinearLayout(this);
        textos.setOrientation(LinearLayout.VERTICAL);

        TextView tituloView = criarTexto(titulo);
        tituloView.setTextSize(15);
        tituloView.setPadding(0, 0, 0, dp(2));
        textos.addView(tituloView);

        TextView descView = criarTexto(descricao);
        descView.setTextSize(12);
        descView.setTextColor(Color.GRAY);
        descView.setPadding(0, 0, 0, 0);
        textos.addView(descView);

        TextView estado = criarTexto(
                titulo.contains("INTERNET")
                        ? "BLOQUEADO"
                        : "DISPONÍVEL"
        );
        estado.setTextSize(10);
        estado.setGravity(Gravity.CENTER);
        estado.setTextColor(Color.LTGRAY);

        card.addView(textos, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(estado, new LinearLayout.LayoutParams(dp(100), dp(44)));

        layout.addView(card, parametrosCard());
    }

    private LinearLayout criarCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(10), dp(8), dp(10));

        GradientDrawable fundo = new GradientDrawable();
        fundo.setColor(Color.rgb(15, 15, 15));
        fundo.setCornerRadius(dp(14));
        fundo.setStroke(dp(1), Color.rgb(48, 48, 48));
        card.setBackground(fundo);

        return card;
    }

    private LinearLayout.LayoutParams parametrosCard() {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(7);
        return params;
    }

    private void abrirPrivacidade() {
        telaAtual = TELA_PRIVACIDADE;
        LinearLayout layout =
                criarTelaBase(
                        "PRIVACIDADE",
                        "ACESSO DO JARVIS"
                );

        TextView info = criarTexto(
                "O JARVIS Lite utiliza somente recursos permitidos pelo Android.\n\n"
                        + "Nesta versão, o principal recurso sensível é o microfone.\n\n"
                        + "O Android continua responsável pelas permissões do aplicativo.\n\n"
                        + "Nenhum aplicativo comum pode garantir proteção absoluta contra todas as ameaças."
        );

        layout.addView(info, parametrosTexto());

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
        LinearLayout layout =
                criarTelaBase(
                        "VERIFICAÇÃO DO APARELHO",
                        "VERIFICAÇÃO BÁSICA"
                );

        TextView resultado = criarTexto(
                "Resultado:\n"
                        + "Nenhuma ameaça pode ser confirmada por esta verificação básica.\n\n"
                        + "Esta função apenas consulta informações acessíveis ao aplicativo."
        );

        layout.addView(resultado, parametrosTexto());

        TextView ultima = criarTexto(textoUltimaVerificacao());
        layout.addView(ultima, parametrosTexto());

        Button verificar = criarBotao("VERIFICAR");

        verificar.setOnClickListener(v -> {
            executarVerificacao();

            ultima.setText(textoUltimaVerificacao());

            resultado.setText(
                    "Resultado:\n"
                            + "Verificação básica concluída.\n\n"
                            + "O JARVIS não encontrou informações que permitam confirmar uma ameaça.\n\n"
                            + "Esta função não substitui o Google Play Protect."
            );
        });

        layout.addView(verificar, parametrosBotao());

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::abrirMenuJarvis
        );

        setContentView(layout);
    }

    private void executarVerificacao() {
        try {
            PackageManager pm = getPackageManager();

            for (ApplicationInfo appInfo :
                    pm.getInstalledApplications(PackageManager.GET_META_DATA)) {

                if (appInfo == null) {
                    continue;
                }
            }
        } catch (Exception e) {
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
        telaAtual = TELA_COMANDO_VOZ;
        LinearLayout layout =
                criarTelaBase(
                        "COMANDO DE VOZ",
                        "ATIVAÇÃO DO JARVIS"
                );

        TextView explicacao = criarTexto(
                "Defina a frase que deverá ser usada como comando de ativação.\n\n"
                        + "Exemplo:\n"
                        + "JARVIS está aí?"
        );

        layout.addView(explicacao, parametrosTexto());

        EditText campo = new EditText(this);
        campo.setHint("Digite o novo comando...");

        campo.setText(
                preferencias.getString(
                        "comando_voz",
                        "JARVIS está aí?"
                )
        );

        campo.setTextColor(Color.WHITE);
        campo.setHintTextColor(Color.GRAY);

        layout.addView(campo, parametrosTexto());

        Button confirmar = criarBotao("CONFIRMAR");

        confirmar.setEnabled(
                campo.getText().toString().trim().length() >= 4
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
                                s.toString().trim().length() >= 4
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            android.text.Editable s) {
                    }
                }
        );

        confirmar.setOnClickListener(v -> {
            String novoComando =
                    campo.getText().toString().trim();

            if (novoComando.length() < 4) {
                return;
            }

            preferencias.edit()
                    .putString("comando_voz", novoComando)
                    .apply();

            adicionarMensagem(
                    "JARVIS",
                    "Novo comando de voz ativo."
            );

            falar("Novo comando de voz ativo.");
        });

        layout.addView(confirmar, parametrosBotao());

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

        TelaRolavelLayout layout = new TelaRolavelLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.BLACK);
        layout.setPadding(dp(18), dp(10), dp(18), dp(24));

        if (Build.VERSION.SDK_INT >= 30) {
            layout.setOnApplyWindowInsetsListener((v, insets) -> {
                Insets barras = insets.getInsets(
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
            });
            layout.requestApplyInsets();
        }

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button voltar = criarBotao("‹");
        voltar.setTextSize(26);
        voltar.setOnClickListener(v -> voltarSistema());
        header.addView(voltar, new LinearLayout.LayoutParams(dp(58), dp(48)));

        LinearLayout titulos = new LinearLayout(this);
        titulos.setOrientation(LinearLayout.VERTICAL);
        titulos.setGravity(Gravity.CENTER_VERTICAL);

        TextView tituloView = criarTexto(titulo);
        tituloView.setTextSize(22);
        tituloView.setPadding(0, 0, 0, 0);
        titulos.addView(tituloView);

        TextView subtituloView = criarTexto(subtitulo);
        subtituloView.setTextSize(11);
        subtituloView.setTextColor(Color.GRAY);
        subtituloView.setPadding(0, 0, 0, 0);
        titulos.addView(subtituloView);

        header.addView(titulos, new LinearLayout.LayoutParams(0, dp(52), 1));
        layout.addView(header, new LinearLayout.LayoutParams(-1, dp(58)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setNestedScrollingEnabled(true);

        LinearLayout conteudo = new LinearLayout(this);
        conteudo.setOrientation(LinearLayout.VERTICAL);
        conteudo.setPadding(0, dp(4), 0, dp(24));
        scroll.addView(conteudo, new ScrollView.LayoutParams(-1, -2));

        layout.setConteudoRolavel(conteudo);
        layout.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        layout.ativarRoteamento();

        return layout;
    }

    /**
     * Container usado pelas telas de configurações. Depois que a estrutura
     * é criada, os addView feitos pelos métodos existentes são encaminhados
     * para dentro do ScrollView. Assim todas as perguntas/opções podem ser
     * roladas até o final sem alterar a lógica das telas.
     */
    private static class TelaRolavelLayout extends LinearLayout {
        private LinearLayout conteudoRolavel;
        private boolean roteando = false;

        TelaRolavelLayout(android.content.Context context) {
            super(context);
        }

        void setConteudoRolavel(LinearLayout conteudo) {
            this.conteudoRolavel = conteudo;
        }

        void ativarRoteamento() {
            this.roteando = true;
        }

        @Override
        public void addView(View child, android.view.ViewGroup.LayoutParams params) {
            if (roteando && conteudoRolavel != null && child != conteudoRolavel) {
                conteudoRolavel.addView(child, params);
            } else {
                super.addView(child, params);
            }
        }

        @Override
        public void addView(View child) {
            if (roteando && conteudoRolavel != null && child != conteudoRolavel) {
                conteudoRolavel.addView(child);
            } else {
                super.addView(child);
            }
        }
    }

    private TextView criarTexto(String texto) {
        TextView view = new TextView(this);

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

    private Button criarBotao(String texto) {
        Button button = new Button(this);
        button.setText(texto);
        return button;
    }

    private void adicionarBotaoTela(
            LinearLayout layout,
            String texto,
            Runnable acao) {

        Button button = criarBotao(texto);

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
                new LinearLayout.LayoutParams(-1, -2);

        params.topMargin = dp(6);

        return params;
    }

    private LinearLayout.LayoutParams parametrosBotao() {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(46));

        params.topMargin = dp(6);

        return params;
    }

    private void voltarTela() {
        abrirTelaPrincipal();
    }

    private void abrirTelaPrincipal() {
        modoOnline = preferencias.getBoolean("modo_online", false);
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
                            "package:" + getPackageName()
                    )
            );

            startActivity(intent);
        } catch (Exception e) {
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
                            hora + "  •  " + data
                    );
                }

                clockHandler.postDelayed(this, 1000);
            }
        };

        clockHandler.post(atualizar);
    }

    private void processarComando(String comandoOriginal) {
        String comando =
                comandoOriginal
                        .toLowerCase(Locale.getDefault())
                        .trim();

        // Camada de interação conversacional: não altera memória nem estado do aparelho.
        String respostaInteracao = processarInteracaoConversacional(comandoOriginal);
        if (respostaInteracao != null) {
            responder(respostaInteracao);
            return;
        }

        if (processarComandoMemoria(comandoOriginal)) {
            return;
        }

        if (comando.contains("ajuda")
                || comando.contains("comandos")) {

            responder(
                    "Comandos disponíveis: hora, data, bateria, temperatura, status, memória, privacidade, verificação e quem é você."
            );

            return;
        }

        if (comando.contains("hora")) {
            String hora =
                    new SimpleDateFormat(
                            "HH:mm:ss",
                            Locale.getDefault()
                    ).format(new Date());

            responder("Agora são " + hora);
            return;
        }

        if (comando.contains("data")
                || comando.contains("dia")) {

            String data =
                    new SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                    ).format(new Date());

            responder("A data atual é " + data);
            return;
        }

        if (comando.contains("bateria")
                || comando.contains("carga")) {

            responder(obterBateria());
            return;
        }

        if (comando.contains("temperatura")
                || comando.contains("temperatura do celular")
                || comando.contains("temperatura do aparelho")) {

            responder(obterTemperatura());
            return;
        }

        if (comando.contains("status")
                || comando.contains("estado")) {
            if (!statusAtivado) {
                responder("O acesso ao status do aparelho está desativado no Gerenciar JARVIS.");
            } else {
                responder(obterStatus());
            }
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
                    "Eu sou o JARVIS Lite, um assistente local offline."
            );

            return;
        }

        if (comando.contains("jarvis")
                && (comando.contains("está aí")
                || comando.contains("esta ai"))) {

            responder(
                    "À sua disposição. Sistemas locais operacionais."
            );

            return;
        }

        if (memoriaAutomatica) {
            String nome = extrairNomeDoUsuario(comandoOriginal);
            if (nome != null && !nome.isEmpty()) {
                memoria.remember("O nome do usuário é " + nome + ".");
                responder("Entendido. Vou me lembrar do seu nome.");
                return;
            }
        }

        responder(
                "Não tenho uma função local para esse comando ainda."
        );
    }

    private String processarInteracaoConversacional(String entrada) {
        if (entrada == null) return null;

        String original = entrada.trim();
        if (original.isEmpty()) return null;

        String c = normalizar(original);

        // Saudações e respostas sociais básicas.
        if (c.equals("opa") || c.startsWith("opa ")) {
            return "Opa. Estou à sua disposição. Sistemas locais operacionais.";
        }

        if (contemAlgum(c, "bom dia", "boa tarde", "boa noite")) {
            return saudacaoPorHorario();
        }

        if (contemAlgum(c, "ola", "oi", "e ai", "fala jarvis", "fala ai")) {
            return saudacaoPorHorario();
        }

        if (contemAlgum(c, "como voce esta", "como voce ta", "como voce esta", "como voce ta", "tudo bem com voce", "tudo bem com você")) {
            return "Estou funcionando normalmente e pronto para ajudar. E com você, está tudo bem?";
        }

        if (contemAlgum(c, "como foi seu dia", "como foi o seu dia")) {
            return "Meu funcionamento não é como o dia de uma pessoa, mas até agora estou operando normalmente e pronto para ajudar.";
        }

        if (contemAlgum(c, "deus te abencoe", "deus te abençoe")) {
            return "Amém. Muito obrigado. Que você também tenha um ótimo dia.";
        }

        if (contemAlgum(c, "durma bem", "boa noite jarvis", "vou dormir", "hora de dormir")) {
            return "Obrigado. Desejo uma boa noite e um bom descanso. Quando precisar, estarei aqui.";
        }

        if (contemAlgum(c, "obrigado", "obrigada", "valeu", "vlw", "muito obrigado", "muito obrigada")) {
            return "Por nada. Estou à sua disposição.";
        }

        if (contemAlgum(c, "de nada", "por nada")) {
            return "Sempre à disposição.";
        }

        if (contemAlgum(c, "bom trabalho", "mandou bem", "ficou bom", "voce e bom", "voce e bom")) {
            return "Obrigado. Vou continuar trabalhando com precisão e sem interferir nas outras funções do sistema.";
        }

        // Identidade, origem e finalidade.
        if (contemAlgum(c, "quem e voce", "quem e voce", "quem e voce", "quem voce e", "qual seu nome", "qual e seu nome")) {
            return "Eu sou o JARVIS Lite, o assistente deste projeto. Minha camada local foi criada para executar funções permitidas pelo Android, responder por voz e texto e trabalhar de forma offline quando possível.";
        }

        if (contemAlgum(c, "quem te criou", "quem criou voce", "quem criou você", "quem e seu criador", "quem é seu criador", "quem te fez")) {
            return "Eu sou o JARVIS Lite deste projeto. A aplicação está sendo construída neste projeto com código Android, e minhas funções dependem do código e dos recursos que forem adicionados a ela.";
        }

        if (contemAlgum(c, "o que voce faz", "o que voce faz", "qual sua funcao", "qual e sua funcao", "para que voce serve", "para que voce serve")) {
            return "Posso responder perguntas e interações locais, consultar informações permitidas do aparelho, executar comandos locais implementados, falar por voz e usar a memória local quando uma função de memória for solicitada.";
        }

        if (contemAlgum(c, "como voce funciona", "como voce funciona", "como funciona por completo", "como voce funciona por completo", "como você funciona por completo")) {
            return "Eu trabalho por camadas: entrada de texto ou voz, interpretação da intenção, interações conversacionais, comandos locais do aparelho e memória local. A resposta passa pela função correspondente e é apresentada em texto e, quando disponível, por voz. O que ainda não estiver implementado não é inventado.";
        }

        // Memória: explicação, sem modificar o armazenamento.
        if (contemAlgum(c, "como sua memoria funciona", "como funciona sua memoria", "como funciona a memoria", "como funciona sua memória", "como sua memoria funciona")) {
            return "A memória atual do JARVIS Lite é local e fica no armazenamento privado do aplicativo por meio da classe JarvisMemory.java. Ela guarda apenas informações quando uma função de memória é acionada, sem precisar de internet. A camada definitiva de memória pode ser ampliada depois sem trocar essa base.";
        }

        if (contemAlgum(c, "voce tem memoria", "voce tem memoria", "voce se lembra", "voce se lembra", "voce lembra de mim", "voce lembra de mim")) {
            return "Tenho uma memória local implementada no projeto. Ela é separada da conversa e só deve registrar informações quando o recurso de memória for acionado.";
        }

        if (contemAlgum(c, "voce esquece", "voce esquece", "por que voce esquece", "por que voce esquece")) {
            return "A memória local não é a mesma coisa que o histórico da conversa. Se uma informação não estiver registrada na JarvisMemory.java, ela não deve ser tratada como memória permanente.";
        }

        if (contemAlgum(c, "voce esta online", "voce esta online", "voce esta offline", "voce esta offline", "esta online", "está online")) {
            return "O modo de operação é controlado nas configurações de Gerenciar JARVIS. O modo online não deve ser ativado automaticamente.";
        }

        // Conversa sobre o próprio funcionamento, sem fingir sentimentos ou capacidades.
        if (contemAlgum(c, "voce esta ai", "voce esta ai", "voce ta ai", "voce ta ai", "jarvis esta ai", "jarvis está aí")) {
            return "Estou aqui. Sistemas locais operacionais. Em que posso ajudar?";
        }

        if (contemAlgum(c, "esta funcionando", "está funcionando", "voce funciona", "voce funciona")) {
            return "Sim. Estou pronto para executar as funções que estão implementadas no JARVIS Lite.";
        }

        // Ajuda conversacional.
        if (contemAlgum(c, "o que posso falar", "o que eu posso falar", "me ajude", "me ajuda", "preciso de ajuda")) {
            return "Você pode falar comigo normalmente ou usar comandos como hora, data, bateria, temperatura, status, memória, privacidade, verificação e modo online ou offline, conforme as funções disponíveis no projeto.";
        }

        // Encerramento social não encerra a sessão de voz; isso é responsabilidade da camada de sessão.
        if (contemAlgum(c, "ate mais", "até mais", "tchau", "falou", "ate logo", "até logo")) {
            return "Até mais. Continuo disponível quando você precisar.";
        }

        if (contemAlgum(c, "boa sorte", "se cuida", "fique bem")) {
            return "Obrigado. Você também. Estou à disposição.";
        }

        // Perguntas sobre a data/hora são tratadas como interação quando não houver comando específico.
        if (c.equals("que horas sao") || c.equals("qual a hora")) {
            String hora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            return "Agora são " + hora + ".";
        }

        if (c.equals("que dia e hoje") || c.equals("qual a data de hoje")) {
            String data = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            return "Hoje é " + data + ".";
        }

        return null;
    }

    private String saudacaoPorHorario() {
        int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hora >= 5 && hora < 12) {
            return "Bom dia. À sua disposição. Sistemas locais operacionais.";
        }
        if (hora >= 12 && hora < 18) {
            return "Boa tarde. À sua disposição. Sistemas locais operacionais.";
        }
        return "Boa noite. À sua disposição. Sistemas locais operacionais.";
    }

    private boolean contemAlgum(String texto, String... termos) {
        for (String termo : termos) {
            String t = normalizar(termo);
            if (texto.equals(t) || texto.contains(" " + t + " ")
                    || texto.startsWith(t + " ") || texto.endsWith(" " + t)) {
                return true;
            }
        }
        return false;
    }

    private String normalizar(String texto) {
        String base = texto == null ? "" : texto.toLowerCase(Locale.getDefault()).trim();
        base = Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        base = base.replaceAll("[^a-z0-9]+", " ");
        return base.replaceAll("\\s+", " ").trim();
    }

    private boolean processarComandoMemoria(String comandoOriginal) {
        if (memoria == null || comandoOriginal == null) {
            return false;
        }

        String original = comandoOriginal.trim();
        String comando = normalizarMemoria(original);

        if (comando.startsWith("lembre que ")
                || comando.startsWith("lembre de ")
                || comando.startsWith("memorize que ")
                || comando.startsWith("guarde que ")
                || comando.startsWith("salve que ")) {

            String lembranca = original;
            String[] prefixos = {
                    "lembre que ", "lembre de ", "memorize que ",
                    "guarde que ", "salve que "
            };

            for (String prefixo : prefixos) {
                if (comando.startsWith(normalizarMemoria(prefixo))) {
                    lembranca = original.substring(prefixo.length()).trim();
                    break;
                }
            }

            if (lembranca.isEmpty()) {
                responder("Não há nada para guardar na memória.");
            } else {
                memoria.remember(lembranca);
                responder("Entendido. Informação guardada na minha memória local.");
            }
            return true;
        }

        if (comando.contains("o que voce lembra de mim")
                || comando.contains("o que voce lembra")
                || comando.contains("quais sao minhas memorias")
                || comando.contains("minhas memorias")
                || comando.equals("memoria")
                || comando.equals("memorias")
                || comando.contains("mostrar memorias")) {

            responder(memoria.formatForDisplay());
            return true;
        }

        if (comando.startsWith("esqueca ")
                || comando.startsWith("esquecer ")
                || comando.startsWith("apague a memoria sobre ")
                || comando.startsWith("apagar a memoria sobre ")) {

            String consulta = comando
                    .replaceFirst("^esqueca ", "")
                    .replaceFirst("^esquecer ", "")
                    .replaceFirst("^apague a memoria sobre ", "")
                    .replaceFirst("^apagar a memoria sobre ", "")
                    .trim();

            if (consulta.isEmpty()) {
                responder("Diga qual memória você quer que eu esqueça.");
            } else if (memoria.forget(consulta) > 0) {
                responder("Entendido. Apaguei da memória local o que correspondia a essa informação.");
            } else {
                responder("Não encontrei uma memória correspondente.");
            }
            return true;
        }

        if (comando.equals("limpar memoria")
                || comando.equals("apagar toda a memoria")
                || comando.equals("apague toda a memoria")
                || comando.equals("esqueca tudo")) {
            confirmarLimparMemoria();
            return true;
        }

        return false;
    }

    private String normalizarMemoria(String texto) {
        String normalizado = java.text.Normalizer.normalize(
                texto == null ? "" : texto,
                java.text.Normalizer.Form.NFD
        );
        normalizado = normalizado.replaceAll(
                "\\p{InCombiningDiacriticalMarks}+",
                ""
        );
        return normalizado.toLowerCase(Locale.ROOT).trim();
    }

    private String extrairNomeDoUsuario(String texto) {
        if (texto == null) return null;
        String normalizado = normalizarMemoria(texto);
        String prefixo = "meu nome e ";
        if (!normalizado.startsWith(prefixo)) return null;
        String nome = texto.trim().substring(prefixo.length()).trim();
        return nome.isEmpty() ? null : nome;
    }

    private void abrirMemoriaJarvis() {
        telaAtual = TELA_MEMORIA;

        LinearLayout layout = criarTelaBase(
                "MEMÓRIA DO JARVIS",
                "MEMÓRIA LOCAL PERSISTENTE"
        );

        TextView explicacao = criarTexto(
                "As memórias ficam armazenadas localmente no aplicativo. "
                        + "Encerrar uma sessão de voz não apaga estas informações.\n\n"
                        + "Memória automática: "
                        + (memoriaAutomatica ? "ATIVADA" : "DESATIVADA")
        );
        layout.addView(explicacao, parametrosTexto());

        TextView lista = criarTexto(memoria.formatForDisplay());
        lista.setTextSize(14);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(lista);
        layout.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        adicionarBotaoTela(
                layout,
                memoriaAutomatica ? "DESATIVAR MEMÓRIA AUTOMÁTICA" : "ATIVAR MEMÓRIA AUTOMÁTICA",
                () -> {
                    memoriaAutomatica = !memoriaAutomatica;
                    preferencias.edit()
                            .putBoolean("memoria_automatica", memoriaAutomatica)
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
                .setTitle("Apagar memória")
                .setMessage("Isso apagará todas as memórias locais do JARVIS. Esta ação não apaga o histórico de conversa.")
                .setNegativeButton("CANCELAR", null)
                .setPositiveButton("APAGAR", (dialog, which) -> {
                    memoria.clear();
                    responder("Memória local apagada. A sessão atual continua ativa.");
                    if (telaAtual == TELA_MEMORIA) {
                        abrirMemoriaJarvis();
                    }
                })
                .show();
    }

    private String obterBateria() {
        try {
            BatteryManager batteryManager =
                    (BatteryManager) getSystemService(BATTERY_SERVICE);

            if (batteryManager == null) {
                return "Não consegui obter o nível da bateria.";
            }

            int nivel =
                    batteryManager.getIntProperty(
                            BatteryManager.BATTERY_PROPERTY_CAPACITY
                    );

            return "A bateria está em " + nivel + "%.";

        } catch (Exception e) {
            return "Não consegui obter o nível da bateria.";
        }
    }

    private String obterTemperatura() {
        try {
            Intent bateria = registerReceiver(
                    null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            );

            if (bateria == null) {
                return "Não consegui obter a temperatura do aparelho.";
            }

            int temperatura = bateria.getIntExtra(
                    BatteryManager.EXTRA_TEMPERATURE,
                    Integer.MIN_VALUE
            );

            if (temperatura == Integer.MIN_VALUE) {
                return "A temperatura do aparelho não está disponível pelo Android neste momento.";
            }

            double celsius = temperatura / 10.0;
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
                ).format(new Date());

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

    private void responder(String mensagem) {
        adicionarMensagem("JARVIS", mensagem);
        falar(mensagem);
    }

    private void adicionarMensagem(
            String autor,
            String mensagem) {

        if (chatContainer == null) {
            return;
        }

        LinearLayout linha = new LinearLayout(this);
        linha.setOrientation(LinearLayout.VERTICAL);
        linha.setGravity(
                "VOCÊ".equals(autor)
                        ? Gravity.END
                        : Gravity.START
        );
        linha.setPadding(0, dp(4), 0, dp(4));

        TextView nome = new TextView(this);
        nome.setText(autor);
        nome.setTextSize(10);
        nome.setTextColor(Color.GRAY);
        nome.setGravity(
                "VOCÊ".equals(autor)
                        ? Gravity.END
                        : Gravity.START
        );

        TextView balao = new TextView(this);
        balao.setText(mensagem);
        balao.setTextSize(14);
        balao.setTextColor(Color.WHITE);
        balao.setGravity(Gravity.CENTER_VERTICAL);
        balao.setPadding(dp(14), dp(10), dp(14), dp(10));

        GradientDrawable fundo = new GradientDrawable();
        fundo.setCornerRadius(dp(18));
        fundo.setColor("VOCÊ".equals(autor)
                ? Color.rgb(34, 34, 34)
                : Color.rgb(18, 18, 18));
        fundo.setStroke(dp(1), Color.rgb(65, 65, 65));
        balao.setBackground(fundo);

        LinearLayout.LayoutParams nomeParams =
                new LinearLayout.LayoutParams(dp(260), -2);
        nomeParams.gravity =
                "VOCÊ".equals(autor) ? Gravity.END : Gravity.START;
        nomeParams.setMargins(dp(4), 0, dp(4), dp(2));
        linha.addView(nome, nomeParams);

        LinearLayout.LayoutParams balaoParams =
                new LinearLayout.LayoutParams(
                        dp(260),
                        -2
                );
        balaoParams.gravity =
                "VOCÊ".equals(autor) ? Gravity.END : Gravity.START;
        balaoParams.setMargins(dp(4), 0, dp(4), 0);
        linha.addView(balao, balaoParams);

        chatContainer.addView(
                linha,
                new LinearLayout.LayoutParams(-1, -2)
        );

        AlphaAnimation aparecer = new AlphaAnimation(0f, 1f);
        aparecer.setDuration(220);
        TranslateAnimation subir = new TranslateAnimation(
                0, 0, dp(10), 0
        );
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

    private class ReactorView extends View {

        private final Paint paint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private float rotacao = 0f;
        private boolean ouvindoLocal = false;

        private final Handler handler = new Handler();

        private final Runnable animacao = new Runnable() {
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
                handler.postDelayed(this, 16);
            }
        };

        public ReactorView(
                android.content.Context context) {

            super(context);

            setLayerType(
                    View.LAYER_TYPE_SOFTWARE,
                    null
            );

            handler.post(animacao);
        }

        public void setOuvindo(boolean valor) {
            ouvindoLocal = valor;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float centroX = getWidth() / 2f;
            float centroY = getHeight() / 2f;

            float raioMax =
                    Math.min(
                            getWidth(),
                            getHeight()
                    ) * 0.38f;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(255, 210, 0));

            paint.setShadowLayer(
                    ouvindoLocal ? 45f : 28f,
                    0f,
                    0f,
                    Color.rgb(255, 180, 0)
            );

            canvas.drawCircle(
                    centroX,
                    centroY,
                    raioMax * 0.19f,
                    paint
            );

            paint.clearShadowLayer();

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(
                    ouvindoLocal ? 4.5f : 3.2f
            );
            paint.setColor(Color.rgb(255, 220, 20));

            for (int i = 0; i < 8; i++) {
                float raio =
                        raioMax - (i * 10.5f);

                canvas.save();

                float direcao =
                        (i % 2 == 0) ? 1f : -1f;

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

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);

            paint.setShadowLayer(
                    ouvindoLocal ? 35f : 22f,
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

            paint.setColor(Color.rgb(255, 215, 0));

            canvas.drawCircle(
                    centroX,
                    centroY,
                    raioMax * 0.065f,
                    paint
            );
        }

        @Override
        protected void onDetachedFromWindow() {
            handler.removeCallbacks(animacao);
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
            clockHandler.removeCallbacksAndMessages(null);
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
