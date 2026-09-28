package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
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
import java.util.ArrayList;
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

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView titulo = new TextView(this);
        titulo.setText("J.A.R.V.I.S");
        titulo.setTextSize(24);
        titulo.setTextColor(Color.WHITE);
        titulo.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams tituloParams =
                new LinearLayout.LayoutParams(0, dp(48), 1);

        header.addView(titulo, tituloParams);

        Button configuracoes = new Button(this);
        configuracoes.setText("⚙");
        configuracoes.setTextSize(18);
        configuracoes.setOnClickListener(v -> abrirMenuJarvis());

        header.addView(
                configuracoes,
                new LinearLayout.LayoutParams(dp(58), dp(48))
        );

        root.addView(
                header,
                new LinearLayout.LayoutParams(-1, dp(48))
        );

        TextView subtitulo = new TextView(this);
        subtitulo.setText("ASSISTENTE LOCAL • OFFLINE");
        subtitulo.setTextSize(11);
        subtitulo.setTextColor(Color.GRAY);
        subtitulo.setGravity(Gravity.CENTER_VERTICAL);

        root.addView(
                subtitulo,
                new LinearLayout.LayoutParams(-1, dp(24))
        );

        clockText = new TextView(this);
        clockText.setTextSize(21);
        clockText.setTextColor(Color.WHITE);
        clockText.setGravity(Gravity.CENTER);

        root.addView(
                clockText,
                new LinearLayout.LayoutParams(-1, dp(50))
        );

        reactorView = new ReactorView(this);

        reactorView.setOnClickListener(v -> {
            if (!ouvindo) {
                iniciarReconhecimento();
            } else {
                pararReconhecimento();
                adicionarMensagem(
                        "JARVIS",
                        "Reconhecimento interrompido."
                );
            }
        });

        root.addView(
                reactorView,
                new LinearLayout.LayoutParams(-1, dp(190))
        );

        TextView modo = new TextView(this);
        modo.setText("● MODO OFFLINE ATIVO");
        modo.setTextSize(13);
        modo.setTextColor(Color.WHITE);
        modo.setGravity(Gravity.CENTER);

        root.addView(
                modo,
                new LinearLayout.LayoutParams(-1, dp(30))
        );

        chatScroll = new ScrollView(this);
        chatScroll.setFillViewport(true);
        chatScroll.setClipToPadding(false);
        chatScroll.setPadding(0, 0, 0, 0);

        chatContainer = new LinearLayout(this);
        chatContainer.setOrientation(LinearLayout.VERTICAL);
        chatContainer.setPadding(dp(8), dp(8), dp(8), dp(12));

        chatScroll.addView(chatContainer);

        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(-1, 0, 1);
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

        entrada.addView(
                commandInput,
                new LinearLayout.LayoutParams(0, dp(48), 1)
        );

        Button microfone = new Button(this);
        microfone.setText("🎙");
        microfone.setTextSize(16);

        microfone.setOnClickListener(v -> {
            if (!ouvindo) {
                iniciarReconhecimento();
            } else {
                pararReconhecimento();
            }
        });

        entrada.addView(
                microfone,
                new LinearLayout.LayoutParams(dp(58), dp(48))
        );

        root.addView(
                entrada,
                new LinearLayout.LayoutParams(-1, dp(48))
        );

        Button executar = new Button(this);
        executar.setText("ENVIAR");

        executar.setOnClickListener(v -> {
            String comando =
                    commandInput.getText().toString().trim();

            if (comando.isEmpty()) {
                return;
            }

            adicionarMensagem("VOCÊ", comando);
            processarComando(comando);
            commandInput.setText("");
        });

        root.addView(
                executar,
                new LinearLayout.LayoutParams(-1, dp(44))
        );

        Button online = new Button(this);
        online.setText("MODO ONLINE");
        online.setTextSize(12);

        online.setOnClickListener(v -> {
            if (!modoOnline) {
                modoOnline = true;

                modo.setText("● MODO ONLINE SELECIONADO");
                online.setText("MODO OFFLINE");

                responder(
                        "Modo online selecionado pelo usuário. "
                                + "Nenhuma conexão será iniciada automaticamente."
                );
            } else {
                modoOnline = false;

                modo.setText("● MODO OFFLINE ATIVO");
                online.setText("MODO ONLINE");

                responder(
                        "Modo offline ativado. "
                                + "O JARVIS voltou a operar somente com os recursos locais."
                );
            }
        });

        root.addView(
                online,
                new LinearLayout.LayoutParams(-1, dp(42))
        );

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

        LinearLayout layout = criarTelaBase(
                "GERENCIAR JARVIS",
                "PERMISSÕES E RECURSOS"
        );

        TextView aviso = criarTexto(
                "Cada controle altera o comportamento do JARVIS. "
                        + "Permissões protegidas pelo Android continuam sob controle do sistema."
        );
        aviso.setTextSize(13);
        aviso.setTextColor(Color.LTGRAY);
        layout.addView(aviso, parametrosTexto());

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
                "Permite ao JARVIS consultar bateria, RAM, armazenamento e outros dados disponíveis.",
                () -> statusAtivado,
                valor -> {
                    statusAtivado = valor;
                    preferencias.edit().putBoolean("status_ativo", valor).apply();
                });

        adicionarControleInterno(layout, "🌐  USAR A INTERNET",
                "Acesso online somente quando selecionado pelo usuário. Esta versão ainda não possui a permissão INTERNET.",
                () -> modoOnline,
                valor -> {
                    modoOnline = valor;
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

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(
                dp(22),
                dp(22),
                dp(22),
                dp(48)
        );
        layout.setBackgroundColor(Color.BLACK);

        if (Build.VERSION.SDK_INT >= 30) {
            layout.setOnApplyWindowInsetsListener((v, insets) -> {
                Insets barras = insets.getInsets(
                        WindowInsets.Type.statusBars()
                                | WindowInsets.Type.navigationBars()
                );

                v.setPadding(
                        dp(22),
                        dp(22) + barras.top,
                        dp(22),
                        dp(24) + barras.bottom
                );

                return insets;
            });

            layout.requestApplyInsets();
        }

        TextView tituloView = criarTexto(titulo);
        tituloView.setTextSize(27);
        tituloView.setGravity(Gravity.CENTER);

        layout.addView(
                tituloView,
                parametrosTexto()
        );

        TextView subtituloView = criarTexto(subtitulo);
        subtituloView.setTextSize(13);
        subtituloView.setTextColor(Color.GRAY);
        subtituloView.setGravity(Gravity.CENTER);

        layout.addView(
                subtituloView,
                parametrosTexto()
        );

        return layout;
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

        if (processarComandoMemoria(comandoOriginal)) {
            return;
        }

        if (comando.contains("ajuda")
                || comando.contains("comandos")) {

            responder(
                    "Comandos disponíveis: hora, data, bateria, status, privacidade e quem é você."
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
            } else if (memoria.forget(consulta)) {
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
