package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    // ============================================================
    // CONSTANTES
    // ============================================================

    private static final int REQUEST_AUDIO = 100;

    // ============================================================
    // COMPONENTES DA TELA
    // ============================================================

    private TextView clockText;
    private LinearLayout chatContainer;
    private ScrollView chatScroll;
    private EditText commandInput;

    private View reactorArea;
    private ReactorView reactorView;

    // ============================================================
    // VOZ
    // ============================================================

    private Handler clockHandler;

    private TextToSpeech tts;
    private boolean ttsReady = false;

    private SpeechRecognizer speechRecognizer;
    private boolean ouvindo = false;

    // ============================================================
    // CONFIGURAÇÕES
    // ============================================================

    private boolean modoOnline = false;
    private boolean microfoneAtivado = true;
    private boolean notificacoesAtivadas = true;
    private boolean statusAtivado = true;
    private boolean arquivosMidiaAtivados = false;

    private SharedPreferences preferencias;

    private long ultimaVerificacao = 0L;

    // ============================================================
    // HISTÓRICO
    // ============================================================

    private final ArrayList<MensagemHistorico> historico =
            new ArrayList<>();

    private boolean telaPrincipalLimpa = false;

    // ============================================================
    // CLASSE DO HISTÓRICO
    // ============================================================

    private static class MensagemHistorico {

        String autor;
        String texto;
        long horario;

        MensagemHistorico(
                String autor,
                String texto,
                long horario
        ) {
            this.autor = autor;
            this.texto = texto;
            this.horario = horario;
        }
    }

    // ============================================================
    // CICLO DE VIDA
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        // A tela principal não mantém o serviço de escuta aberto.
        try {
            stopService(
                    new Intent(
                            this,
                            JarvisVoiceService.class
                    )
            );
        } catch (Exception ignored) {
        }

        preferencias = getSharedPreferences(
                "jarvis_config",
                MODE_PRIVATE
        );

        ultimaVerificacao = preferencias.getLong(
                "ultima_verificacao",
                0L
        );

        // SEGURANÇA:
        // Toda vez que o aplicativo abre, começa offline.
        modoOnline = false;

        carregarConfiguracoes();
        carregarHistorico();

        telaPrincipalLimpa = preferencias.getBoolean(
                "tela_principal_limpa",
                false
        );

        configurarTela();
        iniciarRelogio();
        iniciarVoz();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(
                    Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.RECORD_AUDIO
                        },
                        REQUEST_AUDIO
                );
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (clockHandler != null) {
            clockHandler.removeCallbacksAndMessages(null);
        }

        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
            } catch (Exception ignored) {
            }

            speechRecognizer = null;
        }

        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {
            }

            tts = null;
        }
    }

    // ============================================================
    // CONFIGURAÇÕES
    // ============================================================

    private void carregarConfiguracoes() {

        microfoneAtivado = preferencias.getBoolean(
                "microfone",
                true
        );

        notificacoesAtivadas = preferencias.getBoolean(
                "notificacoes",
                true
        );

        statusAtivado = preferencias.getBoolean(
                "status",
                true
        );

        arquivosMidiaAtivados = preferencias.getBoolean(
                "arquivos_midia",
                false
        );
    }

    private void salvarConfiguracoes() {

        preferencias.edit()
                .putBoolean("microfone", microfoneAtivado)
                .putBoolean(
                        "notificacoes",
                        notificacoesAtivadas
                )
                .putBoolean("status", statusAtivado)
                .putBoolean(
                        "arquivos_midia",
                        arquivosMidiaAtivados
                )
                .apply();
    }

    // ============================================================
    // TAMANHO EM DP
    // ============================================================

    private int dp(int valor) {
        return (int) (
                valor *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    // ============================================================
    // ÁREA SEGURA / TECLADO
    // ============================================================

    private void configurarAreaSegura(View root) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            root.setOnApplyWindowInsetsListener(
                    (view, insets) -> {

                        android.graphics.Insets barras =
                                insets.getInsets(
                                        WindowInsets.Type.systemBars()
                                );

                        android.graphics.Insets teclado =
                                insets.getInsets(
                                        WindowInsets.Type.ime()
                                );

                        int inferior = Math.max(
                                barras.bottom,
                                teclado.bottom
                        );

                        view.setPadding(
                                view.getPaddingLeft(),
                                view.getPaddingTop(),
                                view.getPaddingRight(),
                                dp(24) + inferior
                        );

                        return insets;
                    }
            );

            root.requestApplyInsets();

        } else {

            root.setPadding(
                    root.getPaddingLeft(),
                    root.getPaddingTop(),
                    root.getPaddingRight(),
                    dp(24)
            );
        }
    }

    // ============================================================
    // TELA PRINCIPAL
    // ============================================================

    private void configurarTela() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setBackgroundColor(Color.BLACK);

        configurarAreaSegura(layout);

        // ========================================================
        // CABEÇALHO
        // ========================================================

        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.HORIZONTAL
        );

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        header.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(4)
        );

        // MENU
        ImageButton menu =
                new ImageButton(this);

        menu.setImageResource(
                android.R.drawable.ic_menu_sort_by_size
        );

        menu.setBackgroundColor(
                Color.TRANSPARENT
        );

        menu.setColorFilter(Color.WHITE);

        menu.setContentDescription(
                "Menu"
        );

        menu.setOnClickListener(
                v -> abrirMenuHistorico()
        );

        header.addView(
                menu,
                new LinearLayout.LayoutParams(
                        dp(48),
                        dp(48)
                )
        );

        // TÍTULO
        LinearLayout tituloArea =
                new LinearLayout(this);

        tituloArea.setOrientation(
                LinearLayout.VERTICAL
        );

        tituloArea.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView titulo =
                criarTexto(
                        "J.A.R.V.I.S",
                        21,
                        Color.WHITE
                );

        titulo.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        TextView subtitulo =
                criarTexto(
                        "ASSISTENTE LOCAL",
                        10,
                        Color.GRAY
                );

        tituloArea.addView(titulo);
        tituloArea.addView(subtitulo);

        header.addView(
                tituloArea,
                new LinearLayout.LayoutParams(
                        0,
                        dp(48),
                        1f
                )
        );

        // CONFIGURAÇÕES
        ImageButton configuracoes =
                new ImageButton(this);

        configuracoes.setImageResource(
                android.R.drawable.ic_menu_manage
        );

        configuracoes.setBackgroundColor(
                Color.TRANSPARENT
        );

        configuracoes.setColorFilter(Color.WHITE);

        configuracoes.setContentDescription(
                "Configurações"
        );

        configuracoes.setOnClickListener(
                v -> abrirMenuJarvis()
        );

        header.addView(
                configuracoes,
                new LinearLayout.LayoutParams(
                        dp(48),
                        dp(48)
                )
        );

        layout.addView(
                header,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(60)
                )
        );

        // ========================================================
        // RELÓGIO
        // ========================================================

        clockText =
                criarTexto(
                        "",
                        13,
                        Color.GRAY
                );

        clockText.setGravity(
                Gravity.CENTER
        );

        layout.addView(
                clockText,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(26)
                )
        );

        // ========================================================
        // ÁREA DO REATOR
        // ========================================================

        reactorArea =
                new LinearLayout(this);

        reactorArea.setGravity(
                Gravity.CENTER
        );

        reactorView =
                new ReactorView(this);

        reactorArea.addView(
                reactorView,
                new LinearLayout.LayoutParams(
                        dp(230),
                        dp(230)
                )
        );

        layout.addView(
                reactorArea,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(250)
                )
        );

        // ========================================================
        // CHAT
        // ========================================================

        chatScroll =
                new ScrollView(this);

        chatScroll.setFillViewport(true);

        chatContainer =
                new LinearLayout(this);

        chatContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        chatContainer.setPadding(
                dp(12),
                dp(8),
                dp(12),
                dp(8)
        );

        chatScroll.addView(chatContainer);

        layout.addView(
                chatScroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1f
                )
        );

        // ========================================================
        // ÁREA DE COMANDO
        // ========================================================

        LinearLayout entrada =
                new LinearLayout(this);

        entrada.setOrientation(
                LinearLayout.HORIZONTAL
        );

        entrada.setGravity(
                Gravity.CENTER_VERTICAL
        );

        entrada.setPadding(
                dp(8),
                dp(6),
                dp(8),
                dp(6)
        );

        // CAMPO DE TEXTO
        commandInput =
                new EditText(this);

        commandInput.setSingleLine(true);

        commandInput.setTextColor(Color.WHITE);

        commandInput.setHintTextColor(
                Color.DKGRAY
        );

        commandInput.setHint(
                "Digite um comando..."
        );

        commandInput.setTextSize(15);

        commandInput.setImeOptions(
                EditorInfo.IME_ACTION_SEND
        );

        GradientDrawable fundoEntrada =
                new GradientDrawable();

        fundoEntrada.setColor(
                Color.rgb(20, 20, 20)
        );

        fundoEntrada.setCornerRadius(
                dp(18)
        );

        commandInput.setBackground(
                fundoEntrada
        );

        commandInput.setPadding(
                dp(16),
                dp(8),
                dp(16),
                dp(8)
        );

        commandInput.setOnEditorActionListener(
                (v, actionId, event) -> {

                    if (actionId ==
                            EditorInfo.IME_ACTION_SEND) {

                        enviarTexto();
                        return true;
                    }

                    return false;
                }
        );

        entrada.addView(
                commandInput,
                new LinearLayout.LayoutParams(
                        0,
                        dp(52),
                        1f
                )
        );

        // MICROFONE
        ImageButton microfone =
                new ImageButton(this);

        microfone.setImageResource(
                android.R.drawable.ic_btn_speak_now
        );

        microfone.setBackgroundColor(
                Color.TRANSPARENT
        );

        microfone.setColorFilter(
                Color.WHITE
        );

        microfone.setContentDescription(
                "Microfone"
        );

        microfone.setOnClickListener(
                v -> {

                    if (!microfoneAtivado) {

                        Toast.makeText(
                                this,
                                "Microfone desativado nas configurações.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    iniciarReconhecimento();
                }
        );

        entrada.addView(
                microfone,
                new LinearLayout.LayoutParams(
                        dp(52),
                        dp(52)
                )
        );

        // ENVIAR
        Button enviar =
                new Button(this);

        enviar.setText("ENVIAR");

        enviar.setTextColor(
                Color.WHITE
        );

        enviar.setBackgroundColor(
                Color.TRANSPARENT
        );

        enviar.setOnClickListener(
                v -> enviarTexto()
        );

        entrada.addView(
                enviar,
                new LinearLayout.LayoutParams(
                        dp(82),
                        dp(52)
                )
        );

        layout.addView(
                entrada,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(64)
                )
        );

        setContentView(layout);

        // ========================================================
        // RESTAURAÇÃO DO ESTADO
        // ========================================================

        if (telaPrincipalLimpa ||
                historico.isEmpty()) {

            mostrarReator();

        } else {

            esconderReator();
            renderizarHistoricoPrincipal();
        }
    }

    // ============================================================
    // ENVIO DE TEXTO
    // ============================================================

    private void enviarTexto() {

        if (commandInput == null) {
            return;
        }

        String texto =
                commandInput
                        .getText()
                        .toString()
                        .trim();

        if (texto.isEmpty()) {
            return;
        }

        commandInput.setText("");

        adicionarAoHistorico(
                "VOCÊ",
                texto
        );

        esconderReator();

        adicionarBalaoVisual(
                "VOCÊ",
                texto
        );

        processarComando(texto);

        rolarParaBaixo();
    }

    // ============================================================
    // REATOR
    // ============================================================

    private void mostrarReator() {

        if (reactorArea != null) {
            reactorArea.setVisibility(
                    View.VISIBLE
            );
        }
    }

    private void esconderReator() {

        if (reactorArea != null) {
            reactorArea.setVisibility(
                    View.GONE
            );
        }
    }

    private void marcarTelaComMensagem() {

        telaPrincipalLimpa = false;

        preferencias.edit()
                .putBoolean(
                        "tela_principal_limpa",
                        false
                )
                .apply();
    }

    // ============================================================
    // HISTÓRICO
    // ============================================================

    private void carregarHistorico() {

        historico.clear();

        String dados =
                preferencias.getString(
                        "historico",
                        ""
                );

        if (dados.isEmpty()) {
            return;
        }

        String[] registros =
                dados.split(
                        "\\|\\|REGISTRO\\|\\|"
                );

        for (String registro : registros) {

            if (registro.trim().isEmpty()) {
                continue;
            }

            try {

                String[] partes =
                        registro.split(
                                "\\|\\|CAMPO\\|\\|",
                                -1
                        );

                if (partes.length < 3) {
                    continue;
                }

                String autor =
                        new String(
                                android.util.Base64.decode(
                                        partes[0],
                                        android.util.Base64.DEFAULT
                                )
                        );

                String texto =
                        new String(
                                android.util.Base64.decode(
                                        partes[1],
                                        android.util.Base64.DEFAULT
                                )
                        );

                long horario =
                        Long.parseLong(partes[2]);

                historico.add(
                        new MensagemHistorico(
                                autor,
                                texto,
                                horario
                        )
                );

            } catch (Exception ignored) {
            }
        }
    }

    private void salvarHistorico() {

        StringBuilder builder =
                new StringBuilder();

        for (MensagemHistorico mensagem :
                historico) {

            String autor =
                    android.util.Base64.encodeToString(
                            mensagem.autor.getBytes(),
                            android.util.Base64.NO_WRAP
                    );

            String texto =
                    android.util.Base64.encodeToString(
                            mensagem.texto.getBytes(),
                            android.util.Base64.NO_WRAP
                    );

            builder
                    .append(autor)
                    .append("||CAMPO||")
                    .append(texto)
                    .append("||CAMPO||")
                    .append(mensagem.horario)
                    .append("||REGISTRO||");
        }

        preferencias.edit()
                .putString(
                        "historico",
                        builder.toString()
                )
                .apply();
    }

    private void adicionarAoHistorico(
            String autor,
            String texto
    ) {

        historico.add(
                new MensagemHistorico(
                        autor,
                        texto,
                        System.currentTimeMillis()
                )
        );

        salvarHistorico();

        marcarTelaComMensagem();
    }

    private void renderizarHistoricoPrincipal() {

        if (chatContainer == null) {
            return;
        }

        chatContainer.removeAllViews();

        for (MensagemHistorico mensagem :
                historico) {

            adicionarBalaoVisual(
                    mensagem.autor,
                    mensagem.texto
            );
        }

        rolarParaBaixo();
    }

    private void limparTelaPrincipal() {

        telaPrincipalLimpa = true;

        preferencias.edit()
                .putBoolean(
                        "tela_principal_limpa",
                        true
                )
                .apply();

        if (chatContainer != null) {
            chatContainer.removeAllViews();
        }

        mostrarReator();

        Toast.makeText(
                this,
                "Tela principal limpa. O histórico foi preservado.",
                Toast.LENGTH_SHORT
        ).show();
    }

    // ============================================================
    // BALÕES DE MENSAGEM
    // ============================================================

    private void adicionarBalaoVisual(
            String autor,
            String texto
    ) {

        if (chatContainer == null) {
            return;
        }

        LinearLayout linha =
                new LinearLayout(this);

        linha.setOrientation(
                LinearLayout.VERTICAL
        );

        linha.setPadding(
                dp(4),
                dp(4),
                dp(4),
                dp(4)
        );

        TextView nome =
                criarTexto(
                        autor,
                        11,
                        autor.equals("VOCÊ")
                                ? Color.GRAY
                                : Color.WHITE
                );

        nome.setPadding(
                dp(8),
                dp(2),
                dp(8),
                dp(2)
        );

        TextView mensagem =
                criarTexto(
                        texto,
                        15,
                        Color.WHITE
                );

        mensagem.setPadding(
                dp(14),
                dp(10),
                dp(14),
                dp(10)
        );

        GradientDrawable fundo =
                new GradientDrawable();

        fundo.setColor(
                autor.equals("VOCÊ")
                        ? Color.rgb(30, 30, 30)
                        : Color.rgb(15, 15, 15)
        );

        fundo.setCornerRadius(
                dp(14)
        );

        mensagem.setBackground(fundo);

        linha.addView(nome);

        linha.addView(
                mensagem,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        chatContainer.addView(
                linha,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );
    }

    private void adicionarMensagem(
            String autor,
            String texto
    ) {

        adicionarAoHistorico(
                autor,
                texto
        );

        esconderReator();

        adicionarBalaoVisual(
                autor,
                texto
        );

        rolarParaBaixo();
    }

    private void responder(String texto) {

        adicionarMensagem(
                "J.A.R.V.I.S",
                texto
        );

        falar(texto);
    }

    private void rolarParaBaixo() {

        if (chatScroll == null) {
            return;
        }

        chatScroll.post(
                () -> chatScroll.fullScroll(
                        View.FOCUS_DOWN
                )
        );
    }

    // ============================================================
    // MENU DE HISTÓRICO
    // ============================================================

    private void abrirMenuHistorico() {

        LinearLayout layout =
                criarLayoutMenu();

        adicionarBotaoTela(
                layout,
                "🕘  HISTÓRICO DE CONVERSA",
                this::abrirHistorico
        );

        adicionarBotaoTela(
                layout,
                "📤  EXPORTAR CONVERSAS",
                this::exportarConversas
        );

        adicionarBotaoTela(
                layout,
                "🧹  LIMPAR CONVERSAS",
                this::confirmarLimparConversas
        );

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::voltarTela
        );

        setContentView(layout);
    }

    private void abrirHistorico() {

        LinearLayout layout =
                criarLayoutMenu();

        TextView titulo =
                criarTexto(
                        "HISTÓRICO DE CONVERSA",
                        20,
                        Color.WHITE
                );

        titulo.setGravity(
                Gravity.CENTER
        );

        layout.addView(
                titulo,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(60)
                )
        );

        if (historico.isEmpty()) {

            TextView vazio =
                    criarTexto(
                            "Nenhuma conversa registrada.",
                            15,
                            Color.GRAY
                    );

            vazio.setGravity(
                    Gravity.CENTER
            );

            layout.addView(
                    vazio,
                    new LinearLayout.LayoutParams(
                            -1,
                            dp(80)
                    )
            );

        } else {

            for (MensagemHistorico mensagem :
                    historico) {

                TextView item =
                        criarTexto(
                                mensagem.autor +
                                        ": " +
                                        mensagem.texto,
                                14,
                                Color.WHITE
                        );

                item.setPadding(
                        dp(12),
                        dp(12),
                        dp(12),
                        dp(12)
                );

                layout.addView(
                        item,
                        new LinearLayout.LayoutParams(
                                -1,
                                -2
                        )
                );
            }
        }

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::abrirMenuHistorico
        );

        setContentView(layout);
    }

    // ============================================================
    // LIMPAR HISTÓRICO
    // ============================================================

    private void confirmarLimparConversas() {

        new AlertDialog.Builder(this)
                .setTitle(
                        "Limpar conversas"
                )
                .setMessage(
                        "Isso apagará permanentemente todo o histórico salvo."
                )
                .setNegativeButton(
                        "CANCELAR",
                        null
                )
                .setPositiveButton(
                        "SIM",
                        (dialog, which) -> {

                            historico.clear();

                            preferencias.edit()
                                    .remove("historico")
                                    .apply();

                            telaPrincipalLimpa =
                                    true;

                            preferencias.edit()
                                    .putBoolean(
                                            "tela_principal_limpa",
                                            true
                                    )
                                    .apply();

                            Toast.makeText(
                                    this,
                                    "Conversas apagadas permanentemente.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            configurarTela();
                        }
                )
                .show();
    }

    // ============================================================
    // EXPORTAR CONVERSAS
    // ============================================================

    private void exportarConversas() {

        if (historico.isEmpty()) {

            Toast.makeText(
                    this,
                    "Não há conversas para exportar.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        "Exportar conversas"
                )
                .setMessage(
                        "As conversas podem conter informações pessoais. " +
                                "O Android mostrará as opções de compartilhamento."
                )
                .setNegativeButton(
                        "CANCELAR",
                        null
                )
                .setPositiveButton(
                        "CONTINUAR",
                        (dialog, which) ->
                                realizarExportacao()
                )
                .show();
    }

    private void realizarExportacao() {

        StringBuilder texto =
                new StringBuilder();

        texto.append(
                "J.A.R.V.I.S — HISTÓRICO\n\n"
        );

        SimpleDateFormat formato =
                new SimpleDateFormat(
                        "dd/MM/yyyy HH:mm:ss",
                        Locale.getDefault()
                );

        for (MensagemHistorico mensagem :
                historico) {

            texto.append(
                    "["
            );

            texto.append(
                    formato.format(
                            new Date(
                                    mensagem.horario
                            )
                    )
            );

            texto.append(
                    "] "
            );

            texto.append(
                    mensagem.autor
            );

            texto.append(
                    ": "
            );

            texto.append(
                    mensagem.texto
            );

            texto.append(
                    "\n\n"
            );
        }

        Intent compartilhar =
                new Intent(
                        Intent.ACTION_SEND
                );

        compartilhar.setType(
                "text/plain"
        );

        compartilhar.putExtra(
                Intent.EXTRA_TEXT,
                texto.toString()
        );

        startActivity(
                Intent.createChooser(
                        compartilhar,
                        "Compartilhar histórico"
                )
        );
    }

    // ============================================================
    // MENU JARVIS
    // ============================================================

    private void abrirMenuJarvis() {

        LinearLayout layout =
                criarLayoutMenu();

        adicionarBotaoTela(
                layout,
                "⚙  GERENCIAR JARVIS",
                this::abrirGerenciarJarvis
        );

        adicionarBotaoTela(
                layout,
                "🔒  PRIVACIDADE",
                this::abrirPrivacidade
        );

        adicionarBotaoTela(
                layout,
                "🛡  VERIFICAÇÃO",
                this::abrirVerificacao
        );

        adicionarBotaoTela(
                layout,
                "🎙  COMANDO DE VOZ",
                this::abrirComandoVoz
        );

        adicionarBotaoTela(
                layout,
                "⚙  CONFIGURAÇÕES DO ANDROID",
                this::abrirConfiguracoesAndroid
        );

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::voltarTela
        );

        setContentView(layout);
    }

    // ============================================================
    // GERENCIAR JARVIS
    // ============================================================

    private void abrirGerenciarJarvis() {

        LinearLayout layout =
                criarLayoutMenu();

        adicionarTituloMenu(
                layout,
                "GERENCIAR JARVIS"
        );

        adicionarTextoConfiguracao(
                layout,
                "Modo de operação",
                "O JARVIS sempre inicia em modo offline."
        );

        adicionarBotaoTela(
                layout,
                modoOnline
                        ? "OFFLINE [ATIVAR]"
                        : "OFFLINE [EM USO]",
                () -> {

                    modoOnline = false;

                    responder(
                            "Modo offline ativado. " +
                                    "O JARVIS não realizará conexões online."
                    );

                    abrirGerenciarJarvis();
                }
        );

        adicionarBotaoTela(
                layout,
                modoOnline
                        ? "ONLINE [EM USO]"
                        : "ONLINE [ATIVAR]",
                () -> {

                    modoOnline = true;

                    responder(
                            "Modo online selecionado. " +
                                    "O acesso à rede ainda depende de uma implementação " +
                                    "de conexão e das permissões correspondentes."
                    );

                    abrirGerenciarJarvis();
                }
        );

        adicionarTextoConfiguracao(
                layout,
                "Microfone",
                microfoneAtivado
                        ? "Ativado"
                        : "Desativado"
        );

        adicionarBotaoTela(
                layout,
                microfoneAtivado
                        ? "DESATIVAR MICROFONE"
                        : "ATIVAR MICROFONE",
                () -> {

                    microfoneAtivado =
                            !microfoneAtivado;

                    salvarConfiguracoes();

                    abrirGerenciarJarvis();
                }
        );

        adicionarTextoConfiguracao(
                layout,
                "Status do telefone",
                statusAtivado
                        ? "Ativado"
                        : "Desativado"
        );

        adicionarBotaoTela(
                layout,
                statusAtivado
                        ? "DESATIVAR STATUS"
                        : "ATIVAR STATUS",
                () -> {

                    statusAtivado =
                            !statusAtivado;

                    salvarConfiguracoes();

                    abrirGerenciarJarvis();
                }
        );

        adicionarTextoConfiguracao(
                layout,
                "Notificações",
                notificacoesAtivadas
                        ? "Ativadas"
                        : "Desativadas"
        );

        adicionarBotaoTela(
                layout,
                notificacoesAtivadas
                        ? "DESATIVAR NOTIFICAÇÕES"
                        : "ATIVAR NOTIFICAÇÕES",
                () -> {

                    notificacoesAtivadas =
                            !notificacoesAtivadas;

                    salvarConfiguracoes();

                    abrirGerenciarJarvis();
                }
        );

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::abrirMenuJarvis
        );

        setContentView(layout);
    }

    // ============================================================
    // PRIVACIDADE
    // ============================================================

    private void abrirPrivacidade() {

        LinearLayout layout =
                criarLayoutMenu();

        adicionarTituloMenu(
                layout,
                "PRIVACIDADE"
        );

        TextView texto =
                criarTexto(
                        "O JARVIS foi projetado para funcionar " +
                                "dentro das permissões normais do Android.\n\n" +

                                "Ele não possui root, não é administrador " +
                                "do dispositivo e não deve tentar ultrapassar " +
                                "as restrições do Android.\n\n" +

                                "O modo offline é o padrão ao abrir o aplicativo.\n\n" +

                                "O aplicativo não deve fingir que possui acesso " +
                                "a dados que o Android não disponibilizou.",
                        14,
                        Color.LTGRAY
                );

        texto.setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(16)
        );

        layout.addView(
                texto,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::abrirMenuJarvis
        );

        setContentView(layout);
    }

    // ============================================================
    // VERIFICAÇÃO
    // ============================================================

    private void abrirVerificacao() {

        LinearLayout layout =
                criarLayoutMenu();

        adicionarTituloMenu(
                layout,
                "VERIFICAÇÃO"
        );

        TextView aviso =
                criarTexto(
                        "A verificação usa somente informações " +
                                "que um aplicativo Android comum consegue " +
                                "consultar sem root.\n\n" +

                                "Ela não é um antivírus e não consegue " +
                                "garantir que o telefone esteja livre de malware.",
                        14,
                        Color.LTGRAY
                );

        aviso.setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(16)
        );

        layout.addView(
                aviso,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        TextView resultado =
                criarTexto(
                        textoUltimaVerificacao(),
                        14,
                        Color.WHITE
                );

        resultado.setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(16)
        );

        layout.addView(
                resultado,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        Button verificar =
                criarBotao(
                        "EXECUTAR VERIFICAÇÃO"
                );

        verificar.setOnClickListener(
                v -> {

                    String resultadoTexto =
                            executarVerificacao();

                    resultado.setText(
                            resultadoTexto
                    );

                    Toast.makeText(
                            this,
                            "Verificação concluída.",
                            Toast.LENGTH_SHORT
                    ).show();
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

    private String textoUltimaVerificacao() {

        if (ultimaVerificacao <= 0L) {

            return "Última verificação:\nNunca executada.";
        }

        SimpleDateFormat formato =
                new SimpleDateFormat(
                        "dd/MM/yyyy HH:mm:ss",
                        Locale.getDefault()
                );

        return "Última verificação:\n" +
                formato.format(
                        new Date(
                                ultimaVerificacao
                        )
                );
    }

    private String executarVerificacao() {

        ultimaVerificacao =
                System.currentTimeMillis();

        preferencias.edit()
                .putLong(
                        "ultima_verificacao",
                        ultimaVerificacao
                )
                .apply();

        int aplicativos =
                0;

        try {

            List<ApplicationInfo> lista =
                    getPackageManager()
                            .getInstalledApplications(
                                    PackageManager.GET_META_DATA
                            );

            if (lista != null) {
                aplicativos = lista.size();
            }

        } catch (Exception ignored) {
        }

        return
                "Resultado:\n\n" +
                        "Verificação básica concluída.\n\n" +

                        "Aplicativos visíveis ao sistema: " +
                        aplicativos +
                        "\n\n" +

                        "O JARVIS analisou somente informações " +
                        "que o aplicativo consegue acessar normalmente.\n\n" +

                        "Isso não é um antivírus e não permite " +
                        "confirmar a existência ou ausência de malware.\n\n" +

                        textoUltimaVerificacao();
    }

    // ============================================================
    // COMANDO DE VOZ
    // ============================================================

    private void abrirComandoVoz() {

        LinearLayout layout =
                criarLayoutMenu();

        adicionarTituloMenu(
                layout,
                "COMANDO DE VOZ"
        );

        TextView info =
                criarTexto(
                        "Reconhecimento offline: " +
                                "o JARVIS tenta usar o reconhecimento " +
                                "de voz fornecido pelo próprio Android.\n\n" +

                                "O serviço de ativação atual reconhece " +
                                "a palavra JARVIS.",
                        14,
                        Color.LTGRAY
                );

        info.setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(16)
        );

        layout.addView(
                info,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        adicionarBotaoTela(
                layout,
                microfoneAtivado
                        ? "MICROFONE: ATIVADO"
                        : "MICROFONE: DESATIVADO",
                () -> {

                    microfoneAtivado =
                            !microfoneAtivado;

                    salvarConfiguracoes();

                    abrirComandoVoz();
                }
        );

        adicionarBotaoTela(
                layout,
                "TESTAR RECONHECIMENTO",
                () -> {

                    if (!microfoneAtivado) {

                        Toast.makeText(
                                this,
                                "Microfone desativado.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    iniciarReconhecimento();
                }
        );

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::abrirMenuJarvis
        );

        setContentView(layout);
    }

    // ============================================================
    // CONFIGURAÇÕES DO ANDROID
    // ============================================================

    private void abrirConfiguracoesAndroid() {

        try {

            Intent intent =
                    new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    );

            intent.setData(
                    android.net.Uri.parse(
                            "package:" +
                                    getPackageName()
                    )
            );

            startActivity(intent);

        } catch (Exception e) {

            try {

                startActivity(
                        new Intent(
                                Settings.ACTION_SETTINGS
                        )
                );

            } catch (Exception ignored) {
            }
        }
    }

    // ============================================================
    // TEXT TO SPEECH
    // ============================================================

    private void iniciarVoz() {

        tts =
                new TextToSpeech(
                        this,
                        status -> {

                            if (status ==
                                    TextToSpeech.SUCCESS) {

                                int idioma =
                                        tts.setLanguage(
                                                new Locale(
                                                        "pt",
                                                        "BR"
                                                )
                                        );

                                ttsReady =
                                        idioma !=
                                                TextToSpeech.LANG_MISSING_DATA &&
                                                idioma !=
                                                        TextToSpeech.LANG_NOT_SUPPORTED;

                                if (ttsReady) {

                                    configurarVozTts();
                                }
                            }
                        }
                );
    }

    private void configurarVozTts() {

        if (tts == null) {
            return;
        }

        try {

            tts.setSpeechRate(
                    0.92f
            );

            tts.setPitch(
                    0.95f
            );

            Set<Voice> vozes =
                    tts.getVoices();

            if (vozes == null) {
                return;
            }

            for (Voice voz : vozes) {

                if (voz == null) {
                    continue;
                }

                Locale locale =
                        voz.getLocale();

                if (locale != null &&
                        locale.getLanguage()
                                .equals("pt")) {

                    try {
                        tts.setVoice(voz);
                    } catch (Exception ignored) {
                    }

                    break;
                }
            }

        } catch (Exception ignored) {
        }
    }

    private void falar(String texto) {

        if (!ttsReady ||
                tts == null ||
                texto == null ||
                texto.isEmpty()) {

            return;
        }

        try {

            tts.speak(
                    texto,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "JARVIS_" +
                            System.currentTimeMillis()
            );

        } catch (Exception ignored) {
        }
    }

    // ============================================================
    // RECONHECIMENTO DE VOZ OFFLINE
    // ============================================================

    private void iniciarReconhecimento() {

        if (Build.VERSION.SDK_INT < 31) {

            Toast.makeText(
                    this,
                    "O reconhecimento offline depende do suporte do Android.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(
                this
        )) {

            Toast.makeText(
                    this,
                    "Reconhecimento de voz não disponível.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (speechRecognizer != null) {

            try {
                speechRecognizer.destroy();
            } catch (Exception ignored) {
            }
        }

        try {

            if (SpeechRecognizer.isOnDeviceRecognitionAvailable(
                    this
            )) {

                speechRecognizer =
                        SpeechRecognizer
                                .createOnDeviceSpeechRecognizer(
                                        this
                                );

            } else {

                speechRecognizer =
                        SpeechRecognizer
                                .createSpeechRecognizer(
                                        this
                                );
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Não foi possível iniciar o reconhecimento.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        speechRecognizer.setRecognitionListener(
                new RecognitionListener() {

                    @Override
                    public void onReadyForSpeech(
                            android.os.Bundle params
                    ) {
                        ouvindo = true;
                    }

                    @Override
                    public void onBeginningOfSpeech() {
                    }

                    @Override
                    public void onRmsChanged(
                            float rmsdB
                    ) {
                    }

                    @Override
                    public void onBufferReceived(
                            byte[] buffer
                    ) {
                    }

                    @Override
                    public void onEndOfSpeech() {
                        ouvindo = false;
                    }

                    @Override
                    public void onError(
                            int error
                    ) {

                        ouvindo = false;

                        if (error ==
                                SpeechRecognizer.ERROR_NO_MATCH) {

                            Toast.makeText(
                                    MainActivity.this,
                                    "Não entendi o comando.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onResults(
                            android.os.Bundle results
                    ) {

                        ouvindo = false;

                        ArrayList<String> encontrados =
                                results.getStringArrayList(
                                        SpeechRecognizer.RESULTS_RECOGNITION
                                );

                        if (encontrados == null ||
                                encontrados.isEmpty()) {

                            return;
                        }

                        String texto =
                                encontrados.get(0);

                        if (texto == null ||
                                texto.trim().isEmpty()) {

                            return;
                        }

                        adicionarAoHistorico(
                                "VOCÊ",
                                texto
                        );

                        esconderReator();

                        adicionarBalaoVisual(
                                "VOCÊ",
                                texto
                        );

                        processarComando(
                                texto
                        );
                    }

                    @Override
                    public void onPartialResults(
                            android.os.Bundle partialResults
                    ) {
                    }

                    @Override
                    public void onEvent(
                            int eventType,
                            android.os.Bundle params
                    ) {
                    }
                }
        );

        Intent intent =
                new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "pt-BR"
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                false
        );

        try {

            speechRecognizer.startListening(
                    intent
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Não foi possível iniciar o microfone.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ============================================================
    // NORMALIZAÇÃO DE TEXTO
    // ============================================================

    private String normalizar(String texto) {

        if (texto == null) {
            return "";
        }

        String normalizado =
                Normalizer.normalize(
                        texto,
                        Normalizer.Form.NFD
                );

        normalizado =
                normalizado.replaceAll(
                        "\\p{InCombiningDiacriticalMarks}+",
                        ""
                );

        return normalizado
                .toLowerCase(Locale.getDefault())
                .trim();
    }

    private boolean contem(
            String comando,
            String... termos
    ) {

        String texto =
                normalizar(comando);

        for (String termo : termos) {

            if (texto.contains(
                    normalizar(termo)
            )) {
                return true;
            }
        }

        return false;
    }

    private boolean fraseExata(
            String comando,
            String... frases
    ) {

        String texto =
                normalizar(comando);

        for (String frase : frases) {

            if (texto.equals(
                    normalizar(frase)
            )) {
                return true;
            }
        }

        return false;
    }

    // ============================================================
    // PROCESSAMENTO DE COMANDOS
    // ============================================================

    private void processarComando(
            String comandoOriginal
    ) {

        String comando =
                normalizar(comandoOriginal);

        if (comando.isEmpty()) {
            return;
        }

        // --------------------------------------------------------
        // ATIVAÇÃO / DISPONIBILIDADE
        // --------------------------------------------------------

        if (fraseExata(
                comando,
                "jarvis",
                "jarvis está aí",
                "jarvis esta ai",
                "você está aí",
                "voce esta ai",
                "está disponível",
                "esta disponivel"
        )) {

            responder(
                    "À sua disposição. Sistemas locais operacionais."
            );

            return;
        }

        // --------------------------------------------------------
        // SAUDAÇÕES
        // --------------------------------------------------------

        if (fraseExata(
                comando,
                "oi",
                "ola",
                "olá",
                "e ai",
                "e aí",
                "fala"
        )) {

            responder(
                    "Olá. À sua disposição."
            );

            return;
        }

        if (contem(
                comando,
                "bom dia"
        )) {

            responder(
                    "Bom dia. Sistemas locais operacionais."
            );

            return;
        }

        if (contem(
                comando,
                "boa tarde"
        )) {

            responder(
                    "Boa tarde. Sistemas locais operacionais."
            );

            return;
        }

        if (contem(
                comando,
                "boa noite"
        )) {

            responder(
                    "Boa noite. Sistemas locais operacionais."
            );

            return;
        }

        // --------------------------------------------------------
        // COMO ESTÁ
        // --------------------------------------------------------

        if (contem(
                comando,
                "tudo bem",
                "como voce esta",
                "como você está"
        )) {

            responder(
                    "Todos os sistemas locais estão operacionais."
            );

            return;
        }

        // --------------------------------------------------------
        // AGRADECIMENTO
        // --------------------------------------------------------

        if (contem(
                comando,
                "obrigado",
                "obrigada",
                "valeu"
        )) {

            responder(
                    "À disposição."
            );

            return;
        }

        // --------------------------------------------------------
        // DESPEDIDA
        // --------------------------------------------------------

        if (contem(
                comando,
                "tchau",
                "ate mais",
                "até mais"
        )) {

            responder(
                    "Até mais. Permanecerei disponível."
            );

            return;
        }

        // --------------------------------------------------------
        // AJUDA
        // --------------------------------------------------------

        if (contem(
                comando,
                "o que voce consegue fazer",
                "o que você consegue fazer",
                "o que voce pode fazer",
                "o que você pode fazer",
                "ajuda",
                "comandos"
        )) {

            responder(
                    "Posso conversar, informar horário e data, " +
                            "consultar bateria, temperatura da bateria, " +
                            "memória RAM, armazenamento e conectividade, " +
                            "além de executar comandos locais disponíveis. " +
                            "Para resolver atividades escolares gerais, " +
                            "será necessário um motor de inteligência artificial " +
                            "mais completo."
            );

            return;
        }

        // --------------------------------------------------------
        // IDENTIDADE
        // --------------------------------------------------------

        if (contem(
                comando,
                "quem é você",
                "quem voce e",
                "seu nome"
        )) {

            responder(
                    "Sou o J.A.R.V.I.S., seu assistente local."
            );

            return;
        }

        // --------------------------------------------------------
        // CHATGPT
        // --------------------------------------------------------

        if (contem(
                comando,
                "chatgpt"
        )) {

            responder(
                    "O ChatGPT é um sistema de inteligência artificial " +
                            "da OpenAI. Eu sou o JARVIS Lite e atualmente " +
                            "estou funcionando principalmente com comandos locais."
            );

            return;
        }

        // --------------------------------------------------------
        // NOME PREFERIDO
        // --------------------------------------------------------

        if (comando.startsWith(
                "pode me chamar de "
        )) {

            String nome =
                    comandoOriginal.substring(
                            Math.min(
                                    comandoOriginal.length(),
                                    "pode me chamar de ".length()
                            )
                    ).trim();

            if (!nome.isEmpty()) {

                preferencias.edit()
                        .putString(
                                "nome_preferido",
                                nome
                        )
                        .apply();

                responder(
                        "Entendido. Vou usar " +
                                nome +
                                " nesta instalação."
                );

            } else {

                responder(
                        "Preciso que você diga o nome depois de 'pode me chamar de'."
                );
            }

            return;
        }

        // --------------------------------------------------------
        // NOME SALVO
        // --------------------------------------------------------

        if (contem(
                comando,
                "como voce pode me chamar",
                "como você pode me chamar",
                "qual meu nome"
        )) {

            String nome =
                    preferencias.getString(
                            "nome_preferido",
                            ""
                    );

            if (nome.isEmpty()) {

                responder(
                        "Você ainda não definiu um nome preferido."
                );

            } else {

                responder(
                        "Você pediu para eu usar o nome " +
                                nome +
                                "."
                );
            }

            return;
        }

        // --------------------------------------------------------
        // BATERIA COMPLETA
        // --------------------------------------------------------
        // IMPORTANTE:
        // Este bloco vem ANTES da consulta simples da bateria.
        // Assim "informações completas da bateria" não cai
        // no comando simples.

        if (contem(
                comando,
                "informacoes completas da bateria",
                "informações completas da bateria",
                "informacao completa da bateria",
                "informação completa da bateria",
                "analise completa da bateria",
                "análise completa da bateria",
                "detalhes completos da bateria",
                "todos os dados da bateria"
        )) {

            responder(
                    obterInformacoesCompletasBateria()
            );

            return;
        }

        // --------------------------------------------------------
        // HORA / DATA / BATERIA SIMPLES
        // --------------------------------------------------------

        boolean pediuHora =
                contem(
                        comando,
                        "hora",
                        "horas"
                );

        boolean pediuData =
                contem(
                        comando,
                        "data",
                        "dia de hoje"
                );

        boolean pediuBateria =
                contem(
                        comando,
                        "bateria",
                        "carga"
                );

        if (pediuHora ||
                pediuData ||
                pediuBateria) {

            StringBuilder resposta =
                    new StringBuilder();

            if (pediuHora) {

                resposta.append(
                        "Agora são "
                );

                resposta.append(
                        new SimpleDateFormat(
                                "HH:mm",
                                Locale.getDefault()
                        ).format(
                                new Date()
                        )
                );

                resposta.append(". ");
            }

            if (pediuData) {

                resposta.append(
                        "Hoje é "
                );

                resposta.append(
                        new SimpleDateFormat(
                                "dd/MM/yyyy",
                                Locale.getDefault()
                        ).format(
                                new Date()
                        )
                );

                resposta.append(". ");
            }

            if (pediuBateria) {

                resposta.append(
                        obterBateria()
                );
            }

            responder(
                    resposta.toString()
            );

            return;
        }

        // --------------------------------------------------------
        // STATUS COMPLETO
        // --------------------------------------------------------

        if (contem(
                comando,
                "status do telefone",
                "estado do telefone",
                "status do meu telefone",
                "estado do meu telefone",
                "analise completa do meu telefone",
                "análise completa do meu telefone",
                "informacoes completas do telefone",
                "informações completas do telefone",
                "informacao completa do telefone",
                "informação completa do telefone",
                "verificar meu telefone"
        )) {

            if (!statusAtivado) {

                responder(
                        "A consulta de status está desativada nas configurações."
                );

            } else {

                responder(
                        obterStatusCompleto()
                );
            }

            return;
        }

        // --------------------------------------------------------
        // TEMPERATURA
        // --------------------------------------------------------

        if (contem(
                comando,
                "temperatura",
                "temperatura do celular",
                "temperatura do telefone"
        )) {

            responder(
                    obterTemperaturaBateria()
            );

            return;
        }

        // --------------------------------------------------------
        // RAM
        // --------------------------------------------------------

        if (contem(
                comando,
                "ram",
                "memoria ram",
                "memória ram"
        )) {

            responder(
                    obterMemoriaRAM()
            );

            return;
        }

        // --------------------------------------------------------
        // ARMAZENAMENTO
        // --------------------------------------------------------

        if (contem(
                comando,
                "armazenamento",
                "espaco",
                "espaço",
                "memoria interna",
                "memória interna"
        )) {

            responder(
                    obterArmazenamento()
            );

            return;
        }

        // --------------------------------------------------------
        // REDE
        // --------------------------------------------------------

        if (contem(
                comando,
                "rede",
                "internet",
                "wifi",
                "wi fi",
                "dados moveis",
                "dados móveis",
                "conexao",
                "conexão"
        )) {

            responder(
                    obterInformacoesRede()
            );

            return;
        }

        // --------------------------------------------------------
        // PRIVACIDADE
        // --------------------------------------------------------

        if (contem(
                comando,
                "privacidade",
                "permissoes",
                "permissões"
        )) {

            abrirPrivacidade();

            responder(
                    "Aqui estão as informações de privacidade do aplicativo."
            );

            return;
        }

        // --------------------------------------------------------
        // VERIFICAÇÃO
        // --------------------------------------------------------

        if (contem(
                comando,
                "verificacao",
                "verificação",
                "verificar seguranca",
                "verificar segurança"
        )) {

            abrirVerificacao();

            return;
        }

        // --------------------------------------------------------
        // GERENCIAR JARVIS
        // --------------------------------------------------------

        if (contem(
                comando,
                "gerenciar jarvis",
                "configurar jarvis",
                "configuracoes do jarvis",
                "configurações do jarvis"
        )) {

            abrirGerenciarJarvis();

            return;
        }

        // --------------------------------------------------------
        // MICROFONE
        // --------------------------------------------------------

        if (contem(
                comando,
                "ativar microfone"
        )) {

            microfoneAtivado = true;

            salvarConfiguracoes();

            responder(
                    "Microfone ativado."
            );

            return;
        }

        if (contem(
                comando,
                "desativar microfone"
        )) {

            microfoneAtivado = false;

            salvarConfiguracoes();

            responder(
                    "Microfone desativado."
            );

            return;
        }

        // --------------------------------------------------------
        // MODO OFFLINE
        // --------------------------------------------------------

        if (contem(
                comando,
                "modo offline",
                "ficar offline",
                "ficar sem internet"
        )) {

            modoOnline = false;

            responder(
                    "Modo offline ativado. "
                            + "O JARVIS não realizará conexões online."
            );

            return;
        }

        // --------------------------------------------------------
        // MODO ONLINE
        // --------------------------------------------------------

        if (contem(
                comando,
                "modo online",
                "ficar online"
        )) {

            modoOnline = true;

            responder(
                    "Modo online selecionado. "
                            + "A implementação atual ainda não possui "
                            + "uma conexão de inteligência artificial online."
            );

            return;
        }

        // --------------------------------------------------------
        // MATEMÁTICA SIMPLES
        // --------------------------------------------------------

        String resultadoCalculo =
                tentarCalculoSimples(
                        comandoOriginal
                );

        if (resultadoCalculo != null) {

            responder(
                    resultadoCalculo
            );

            return;
        }

        // --------------------------------------------------------
        // LANTERNA
        // --------------------------------------------------------

        if (contem(
                comando,
                "lanterna",
                "ligar lanterna",
                "desligar lanterna"
        )) {

            responder(
                    "O controle da lanterna ainda não está implementado "
                            + "nesta versão."
            );

            return;
        }

        // --------------------------------------------------------
        // CLIMA
        // --------------------------------------------------------

        if (contem(
                comando,
                "tempo",
                "clima",
                "temperatura de hoje",
                "previsao",
                "previsão"
        )) {

            responder(
                    "A consulta de clima ainda depende de uma conexão online "
                            + "e da implementação de localização."
            );

            return;
        }

        // --------------------------------------------------------
        // FALLBACK HONESTO
        // --------------------------------------------------------

        responder(
                "Entendi o comando, mas essa função ainda não está "
                        + "implementada nesta versão local."
        );
    }

    // ============================================================
    // BATERIA
    // ============================================================

    private String obterBateria() {

        Intent bateria =
                registerReceiver(
                        null,
                        new IntentFilter(
                                Intent.ACTION_BATTERY_CHANGED
                        )
                );

        if (bateria == null) {

            return "Não foi possível consultar a bateria.";
        }

        int nivel =
                bateria.getIntExtra(
                        BatteryManager.EXTRA_LEVEL,
                        -1
                );

        int escala =
                bateria.getIntExtra(
                        BatteryManager.EXTRA_SCALE,
                        -1
                );

        int porcentagem =
                -1;

        if (nivel >= 0 &&
                escala > 0) {

            porcentagem =
                    (int) (
                            nivel *
                                    100f /
                                    escala
                    );
        }

        int status =
                bateria.getIntExtra(
                        BatteryManager.EXTRA_STATUS,
                        -1
                );

        String estado;

        if (status ==
                BatteryManager.BATTERY_STATUS_CHARGING) {

            estado = "carregando";

        } else if (status ==
                BatteryManager.BATTERY_STATUS_FULL) {

            estado = "carga completa";

        } else {

            estado = "não está carregando";
        }

        if (porcentagem < 0) {

            return "Não foi possível determinar a porcentagem da bateria.";
        }

        return "Bateria em " +
                porcentagem +
                "%. Estado: " +
                estado +
                ".";
    }

    private String obterInformacoesCompletasBateria() {

        Intent bateria =
                registerReceiver(
                        null,
                        new IntentFilter(
                                Intent.ACTION_BATTERY_CHANGED
                        )
                );

        if (bateria == null) {

            return "Não foi possível consultar os dados da bateria.";
        }

        int nivel =
                bateria.getIntExtra(
                        BatteryManager.EXTRA_LEVEL,
                        -1
                );

        int escala =
                bateria.getIntExtra(
                        BatteryManager.EXTRA_SCALE,
                        -1
                );

        int porcentagem =
                nivel >= 0 &&
                        escala > 0
                        ? (int) (
                        nivel *
                                100f /
                                escala
                )
                        : -1;

        int status =
                bateria.getIntExtra(
                        BatteryManager.EXTRA_STATUS,
                        -1
                );

        int health =
                bateria.getIntExtra(
                        BatteryManager.EXTRA_HEALTH,
                        -1
                );

        int temperatura =
                bateria.getIntExtra(
                        BatteryManager.EXTRA_TEMPERATURE,
                        -1
                );

        int voltagem =
                bateria.getIntExtra(
                        BatteryManager.EXTRA_VOLTAGE,
                        -1
                );

        String tecnologia =
                bateria.getStringExtra(
                        BatteryManager.EXTRA_TECHNOLOGY
                );

        String estado;

        switch (status) {

            case BatteryManager.BATTERY_STATUS_CHARGING:
                estado = "carregando";
                break;

            case BatteryManager.BATTERY_STATUS_FULL:
                estado = "carga completa";
                break;

            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                estado = "descarregando";
                break;

            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                estado = "não está carregando";
                break;

            default:
                estado = "desconhecido";
                break;
        }

        String saude;

        switch (health) {

            case BatteryManager.BATTERY_HEALTH_GOOD:
                saude = "boa";
                break;

            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                saude = "superaquecimento";
                break;

            case BatteryManager.BATTERY_HEALTH_DEAD:
                saude = "crítica";
                break;

            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                saude = "sobretensão";
                break;

            case BatteryManager.BATTERY_HEALTH_COLD:
                saude = "muito fria";
                break;

            default:
                saude = "desconhecida";
                break;
        }

        StringBuilder resposta =
                new StringBuilder();

        resposta.append(
                "Informações da bateria:\n\n"
        );

        resposta.append(
                "Carga: "
        );

        if (porcentagem >= 0) {
            resposta.append(
                    porcentagem
            ).append("%");
        } else {
            resposta.append(
                    "indisponível"
            );
        }

        resposta.append("\n");

        resposta.append(
                "Estado: "
        ).append(
                estado
        ).append("\n");

        resposta.append(
                "Saúde informada pelo Android: "
        ).append(
                saude
        ).append("\n");

        if (temperatura >= 0) {

            resposta.append(
                    "Temperatura: "
            ).append(
                    temperatura / 10f
            ).append(
                    " °C\n"
            );
        }

        if (voltagem >= 0) {

            resposta.append(
                    "Voltagem: "
            ).append(
                    voltagem / 1000f
            ).append(
                    " V\n"
            );
        }

        if (tecnologia != null &&
                !tecnologia.isEmpty()) {

            resposta.append(
                    "Tecnologia: "
            ).append(
                    tecnologia
            ).append("\n");
        }

        resposta.append(
                "\nObservação: o estado de saúde informado pelo " +
                        "Android não representa necessariamente uma " +
                        "porcentagem de desgaste da bateria."
        );

        return resposta.toString();
    }

    // ============================================================
    // TEMPERATURA DA BATERIA
    // ============================================================

    private String obterTemperaturaBateria() {

        Intent bateria =
                registerReceiver(
                        null,
                        new IntentFilter(
                                Intent.ACTION_BATTERY_CHANGED
                        )
                );

        if (bateria == null) {

            return "Não foi possível consultar a temperatura.";
        }

        int temperatura =
                bateria.getIntExtra(
                        BatteryManager.EXTRA_TEMPERATURE,
                        -1
                );

        if (temperatura < 0) {

            return "O Android não disponibilizou a temperatura da bateria.";
        }

        return "Temperatura da bateria: " +
                (temperatura / 10f) +
                " °C.";
    }

    // ============================================================
    // RAM
    // ============================================================

    private String obterMemoriaRAM() {

        ActivityManager manager =
                (ActivityManager)
                        getSystemService(
                                ACTIVITY_SERVICE
                        );

        if (manager == null) {

            return "Não foi possível consultar a memória RAM.";
        }

        ActivityManager.MemoryInfo info =
                new ActivityManager.MemoryInfo();

        manager.getMemoryInfo(info);

        long total =
                info.totalMem;

        long disponivel =
                info.availMem;

        long usada =
                total -
                        disponivel;

        return
                "Memória RAM:\n" +
                        "Total: " +
                        formatarBytes(total) +
                        "\n" +
                        "Em uso: " +
                        formatarBytes(usada) +
                        "\n" +
                        "Disponível: " +
                        formatarBytes(disponivel);
    }

    // ============================================================
    // ARMAZENAMENTO
    // ============================================================

    private String obterArmazenamento() {

        try {

            StatFs stat =
                    new StatFs(
                            Environment
                                    .getDataDirectory()
                                    .getPath()
                    );

            long total =
                    stat.getTotalBytes();

            long livre =
                    stat.getAvailableBytes();

            long usado =
                    total -
                            livre;

            return
                    "Armazenamento interno:\n" +
                            "Total: " +
                            formatarBytes(total) +
                            "\n" +
                            "Usado: " +
                            formatarBytes(usado) +
                            "\n" +
                            "Livre: " +
                            formatarBytes(livre);

        } catch (Exception e) {

            return "Não foi possível consultar o armazenamento.";
        }
    }

    // ============================================================
    // REDE
    // ============================================================

    private String obterInformacoesRede() {

        ConnectivityManager manager =
                (ConnectivityManager)
                        getSystemService(
                                CONNECTIVITY_SERVICE
                        );

        if (manager == null) {

            return "Não foi possível consultar a conectividade.";
        }

        Network rede =
                manager.getActiveNetwork();

        if (rede == null) {

            return "Nenhuma conexão de rede ativa foi detectada.";
        }

        NetworkCapabilities capacidades =
                manager.getNetworkCapabilities(
                        rede
                );

        if (capacidades == null) {

            return "Não foi possível determinar o tipo da conexão.";
        }

        String tipo;

        if (capacidades.hasTransport(
                NetworkCapabilities.TRANSPORT_WIFI
        )) {

            tipo = "Wi-Fi";

        } else if (capacidades.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR
        )) {

            tipo = "dados móveis";

        } else if (capacidades.hasTransport(
                NetworkCapabilities.TRANSPORT_ETHERNET
        )) {

            tipo = "Ethernet";

        } else {

            tipo = "outro";
        }

        boolean validada =
                capacidades.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED
                );

        return
                "Conectividade:\n" +
                        "Tipo: " +
                        tipo +
                        "\n" +
                        "Internet validada pelo Android: " +
                        (validada
                                ? "sim"
                                : "não");
    }

    // ============================================================
    // STATUS COMPLETO
    // ============================================================

    private String obterStatusCompleto() {

        StringBuilder resposta =
                new StringBuilder();

        resposta.append(
                "Análise do telefone:\n\n"
        );

        resposta.append(
                obterBateria()
        );

        resposta.append(
                "\n\n"
        );

        resposta.append(
                obterTemperaturaBateria()
        );

        resposta.append(
                "\n\n"
        );

        resposta.append(
                obterMemoriaRAM()
        );

        resposta.append(
                "\n\n"
        );

        resposta.append(
                obterArmazenamento()
        );

        resposta.append(
                "\n\n"
        );

        resposta.append(
                obterInformacoesRede()
        );

        resposta.append(
                "\n\n"
        );

        resposta.append(
                "Data: "
        );

        resposta.append(
                new SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                ).format(
                        new Date()
                )
        );

        resposta.append(
                "\nHora: "
        );

        resposta.append(
                new SimpleDateFormat(
                        "HH:mm:ss",
                        Locale.getDefault()
                ).format(
                        new Date()
                )
        );

        resposta.append(
                "\n\n"
        );

        resposta.append(
                "A verificação não é um antivírus e não confirma " +
                        "a existência ou ausência de malware."
        );

        return resposta.toString();
    }

    // ============================================================
    // CÁLCULOS SIMPLES
    // ============================================================

    private String tentarCalculoSimples(
            String textoOriginal
    ) {

        String texto =
                normalizar(
                        textoOriginal
                );

        String expressao =
                texto
                        .replace(
                                "quanto e",
                                ""
                        )
                        .replace(
                                "quanto é",
                                ""
                        )
                        .replace(
                                "calcule",
                                ""
                        )
                        .replace(
                                "calcula",
                                ""
                        )
                        .replace(
                                "resultado de",
                                ""
                        )
                        .trim();

        if (!expressao.matches(
                "[0-9\\s+\\-*/().]+"
        )) {

            return null;
        }

        try {

            if (expressao.contains("+")) {

                String[] partes =
                        expressao.split(
                                "\\+"
                        );

                if (partes.length == 2) {

                    double a =
                            Double.parseDouble(
                                    partes[0].trim()
                            );

                    double b =
                            Double.parseDouble(
                                    partes[1].trim()
                            );

                    return formatarResultado(
                            a + b
                    );
                }
            }

            if (expressao.contains("-")) {

                String[] partes =
                        expressao.split(
                                "\\-"
                        );

                if (partes.length == 2) {

                    double a =
                            Double.parseDouble(
                                    partes[0].trim()
                            );

                    double b =
                            Double.parseDouble(
                                    partes[1].trim()
                            );

                    return formatarResultado(
                            a - b
                    );
                }
            }

            if (expressao.contains("*")) {

                String[] partes =
                        expressao.split(
                                "\\*"
                        );

                if (partes.length == 2) {

                    double a =
                            Double.parseDouble(
                                    partes[0].trim()
                            );

                    double b =
                            Double.parseDouble(
                                    partes[1].trim()
                            );

                    return formatarResultado(
                            a * b
                    );
                }
            }

            if (expressao.contains("/")) {

                String[] partes =
                        expressao.split(
                                "/"
                        );

                if (partes.length == 2) {

                    double a =
                            Double.parseDouble(
                                    partes[0].trim()
                            );

                    double b =
                            Double.parseDouble(
                                    partes[1].trim()
                            );

                    if (b == 0) {

                        return "Não é possível dividir por zero.";
                    }

                    return formatarResultado(
                            a / b
                    );
                }
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    private String formatarResultado(
            double resultado
    ) {

        if (resultado ==
                Math.rint(resultado)) {

            return "O resultado é " +
                    (long) resultado +
                    ".";
        }

        return "O resultado é " +
                resultado +
                ".";
    }

    // ============================================================
    // FORMATAR BYTES
    // ============================================================

    private String formatarBytes(
            long bytes
    ) {

        if (bytes < 1024) {

            return bytes + " B";
        }

        double kb =
                bytes / 1024.0;

        if (kb < 1024) {

            return String.format(
                    Locale.getDefault(),
                    "%.1f KB",
                    kb
            );
        }

        double mb =
                kb / 1024.0;

        if (mb < 1024) {

            return String.format(
                    Locale.getDefault(),
                    "%.1f MB",
                    mb
            );
        }

        double gb =
                mb / 1024.0;

        return String.format(
                Locale.getDefault(),
                "%.2f GB",
                gb
        );
    }

    // ============================================================
    // ELEMENTOS DE MENU
    // ============================================================

    private LinearLayout criarLayoutMenu() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setBackgroundColor(
                Color.BLACK
        );

        layout.setPadding(
                dp(16),
                dp(20),
                dp(16),
                dp(20)
        );

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(layout);

        // A tela retornada é o próprio layout interno.
        // O conteúdo continua rolável através dos itens.
        return layout;
    }

    private void adicionarTituloMenu(
            LinearLayout layout,
            String titulo
    ) {

        TextView texto =
                criarTexto(
                        titulo,
                        20,
                        Color.WHITE
                );

        texto.setGravity(
                Gravity.CENTER
        );

        texto.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        layout.addView(
                texto,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(60)
                )
        );
    }

    private void adicionarTextoConfiguracao(
            LinearLayout layout,
            String titulo,
            String valor
    ) {

        TextView texto =
                criarTexto(
                        titulo +
                                "\n" +
                                valor,
                        14,
                        Color.LTGRAY
                );

        texto.setPadding(
                dp(12),
                dp(14),
                dp(12),
                dp(14)
        );

        layout.addView(
                texto,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );
    }

    private void adicionarBotaoTela(
            LinearLayout layout,
            String texto,
            Runnable acao
    ) {

        Button botao =
                criarBotao(
                        texto
                );

        botao.setOnClickListener(
                v -> acao.run()
        );

        layout.addView(
                botao,
                parametrosBotao()
        );
    }

    private Button criarBotao(
            String texto
    ) {

        Button botao =
                new Button(this);

        botao.setText(texto);

        botao.setTextColor(
                Color.WHITE
        );

        botao.setAllCaps(false);

        botao.setBackgroundColor(
                Color.rgb(
                        25,
                        25,
                        25
                )
        );

        return botao;
    }

    private LinearLayout.LayoutParams parametrosBotao() {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(54)
                );

        params.setMargins(
                0,
                dp(5),
                0,
                dp(5)
        );

        return params;
    }

    private TextView criarTexto(
            String texto,
            float tamanho,
            int cor
    ) {

        TextView view =
                new TextView(this);

        view.setText(texto);

        view.setTextSize(
                tamanho
        );

        view.setTextColor(
                cor
        );

        return view;
    }

    // ============================================================
    // VOLTAR PARA A TELA PRINCIPAL
    // ============================================================

    private void voltarTela() {

        configurarTela();
    }

    // ============================================================
    // RELÓGIO
    // ============================================================

    private void iniciarRelogio() {

        clockHandler =
                new Handler();

        Runnable atualizar =
                new Runnable() {

                    @Override
                    public void run() {

                        if (clockText != null) {

                            clockText.setText(
                                    new SimpleDateFormat(
                                            "HH:mm:ss",
                                            Locale.getDefault()
                                    ).format(
                                            new Date()
                                    )
                            );
                        }

                        if (clockHandler != null) {

                            clockHandler.postDelayed(
                                    this,
                                    1000
                            );
                        }
                    }
                };

        clockHandler.post(
                atualizar
        );
    }

    // ============================================================
    // REATOR ARC
    // ============================================================

    private class ReactorView
            extends View {

        private final Paint paint =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        public ReactorView(
                android.content.Context context
        ) {

            super(context);

            setLayerType(
                    View.LAYER_TYPE_SOFTWARE,
                    null
            );
        }

        @Override
        protected void onDraw(
                Canvas canvas
        ) {

            super.onDraw(canvas);

            float cx =
                    getWidth() / 2f;

            float cy =
                    getHeight() / 2f;

            float raio =
                    Math.min(
                            getWidth(),
                            getHeight()
                    ) * 0.32f;

            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setStrokeWidth(
                    dp(5)
            );

            paint.setColor(
                    Color.WHITE
            );

            paint.setShadowLayer(
                    dp(12),
                    0,
                    0,
                    Color.WHITE
            );

            canvas.drawCircle(
                    cx,
                    cy,
                    raio,
                    paint
            );

            paint.clearShadowLayer();

            paint.setStrokeWidth(
                    dp(2)
            );

            paint.setColor(
                    Color.GRAY
            );

            canvas.drawCircle(
                    cx,
                    cy,
                    raio * 0.72f,
                    paint
            );

            paint.setStyle(
                    Paint.Style.FILL
            );

            paint.setColor(
                    Color.WHITE
            );

            paint.setShadowLayer(
                    dp(20),
                    0,
                    0,
                    Color.WHITE
            );

            canvas.drawCircle(
                    cx,
                    cy,
                    raio * 0.40f,
                    paint
            );

            paint.clearShadowLayer();
        }
    }
 }
