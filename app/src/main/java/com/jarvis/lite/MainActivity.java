package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQUEST_AUDIO = 100;

    private TextView clockText;
    private LinearLayout chatContainer;
    private ScrollView chatScroll;
    private EditText commandInput;
    private View reactorArea;
    private ReactorView reactorView;

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

    private final ArrayList<MensagemHistorico> historico =
            new ArrayList<>();

    private boolean telaPrincipalLimpa = false;

    private static class MensagemHistorico {

        String autor;
        String mensagem;
        long horario;

        MensagemHistorico(
                String autor,
                String mensagem,
                long horario) {

            this.autor = autor;
            this.mensagem = mensagem;
            this.horario = horario;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
        }

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        try {
            stopService(
                    new Intent(
                            this,
                            JarvisVoiceService.class
                    )
            );
        } catch (Exception e) {
        }

        preferencias = getSharedPreferences(
                "jarvis_config",
                MODE_PRIVATE
        );

        ultimaVerificacao =
                preferencias.getLong(
                        "ultima_verificacao",
                        0L
                );

        // Sempre inicia em modo offline.
        modoOnline = false;

        carregarConfiguracoes();
        carregarHistorico();

        telaPrincipalLimpa =
                preferencias.getBoolean(
                        "tela_principal_limpa",
                        false
                );

        configurarTela();
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
    }

    private void carregarConfiguracoes() {

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
                                                |
                                        WindowInsets.Type.navigationBars()
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

    private void configurarTela() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.BLACK
        );

        configurarAreaSegura(root);

        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.HORIZONTAL
        );

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        Button menu =
                new Button(this);

        menu.setText("☰");
        menu.setTextSize(20);

        menu.setOnClickListener(
                v -> abrirMenuHistorico()
        );

        header.addView(
                menu,
                new LinearLayout.LayoutParams(
                        dp(52),
                        dp(48)
                )
        );

        TextView titulo =
                new TextView(this);

        titulo.setText("J.A.R.V.I.S");
        titulo.setTextSize(24);
        titulo.setTextColor(Color.WHITE);
        titulo.setGravity(Gravity.CENTER);

        header.addView(
                titulo,
                new LinearLayout.LayoutParams(
                        0,
                        dp(48),
                        1
                )
        );

        Button configuracoes =
                new Button(this);

        configuracoes.setText("⚙");
        configuracoes.setTextSize(18);

        configuracoes.setOnClickListener(
                v -> abrirMenuJarvis()
        );

        header.addView(
                configuracoes,
                new LinearLayout.LayoutParams(
                        dp(58),
                        dp(48)
                )
        );

        root.addView(
                header,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(48)
                )
        );

        TextView subtitulo =
                new TextView(this);

        subtitulo.setText(
                "ASSISTENTE LOCAL"
        );

        subtitulo.setTextSize(11);
        subtitulo.setTextColor(Color.GRAY);
        subtitulo.setGravity(
                Gravity.CENTER_VERTICAL
        );

        root.addView(
                subtitulo,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(24)
                )
        );

        clockText =
                new TextView(this);

        clockText.setTextSize(21);
        clockText.setTextColor(Color.WHITE);
        clockText.setGravity(
                Gravity.CENTER
        );

        root.addView(
                clockText,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(50)
                )
        );

        reactorArea =
                new LinearLayout(this);

        ((LinearLayout) reactorArea)
                .setOrientation(
                        LinearLayout.VERTICAL
                );

        ((LinearLayout) reactorArea)
                .setGravity(
                        Gravity.CENTER
                );

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

        ((LinearLayout) reactorArea)
                .addView(
                        reactorView,
                        new LinearLayout.LayoutParams(
                                -1,
                                dp(190)
                        )
                );

        TextView modo =
                new TextView(this);

        modo.setText(
                modoOnline
                        ? "● MODO ONLINE SELECIONADO"
                        : "● MODO OFFLINE ATIVO"
        );

        modo.setTextSize(13);
        modo.setTextColor(Color.WHITE);
        modo.setGravity(
                Gravity.CENTER
        );

        ((LinearLayout) reactorArea)
                .addView(
                        modo,
                        new LinearLayout.LayoutParams(
                                -1,
                                dp(30)
                        )
                );

        root.addView(
                reactorArea,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(220)
                )
        );

        chatScroll =
                new ScrollView(this);

        chatScroll.setFillViewport(true);
        chatScroll.setClipToPadding(false);

        chatContainer =
                new LinearLayout(this);

        chatContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        chatContainer.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(12)
        );

        chatScroll.addView(
                chatContainer
        );

        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                );

        scrollParams.topMargin = dp(4);
        scrollParams.bottomMargin = dp(4);

        root.addView(
                chatScroll,
                scrollParams
        );

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

        commandInput.setSingleLine(true);

        commandInput.setOnFocusChangeListener(
                (v, temFoco) -> {

                    if (temFoco) {

                        chatScroll.postDelayed(
                                () ->
                                        chatScroll.fullScroll(
                                                View.FOCUS_DOWN
                                        ),
                                250
                        );
                    }
                }
        );

        entrada.addView(
                commandInput,
                new LinearLayout.LayoutParams(
                        0,
                        dp(48),
                        1
                )
        );

        Button microfone =
                new Button(this);

        microfone.setText("🎙");
        microfone.setTextSize(16);

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
                        dp(58),
                        dp(48)
                )
        );

        root.addView(
                entrada,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(48)
                )
        );

        Button executar =
                new Button(this);

        executar.setText(
                "ENVIAR"
        );

        executar.setOnClickListener(
                v -> enviarTexto()
        );

        root.addView(
                executar,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(44)
                )
        );

        setContentView(root);

        if (telaPrincipalLimpa
                || historico.isEmpty()) {

            mostrarReator();

        } else {

            esconderReator();

            renderizarHistoricoPrincipal();
        }
    }

    private void enviarTexto() {

        if (commandInput == null) {
            return;
        }

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

        commandInput.setText("");

        processarComando(
                comando
        );
    }

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

        esconderReator();
    }

    /*
     * ============================================================
     * HISTÓRICO CONTÍNUO
     * ============================================================
     */

    private void carregarHistorico() {

        historico.clear();

        String dados =
                preferencias.getString(
                        "historico_conversa",
                        ""
                );

        if (dados.trim().isEmpty()) {
            return;
        }

        String[] registros =
                dados.split(
                        "\\n",
                        -1
                );

        for (String registro :
                registros) {

            try {

                String[] partes =
                        registro.split(
                                "\\|",
                                3
                        );

                if (partes.length != 3) {
                    continue;
                }

                String autor =
                        decodificar(
                                partes[0]
                        );

                String mensagem =
                        decodificar(
                                partes[1]
                        );

                long horario =
                        Long.parseLong(
                                partes[2]
                        );

                historico.add(
                        new MensagemHistorico(
                                autor,
                                mensagem,
                                horario
                        )
                );

            } catch (Exception e) {
            }
        }
    }

    private String codificar(
            String texto) {

        return Base64.encodeToString(
                texto.getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                ),
                Base64.NO_WRAP
        );
    }

    private String decodificar(
            String texto) {

        return new String(
                Base64.decode(
                        texto,
                        Base64.NO_WRAP
                ),
                java.nio.charset.StandardCharsets.UTF_8
        );
    }

    private void salvarHistorico() {

        StringBuilder dados =
                new StringBuilder();

        for (MensagemHistorico item :
                historico) {

            if (dados.length() > 0) {
                dados.append("\n");
            }

            dados.append(
                    codificar(item.autor)
            )
                    .append("|")
                    .append(
                            codificar(
                                    item.mensagem
                            )
                    )
                    .append("|")
                    .append(
                            item.horario
                    );
        }

        preferencias.edit()
                .putString(
                        "historico_conversa",
                        dados.toString()
                )
                .apply();
    }

    private void adicionarAoHistorico(
            String autor,
            String mensagem) {

        historico.add(
                new MensagemHistorico(
                        autor,
                        mensagem,
                        System.currentTimeMillis()
                )
        );

        salvarHistorico();
    }

    private void renderizarHistoricoPrincipal() {

        if (chatContainer == null) {
            return;
        }

        chatContainer.removeAllViews();

        for (MensagemHistorico item :
                historico) {

            adicionarBalaoVisual(
                    item.autor,
                    item.mensagem,
                    false
            );
        }

        chatScroll.postDelayed(
                () ->
                        chatScroll.fullScroll(
                                View.FOCUS_DOWN
                        ),
                80
        );
    }

    private void limparTelaPrincipal() {

        new AlertDialog.Builder(this)
                .setTitle(
                        "Limpar conversas"
                )
                .setMessage(
                        "Isso limpará somente a conversa exibida na tela principal. O histórico continuará salvo."
                )
                .setNegativeButton(
                        "CANCELAR",
                        null
                )
                .setPositiveButton(
                        "SIM",
                        (dialog, which) -> {

                            telaPrincipalLimpa =
                                    true;

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
                )
                .show();
    }

    /*
     * ============================================================
     * MENU ☰
     * ============================================================
     */

    private void abrirMenuHistorico() {

        LinearLayout layout =
                criarTelaBase(
                        "HISTÓRICO",
                        "CONVERSA CONTÍNUA"
                );

        adicionarBotaoTela(
                layout,
                "🕘  HISTÓRICO DE CONVERSA",
                this::abrirHistoricoCompleto
        );

        adicionarBotaoTela(
                layout,
                "📤  EXPORTAR CONVERSAS",
                this::exportarHistorico
        );

        adicionarBotaoTela(
                layout,
                "🧹  LIMPAR CONVERSAS",
                this::limparTelaPrincipal
        );

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::voltarTela
        );

        setContentView(layout);
    }

    private void abrirHistoricoCompleto() {

        LinearLayout layout =
                criarTelaBase(
                        "HISTÓRICO DE CONVERSA",
                        "TODAS AS MENSAGENS EM ORDEM CRONOLÓGICA"
                );

        ScrollView scroll =
                new ScrollView(this);

        LinearLayout lista =
                new LinearLayout(this);

        lista.setOrientation(
                LinearLayout.VERTICAL
        );

        if (historico.isEmpty()) {

            TextView vazio =
                    criarTexto(
                            "Nenhuma mensagem foi registrada ainda."
                    );

            vazio.setTextColor(
                    Color.GRAY
            );

            lista.addView(
                    vazio,
                    parametrosTexto()
            );

        } else {

            for (int i = 0;
                 i < historico.size();
                 i++) {

                adicionarItemHistorico(
                        lista,
                        i
                );
            }
        }

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
                "VOLTAR",
                this::abrirMenuHistorico
        );

        setContentView(layout);
    }

    private void adicionarItemHistorico(
            LinearLayout lista,
            int indice) {

        MensagemHistorico item =
                historico.get(indice);

        LinearLayout card =
                criarCard();

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        TextView autor =
                criarTexto(
                        item.autor
                                + " • "
                                + new SimpleDateFormat(
                                        "dd/MM/yyyy HH:mm:ss",
                                        Locale.getDefault()
                                ).format(
                                        new Date(
                                                item.horario
                                        )
                                )
                );

        autor.setTextSize(11);
        autor.setTextColor(Color.GRAY);

        TextView mensagem =
                criarTexto(
                        item.mensagem
                );

        mensagem.setTextSize(14);

        card.addView(autor);
        card.addView(mensagem);

        card.setOnLongClickListener(
                v -> {

                    abrirSelecaoHistorico();

                    return true;
                }
        );

        lista.addView(
                card,
                parametrosCard()
        );
    }

    /*
     * ============================================================
     * SELEÇÃO DO HISTÓRICO
     * ============================================================
     */

    private void abrirSelecaoHistorico() {

        final HashSet<Integer>
                selecionados =
                new HashSet<>();

        LinearLayout layout =
                criarTelaBase(
                        "SELECIONAR CONVERSAS",
                        "TOQUE NAS MENSAGENS PARA SELECIONAR"
                );

        TextView contador =
                criarTexto(
                        "Selecionadas: 0"
                );

        contador.setTextColor(
                Color.LTGRAY
        );

        layout.addView(
                contador,
                parametrosTexto()
        );

        ScrollView scroll =
                new ScrollView(this);

        LinearLayout lista =
                new LinearLayout(this);

        lista.setOrientation(
                LinearLayout.VERTICAL
        );

        for (int i = 0;
             i < historico.size();
             i++) {

            final int indice = i;

            MensagemHistorico item =
                    historico.get(i);

            LinearLayout linha =
                    criarCard();

            Button circulo =
                    criarBotao("○");

            circulo.setTextSize(22);

            circulo.setOnClickListener(
                    v -> {

                        if (selecionados.contains(
                                indice
                        )) {

                            selecionados.remove(
                                    indice
                            );

                            circulo.setText("○");

                        } else {

                            selecionados.add(
                                    indice
                            );

                            circulo.setText("●");
                        }

                        contador.setText(
                                "Selecionadas: "
                                        +
                                selecionados.size()
                        );
                    }
            );

            TextView texto =
                    criarTexto(
                            item.autor
                                    + " — "
                                    + item.mensagem
                    );

            texto.setTextSize(13);

            linha.addView(
                    circulo,
                    new LinearLayout.LayoutParams(
                            dp(55),
                            dp(50)
                    )
            );

            linha.addView(
                    texto,
                    new LinearLayout.LayoutParams(
                            0,
                            -2,
                            1
                    )
            );

            lista.addView(
                    linha,
                    parametrosCard()
            );
        }

        scroll.addView(lista);

        layout.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        Button apagar =
                criarBotao(
                        "🗑  APAGAR SELECIONADAS"
                );

        apagar.setOnClickListener(
                v -> {

                    if (selecionados.isEmpty()) {

                        Toast.makeText(
                                this,
                                "Nenhuma mensagem selecionada.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    confirmarExclusaoHistorico(
                            selecionados
                    );
                }
        );

        layout.addView(
                apagar,
                parametrosBotao()
        );

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::abrirHistoricoCompleto
        );

        setContentView(layout);
    }

    private void confirmarExclusaoHistorico(
            HashSet<Integer> selecionados) {

        new AlertDialog.Builder(this)
                .setTitle(
                        "Apagar conversas"
                )
                .setMessage(
                        "Você tem certeza que quer apagar as conversas selecionadas?"
                )
                .setNegativeButton(
                        "CANCELAR",
                        null
                )
                .setPositiveButton(
                        "SIM",
                        (dialog, which) -> {

                            ArrayList<Integer>
                                    indices =
                                    new ArrayList<>(
                                            selecionados
                                    );

                            java.util.Collections.sort(
                                    indices,
                                    java.util.Collections.reverseOrder()
                            );

                            for (Integer indice :
                                    indices) {

                                if (indice >= 0
                                        && indice <
                                        historico.size()) {

                                    historico.remove(
                                            (int) indice
                                    );
                                }
                            }

                            salvarHistorico();

                            Toast.makeText(
                                    this,
                                    "Mensagens selecionadas apagadas.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            abrirHistoricoCompleto();
                        }
                )
                .show();
    }

    /*
     * ============================================================
     * EXPORTAÇÃO
     * ============================================================
     */

    private void exportarHistorico() {

        if (historico.isEmpty()) {

            Toast.makeText(
                    this,
                    "Não há conversas para exportar.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        StringBuilder texto =
                new StringBuilder();

        texto.append(
                "J.A.R.V.I.S — Histórico de conversa\n\n"
        );

        for (MensagemHistorico item :
                historico) {

            texto.append(
                    new SimpleDateFormat(
                            "dd/MM/yyyy HH:mm:ss",
                            Locale.getDefault()
                    ).format(
                            new Date(
                                    item.horario
                            )
                    )
            );

            texto.append(
                    " — "
            )
                    .append(item.autor)
                    .append(":\n")
                    .append(item.mensagem)
                    .append("\n\n");
        }

        Intent compartilhar =
                new Intent(
                        Intent.ACTION_SEND
                );

        compartilhar.setType(
                "text/plain"
        );

        compartilhar.putExtra(
                Intent.EXTRA_SUBJECT,
                "Histórico JARVIS"
        );

        compartilhar.putExtra(
                Intent.EXTRA_TEXT,
                texto.toString()
        );

        startActivity(
                Intent.createChooser(
                        compartilhar,
                        "Exportar histórico"
                )
        );
    }

    /*
     * ============================================================
     * MENSAGENS
     * ============================================================
     */

    private void adicionarMensagem(
            String autor,
            String mensagem) {

        marcarTelaComMensagem();

        adicionarAoHistorico(
                autor,
                mensagem
        );

        adicionarBalaoVisual(
                autor,
                mensagem,
                true
        );
    }

    private void adicionarBalaoVisual(
            String autor,
            String mensagem,
            boolean animar) {

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
                        ? Color.rgb(
                                34,
                                34,
                                34
                        )
                        : Color.rgb(
                                18,
                                18,
                                18
                        )
        );

        fundo.setStroke(
                dp(1),
                Color.rgb(
                        65,
                        65,
                        65
                )
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

        if (animar) {

            android.view.animation.AlphaAnimation
                    aparecer =
                    new android.view.animation.AlphaAnimation(
                            0f,
                            1f
                    );

            aparecer.setDuration(220);

            android.view.animation.TranslateAnimation
                    subir =
                    new android.view.animation.TranslateAnimation(
                            0,
                            0,
                            dp(10),
                            0
                    );

            subir.setDuration(220);

            android.view.animation.AnimationSet
                    entrada =
                    new android.view.animation.AnimationSet(
                            true
                    );

            entrada.addAnimation(aparecer);
            entrada.addAnimation(subir);

            linha.startAnimation(
                    entrada
            );
        }

        chatScroll.postDelayed(
                () ->
                        chatScroll.fullScroll(
                                View.FOCUS_DOWN
                        ),
                80
        );
    }
        /*
     * ============================================================
     * CONFIGURAÇÕES
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

    private void abrirGerenciarJarvis() {

        carregarConfiguracoes();

        LinearLayout layout =
                criarTelaBase(
                        "GERENCIAR JARVIS",
                        "CONTROLES DO ASSISTENTE"
                );

        TextView permissoes =
                criarTexto(
                        "🔐 PERMISSÕES"
                );

        permissoes.setTextSize(18);
        permissoes.setTextColor(Color.WHITE);

        layout.addView(
                permissoes,
                parametrosTexto()
        );

        adicionarControleInterno(
                layout,
                "🎙  MICROFONE",
                "Permite reconhecimento de voz.",
                () -> microfoneAtivado,
                valor -> {

                    microfoneAtivado =
                            valor;

                    preferencias.edit()
                            .putBoolean(
                                    "microfone_ativo",
                                    valor
                            )
                            .apply();

                    if (!valor) {
                        pararReconhecimento();
                    }
                }
        );

        adicionarControleInterno(
                layout,
                "🔔  NOTIFICAÇÕES",
                "Controle interno das notificações do JARVIS.",
                () -> notificacoesAtivadas,
                valor -> {

                    notificacoesAtivadas =
                            valor;

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
                "Permite consultar informações disponíveis do aparelho.",
                () -> statusAtivado,
                valor -> {

                    statusAtivado =
                            valor;

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
                "Controle interno do recurso. Nenhuma permissão nova é concedida automaticamente.",
                () -> arquivosMidiaAtivados,
                valor -> {

                    arquivosMidiaAtivados =
                            valor;

                    preferencias.edit()
                            .putBoolean(
                                    "arquivos_midia_ativos",
                                    valor
                            )
                            .apply();
                }
        );

        TextView conexoes =
                criarTexto(
                        "🌐 CONEXÕES"
                );

        conexoes.setTextSize(18);
        conexoes.setTextColor(Color.WHITE);

        layout.addView(
                conexoes,
                parametrosTexto()
        );

        adicionarModoConexao(
                layout,
                "🔴 Modo Offline",
                false
        );

        adicionarModoConexao(
                layout,
                "🟢 Modo Online",
                true
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

        ScrollView scroll =
                new ScrollView(this);

        scroll.setFillViewport(
                true
        );

        scroll.addView(
                layout,
                new ScrollView.LayoutParams(
                        -1,
                        -2
                )
        );

        setContentView(scroll);
    }

    private void adicionarModoConexao(
            LinearLayout layout,
            String titulo,
            boolean online) {

        LinearLayout card =
                criarCard();

        LinearLayout textos =
                new LinearLayout(this);

        textos.setOrientation(
                LinearLayout.VERTICAL
        );

        TextView nome =
                criarTexto(
                        titulo
                );

        nome.setTextSize(15);

        TextView descricao =
                criarTexto(
                        online
                                ? "Seleciona o modo online. Esta versão não possui permissão INTERNET, portanto nenhum acesso de rede é iniciado."
                                : "Modo local padrão. O JARVIS não utiliza Internet."
                );

        descricao.setTextSize(12);
        descricao.setTextColor(Color.GRAY);

        textos.addView(nome);
        textos.addView(descricao);

        Button acao =
                criarBotao(
                        (modoOnline == online)
                                ? "EM USO"
                                : "ATIVAR"
                );

        acao.setTextSize(10);

        acao.setOnClickListener(
                v -> {

                    if (modoOnline == online) {
                        return;
                    }

                    modoOnline =
                            online;

                    preferencias.edit()
                            .putBoolean(
                                    "modo_online",
                                    modoOnline
                            )
                            .apply();

                    abrirGerenciarJarvis();
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
                acao,
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
                criarTexto(
                        titulo
                );

        tituloView.setTextSize(15);

        TextView descView =
                criarTexto(
                        descricao
                );

        descView.setTextSize(12);
        descView.setTextColor(Color.GRAY);

        textos.addView(
                tituloView
        );

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

                    alterar.set(
                            novo
                    );

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

    /*
     * ============================================================
     * VOZ
     * ============================================================
     */

    private void abrirGerenciarVoz() {

        LinearLayout layout =
                criarTelaBase(
                        "VOZ DO JARVIS",
                        "ESCOLHA UMA VOZ TTS DISPONÍVEL"
                );

        TextView info =
                criarTexto(
                        "Escolha uma voz instalada no aparelho. "
                                +
                        "A velocidade permanece normal e o tom será ajustado para um perfil mais grave."
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

            for (Voice voz :
                    tts.getVoices()) {

                if (voz == null
                        || voz.getLocale() == null) {
                    continue;
                }

                Locale local =
                        voz.getLocale();

                if ("pt".equalsIgnoreCase(
                        local.getLanguage()
                )
                        &&
                        "BR".equalsIgnoreCase(
                                local.getCountry()
                        )) {

                    vozes.add(
                            voz
                    );
                }
            }

            if (vozes.isEmpty()) {

                TextView nenhuma =
                        criarTexto(
                                "Nenhuma voz pt-BR foi encontrada."
                        );

                nenhuma.setTextColor(
                        Color.GRAY
                );

                lista.addView(
                        nenhuma,
                        parametrosTexto()
                );

            } else {

                for (Voice voz :
                        vozes) {

                    Button escolha =
                            criarBotao(
                                    rotuloVoz(
                                            voz.getName()
                                    )
                                            +
                                    "\n"
                                            +
                                    voz.getName()
                            );

                    escolha.setTextSize(11);

                    escolha.setOnClickListener(
                            v -> {

                                if (tts != null) {

                                    tts.setVoice(
                                            voz
                                    );

                                    tts.setSpeechRate(
                                            1.0f
                                    );

                                    tts.setPitch(
                                            0.85f
                                    );

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

        scroll.addView(
                lista
        );

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
                () ->
                        falar(
                                "À sua disposição. Sistemas locais operacionais."
                        )
        );

        adicionarBotaoTela(
                layout,
                "VOLTAR",
                this::abrirGerenciarJarvis
        );

        setContentView(layout);
    }

    private String rotuloVoz(
            String nome) {

        String n =
                nome.toLowerCase(
                        Locale.getDefault()
                );

        if (n.contains("male")
                || n.contains("mascul")) {

            return "🎙 VOZ MASCULINA";
        }

        if (n.contains("female")
                || n.contains("feminin")) {

            return "🎙 VOZ FEMININA";
        }

        return "🎙 VOZ DISPONÍVEL";
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
                                +
                        "Nenhuma ameaça pode ser confirmada por esta verificação básica.\n\n"
                                +
                        "Esta função apenas consulta informações acessíveis ao aplicativo."
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
                                    +
                            "Verificação básica concluída.\n\n"
                                    +
                            "O JARVIS não encontrou informações que permitam confirmar uma ameaça.\n\n"
                                    +
                            "Esta função não substitui o Google Play Protect."
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

            PackageManager pm =
                    getPackageManager();

            for (ApplicationInfo appInfo :
                    pm.getInstalledApplications(
                            PackageManager.GET_META_DATA
                    )) {

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
                +
                horario;
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
                        "Defina a frase que deverá ser usada como comando de ativação.\n\n"
                                +
                        "Exemplo:\n"
                                +
                        "JARVIS está aí?"
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

                    Toast.makeText(
                            this,
                            "Novo comando de voz salvo.",
                            Toast.LENGTH_SHORT
                    ).show();

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
                        "O JARVIS Lite utiliza somente recursos permitidos pelo Android.\n\n"
                                +
                        "Nesta versão, o principal recurso sensível é o microfone.\n\n"
                                +
                        "O Android continua responsável pelas permissões do aplicativo.\n\n"
                                +
                        "Nenhum aplicativo comum pode garantir proteção absoluta contra todas as ameaças."
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
     * TTS
     * ============================================================
     */

    private void iniciarVoz() {

        try {

            tts =
                    new TextToSpeech(
                            getApplicationContext(),
                            status -> {

                                if (status ==
                                        TextToSpeech.SUCCESS) {

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
                                                        TextToSpeech.LANG_MISSING_DATA
                                                        &&
                                                resultado !=
                                                        TextToSpeech.LANG_NOT_SUPPORTED;

                                        if (ttsReady) {

                                            tts.setSpeechRate(
                                                    1.0f
                                            );

                                            tts.setPitch(
                                                    0.85f
                                            );

                                            aplicarVozSalva();
                                        }

                                    } catch (Exception e) {

                                        ttsReady =
                                                false;
                                    }

                                } else {

                                    ttsReady =
                                            false;
                                }
                            }
                    );

        } catch (Exception e) {

            tts = null;
            ttsReady = false;
        }
    }

    private void falar(
            String texto) {

        if (!ttsReady
                || tts == null) {
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

    private void aplicarVozSalva() {

        if (tts == null
                || !ttsReady) {
            return;
        }

        try {

            String nome =
                    preferencias.getString(
                            "voz_tts",
                            ""
                    );

            if (!nome.isEmpty()) {

                for (Voice voz :
                        tts.getVoices()) {

                    if (nome.equals(
                            voz.getName()
                    )) {

                        tts.setVoice(
                                voz
                        );

                        break;
                    }
                }
            }

            tts.setSpeechRate(
                    1.0f
            );

            tts.setPitch(
                    0.85f
            );

        } catch (Exception e) {
        }
    }

    /*
     * ============================================================
     * RECONHECIMENTO OFFLINE
     * ============================================================
     */

    private void iniciarReconhecimento() {

        if (!microfoneAtivado) {

            responder(
                    "O microfone está desativado no Gerenciar JARVIS."
            );

            return;
        }

        pararReconhecimento();

        if (Build.VERSION.SDK_INT < 31) {

            responder(
                    "O reconhecimento de voz local desta versão exige Android 12 ou superior."
            );

            return;
        }

        if (checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

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
                .isOnDeviceRecognitionAvailable(
                        this
                )) {

            responder(
                    "O reconhecimento de voz offline não está disponível neste aparelho."
            );

            return;
        }

        try {

            speechRecognizer =
                    SpeechRecognizer
                            .createOnDeviceSpeechRecognizer(
                                    this
                            );

            speechRecognizer
                    .setRecognitionListener(
                            new RecognitionListener() {

                                @Override
                                public void onReadyForSpeech(
                                        Bundle params) {

                                    ouvindo = true;

                                    if (reactorView != null) {
                                        reactorView
                                                .setOuvindo(
                                                        true
                                                );
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

                                    ouvindo =
                                            false;
                                }

                                @Override
                                public void onError(
                                        int error) {

                                    ouvindo =
                                            false;

                                    if (reactorView != null) {
                                        reactorView
                                                .setOuvindo(
                                                        false
                                                );
                                    }

                                    String mensagem;

                                    switch (error) {

                                        case SpeechRecognizer
                                                .ERROR_AUDIO:

                                            mensagem =
                                                    "Não consegui acessar o áudio.";

                                            break;

                                        case SpeechRecognizer
                                                .ERROR_NO_MATCH:

                                            mensagem =
                                                    "Não consegui entender o que foi dito.";

                                            break;

                                        case SpeechRecognizer
                                                .ERROR_SPEECH_TIMEOUT:

                                            mensagem =
                                                    "Não detectei nenhuma fala.";

                                            break;

                                        case SpeechRecognizer
                                                .ERROR_RECOGNIZER_BUSY:

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
                                                    "Não foi possível reconhecer o comando. Código: "
                                                            +
                                                    error;

                                            break;
                                    }

                                    responder(
                                            mensagem
                                    );

                                    liberarReconhecedor();
                                }

                                @Override
                                public void onResults(
                                        Bundle results) {

                                    ouvindo =
                                            false;

                                    if (reactorView != null) {
                                        reactorView
                                                .setOuvindo(
                                                        false
                                                );
                                    }

                                    ArrayList<String>
                                            resultados =
                                            results
                                                    .getStringArrayList(
                                                            SpeechRecognizer
                                                                    .RESULTS_RECOGNITION
                                                    );

                                    if (resultados ==
                                            null
                                            ||
                                            resultados
                                                    .isEmpty()) {

                                        responder(
                                                "Não consegui entender o comando."
                                        );

                                        liberarReconhecedor();

                                        return;
                                    }

                                    String comando =
                                            resultados
                                                    .get(0);

                                    adicionarMensagem(
                                            "VOCÊ",
                                            comando
                                    );

                                    liberarReconhecedor();

                                    processarComando(
                                            comando
                                    );
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
                    RecognizerIntent
                            .EXTRA_LANGUAGE,
                    "pt-BR"
            );

            intent.putExtra(
                    RecognizerIntent
                            .EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent
                            .LANGUAGE_MODEL_FREE_FORM
            );

            intent.putExtra(
                    RecognizerIntent
                            .EXTRA_PARTIAL_RESULTS,
                    false
            );

            speechRecognizer
                    .startListening(
                            intent
                    );

        } catch (Exception e) {

            ouvindo =
                    false;

            if (reactorView != null) {

                reactorView
                        .setOuvindo(
                                false
                        );
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

            reactorView.setOuvindo(
                    false
            );
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

            reactorView.setOuvindo(
                    false
            );
        }
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
                    "Comandos disponíveis: hora, data, bateria, status, privacidade, quem é você e informações sobre o histórico."
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
                            +
                    hora
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
                            +
                    data
            );

            return;
        }

        if (comando.contains("bateria")
                || comando.contains("carga")) {

            if (!statusAtivado) {

                responder(
                        "O acesso ao status do aparelho está desativado."
                );

            } else {

                responder(
                        obterBateria()
                );
            }

            return;
        }

        if (comando.contains("status")
                || comando.contains("estado")) {

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

        if (comando.contains(
                "privacidade"
        )) {

            abrirPrivacidade();

            return;
        }

        if (comando.contains(
                "verificação"
        )
                ||
                comando.contains(
                        "verificacao"
                )) {

            abrirVerificacao();

            return;
        }

        if (comando.contains(
                "quem é você"
        )
                ||
                comando.contains(
                        "quem voce e"
                )
                ||
                comando.contains(
                        "quem é voce"
                )
                ||
                comando.contains(
                        "quem voce é"
                )) {

            responder(
                    "Eu sou o JARVIS Lite, um assistente local offline-first."
            );

            return;
        }

        if (comando.contains(
                "o que você lembra"
        )
                ||
                comando.contains(
                        "o que voce lembra"
                )
                ||
                comando.contains(
                        "histórico"
                )
                ||
                comando.contains(
                        "historico"
                )) {

            if (historico.isEmpty()) {

                responder(
                        "Ainda não há mensagens no histórico."
                );

            } else {

                responder(
                        "Tenho "
                                +
                        historico.size()
                                +
                        " mensagens salvas no histórico contínuo."
                );
            }

            return;
        }

        if (comando.contains(
                "jarvis"
        )
                &&
                (
                        comando.contains(
                                "está aí"
                        )
                                ||
                        comando.contains(
                                "esta ai"
                        )
                )) {

            responder(
                    "À sua disposição. Sistemas locais operacionais."
            );

            return;
        }

        responder(
                "Não tenho uma função local para esse comando ainda."
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

            return "A bateria está em "
                    +
                    nivel
                    +
                    "%.";

        } catch (Exception e) {

            return "Não consegui obter o nível da bateria.";
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
                +
                obterBateria()
                +
                " Hora "
                +
                hora
                +
                ". Modo "
                +
                (
                        modoOnline
                                ? "ONLINE selecionado"
                                : "OFFLINE ativo"
                )
                +
                ".";
    }

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
                dp(22),
                dp(22),
                dp(22),
                dp(48)
        );

        layout.setBackgroundColor(
                Color.BLACK
        );

        if (Build.VERSION.SDK_INT >= 30) {

            layout.setOnApplyWindowInsetsListener(
                    (v, insets) -> {

                        Insets barras =
                                insets.getInsets(
                                        WindowInsets.Type.statusBars()
                                                |
                                        WindowInsets.Type.navigationBars()
                                );

                        v.setPadding(
                                dp(22),
                                dp(22) + barras.top,
                                dp(22),
                                dp(24) + barras.bottom
                        );

                        return insets;
                    }
            );

            layout.requestApplyInsets();
        }

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

        params.topMargin =
                dp(6);

        return params;
    }

    private LinearLayout.LayoutParams
    parametrosBotao() {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(46)
                );

        params.topMargin =
                dp(6);

        return params;
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
                Color.rgb(
                        15,
                        15,
                        15
                )
        );

        fundo.setCornerRadius(
                dp(14)
        );

        fundo.setStroke(
                dp(1),
                Color.rgb(
                        48,
                        48,
                        48
                )
        );

        card.setBackground(
                fundo
        );

        return card;
    }

    private LinearLayout.LayoutParams
    parametrosCard() {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        params.topMargin =
                dp(7);

        return params;
    }

    private void voltarTela() {

        configurarTela();
        iniciarRelogio();
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
                                    +
                            getPackageName()
                    )
            );

            startActivity(
                    intent
            );

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

            clockHandler
                    .removeCallbacksAndMessages(
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
                                            +
                                    "  •  "
                                            +
                                    data
                            );
                        }

                        clockHandler
                                .postDelayed(
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
     * REATOR
     * ============================================================
     */

    private class ReactorView
            extends View {

        private final Paint paint =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        private float rotacao =
                0f;

        private boolean ouvindoLocal =
                false;

        private final Handler handler =
                new Handler();

        private final Runnable animacao =
                new Runnable() {

                    @Override
                    public void run() {

                        if (ouvindoLocal) {

                            rotacao +=
                                    3.0f;

                        } else {

                            rotacao +=
                                    1.2f;
                        }

                        if (rotacao >= 360f) {

                            rotacao -=
                                    360f;
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

            super.onDraw(
                    canvas
            );

            float centroX =
                    getWidth() / 2f;

            float centroY =
                    getHeight() / 2f;

            float raioMax =
                    Math.min(
                            getWidth(),
                            getHeight()
                    )
                            *
                    0.38f;

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
                        raioMax -
                                (i * 10.5f);

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
}
