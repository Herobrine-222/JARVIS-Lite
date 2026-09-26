package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int PEDIR_MICROFONE = 100;
    private static final int PEDIR_LOCALIZACAO = 102;
    private static final int RECONHECER_VOZ = 101;

    private TextToSpeech tts;
    private SharedPreferences prefs;
    private Handler handler = new Handler();

    private TextView status;
    private TextView relogio;
    private TextView data;
    private LinearLayout menu;

    private final int AZUL = Color.rgb(41, 182, 246);
    private final int AZUL_CLARO = Color.rgb(129, 212, 250);
    private final int FUNDO = Color.rgb(3, 8, 16);
    private final int PAINEL = Color.rgb(6, 24, 39);
    private final int AMARELO = Color.rgb(255, 214, 40);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("jarvis", MODE_PRIVATE);

        prepararTTS();
        construirTelaPrincipal();
        atualizarRelogio();

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PEDIR_MICROFONE
            );
        }
    }

    private void prepararTTS() {

        tts = new TextToSpeech(this, resultado -> {

            if (resultado == TextToSpeech.SUCCESS) {

                tts.setLanguage(
                        new Locale("pt", "BR")
                );
            }
        });
    }

    private void construirTelaPrincipal() {

        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setPadding(18, 12, 18, 12);
        raiz.setBackgroundColor(FUNDO);

        LinearLayout topo = new LinearLayout(this);
        topo.setGravity(Gravity.CENTER_VERTICAL);

        TextView titulo = texto(
                "J.A.R.V.I.S",
                25,
                AZUL
        );

        titulo.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        topo.addView(
                titulo,
                new LinearLayout.LayoutParams(
                        0,
                        60,
                        1
                )
        );

        Button engrenagem = botao("⚙", 25);

        topo.addView(
                engrenagem,
                new LinearLayout.LayoutParams(
                        58,
                        58
                )
        );

        raiz.addView(topo);

        relogio = texto(
                "",
                43,
                Color.rgb(227, 242, 253)
        );

        relogio.setGravity(Gravity.CENTER);

        relogio.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        raiz.addView(relogio);

        data = texto(
                "",
                16,
                AZUL_CLARO
        );

        data.setGravity(Gravity.CENTER);

        raiz.addView(data);

        TextView clima = texto(
                "☁  Sua região",
                17,
                Color.rgb(179, 229, 252)
        );

        clima.setGravity(Gravity.CENTER);

        raiz.addView(clima);

        TextView previsao = texto(
                "Previsão do tempo",
                14,
                Color.rgb(100, 181, 246)
        );

        previsao.setGravity(Gravity.CENTER);

        raiz.addView(previsao);

        FrameLayoutReator reator =
                new FrameLayoutReator(this);

        raiz.addView(
                reator,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        TextView chatTitulo = texto(
                "JARVIS",
                15,
                AZUL_CLARO
        );

        chatTitulo.setGravity(Gravity.CENTER);

        raiz.addView(chatTitulo);

        EditText chat = new EditText(this);

        chat.setHint(
                "Digite uma mensagem para JARVIS..."
        );

        chat.setHintTextColor(
                Color.rgb(100, 150, 170)
        );

        chat.setTextColor(Color.WHITE);
        chat.setSingleLine(false);
        chat.setPadding(16, 10, 16, 10);
        chat.setBackgroundColor(PAINEL);

        raiz.addView(
                chat,
                new LinearLayout.LayoutParams(
                        -1,
                        58
                )
        );

        Button enviar = botao(
                "ENVIAR",
                14
        );

        enviar.setOnClickListener(v -> {

            String pergunta =
                    chat.getText()
                            .toString()
                            .trim();

            if (pergunta.length() == 0) {
                return;
            }

            responder(pergunta);

            chat.setText("");
        });

        raiz.addView(
                enviar,
                new LinearLayout.LayoutParams(
                        -1,
                        52
                )
        );

        status = texto(
                "À sua disposição.",
                14,
                AZUL_CLARO
        );

        status.setGravity(Gravity.CENTER);

        raiz.addView(status);

        setContentView(raiz);

        engrenagem.setOnClickListener(v -> {

            if (menu == null) {
                criarMenu();
            }

            menu.setVisibility(
                    menu.getVisibility()
                            == View.VISIBLE
                            ? View.GONE
                            : View.VISIBLE
            );
        });

        reator.setOnClickListener(
                v -> iniciarReconhecimento()
        );
    }

    private TextView texto(
            String texto,
            float tamanho,
            int cor) {

        TextView t = new TextView(this);

        t.setText(texto);
        t.setTextSize(tamanho);
        t.setTextColor(cor);

        return t;
    }

    private Button botao(
            String texto,
            float tamanho) {

        Button b = new Button(this);

        b.setText(texto);
        b.setTextSize(tamanho);
        b.setTextColor(AZUL_CLARO);
        b.setBackgroundColor(PAINEL);

        return b;
    }

    private void criarMenu() {

        menu = new LinearLayout(this);

        menu.setOrientation(
                LinearLayout.VERTICAL
        );

        menu.setPadding(8, 8, 8, 8);

        menu.setBackgroundColor(
                Color.rgb(6, 32, 42)
        );

        Button gerenciar =
                botao(
                        "⚙  Gerenciar JARVIS",
                        14
                );

        Button privacidade =
                botao(
                        "🔒  Privacidade",
                        14
                );

        Button verificacao =
                botao(
                        "🛡  Verificação do aparelho",
                        14
                );

        Button comando =
                botao(
                        "🎙  Comando de voz",
                        14
                );

        menu.addView(
                gerenciar,
                new LinearLayout.LayoutParams(
                        -1,
                        58
                )
        );

        menu.addView(
                privacidade,
                new LinearLayout.LayoutParams(
                        -1,
                        58
                )
        );

        menu.addView(
                verificacao,
                new LinearLayout.LayoutParams(
                        -1,
                        58
                )
        );

        menu.addView(
                comando,
                new LinearLayout.LayoutParams(
                        -1,
                        58
                )
        );

        addContentView(
                menu,
                new android.view.ViewGroup.LayoutParams(
                        300,
                        -2
                )
        );

        menu.setX(70);
        menu.setY(65);

        gerenciar.setOnClickListener(
                v -> abrirGerenciamento()
        );

        privacidade.setOnClickListener(
                v -> abrirPrivacidade()
        );

        verificacao.setOnClickListener(
                v -> abrirVerificacao()
        );

        comando.setOnClickListener(
                v -> abrirComandoVoz()
        );
    }

    private void abrirGerenciamento() {

        LinearLayout tela =
                baseTela("Gerenciar JARVIS");

        adicionarPermissao(
                tela,
                "🎙 Microfone",
                Manifest.permission.RECORD_AUDIO,
                PEDIR_MICROFONE
        );

        adicionarPermissao(
                tela,
                "📍 Localização aproximada",
                Manifest.permission.ACCESS_COARSE_LOCATION,
                PEDIR_LOCALIZACAO
        );

        Button voltar =
                botao("VOLTAR", 14);

        voltar.setOnClickListener(
                v -> construirTelaPrincipal()
        );

        tela.addView(voltar);

        setContentView(tela);
    }

    private void adicionarPermissao(
            LinearLayout tela,
            String nome,
            String permissao,
            int codigo) {

        LinearLayout linha =
                new LinearLayout(this);

        linha.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView nomeTexto =
                texto(
                        nome,
                        16,
                        Color.WHITE
                );

        Button botao =
                botao(
                        checkSelfPermission(permissao)
                                == PackageManager.PERMISSION_GRANTED
                                ? "PERMITIDA"
                                : "NEGADA",
                        12
                );

        botao.setOnClickListener(v -> {

            if (checkSelfPermission(permissao)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{permissao},
                        codigo
                );

            } else {

                Intent intent =
                        new Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        );

                intent.setData(
                        android.net.Uri.parse(
                                "package:"
                                        + getPackageName()
                        )
                );

                startActivity(intent);
            }
        });

        linha.addView(
                nomeTexto,
                new LinearLayout.LayoutParams(
                        0,
                        65,
                        1
                )
        );

        linha.addView(
                botao,
                new LinearLayout.LayoutParams(
                        125,
                        55
                )
        );

        tela.addView(linha);
    }

    private void abrirPrivacidade() {

        LinearLayout tela =
                baseTela("Privacidade");

        tela.addView(
                texto(
                        "Controle o que o JARVIS pode acessar.",
                        17,
                        AZUL_CLARO
                )
        );

        tela.addView(
                texto(
                        "\nO JARVIS só poderá usar recursos para os quais o Android conceder permissão.\n",
                        15,
                        Color.WHITE
                )
        );

        Button permissoes =
                botao(
                        "GERENCIAR PERMISSÕES DO APP",
                        14
                );

        permissoes.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    );

            intent.setData(
                    android.net.Uri.parse(
                            "package:"
                                    + getPackageName()
                    )
            );

            startActivity(intent);
        });

        tela.addView(permissoes);

        setContentView(tela);
    }

    private LinearLayout baseTela(
            String titulo) {

        LinearLayout tela =
                new LinearLayout(this);

        tela.setOrientation(
                LinearLayout.VERTICAL
        );

        tela.setPadding(
                22,
                22,
                22,
                22
        );

        tela.setBackgroundColor(FUNDO);

        TextView cabecalho =
                texto(
                        titulo,
                        26,
                        AZUL
                );

        cabecalho.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        tela.addView(cabecalho);

        return tela;
    }

    private void abrirComandoVoz() {

        LinearLayout tela =
                baseTela("Comando de voz");

        tela.addView(
                texto(
                        "\nEscolha uma palavra com no mínimo 4 caracteres.",
                        16,
                        AZUL_CLARO
                )
        );

        LinearLayout linha =
                new LinearLayout(this);

        linha.setGravity(
                Gravity.CENTER_VERTICAL
        );

        EditText campo =
                new EditText(this);

        campo.setHint(
                "Seu comando"
        );

        campo.setHintTextColor(
                Color.GRAY
        );

        campo.setTextColor(
                Color.WHITE
        );

        campo.setSingleLine(true);
        campo.setBackgroundColor(PAINEL);

        Button confirmar =
                botao(
                        "CONFIRMAR",
                        12
                );

        confirmar.setEnabled(false);

        linha.addView(
                campo,
                new LinearLayout.LayoutParams(
                        0,
                        60,
                        1
                )
        );

        linha.addView(
                confirmar,
                new LinearLayout.LayoutParams(
                        125,
                        60
                )
        );

        tela.addView(linha);

        TextView mensagem =
                texto(
                        "",
                        17,
                        AZUL_CLARO
                );

        mensagem.setGravity(
                Gravity.CENTER
        );

        tela.addView(mensagem);

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

                        boolean valido =
                                s.toString()
                                        .trim()
                                        .length() >= 4;

                        confirmar.setEnabled(
                                valido
                        );

                        confirmar.setTextColor(
                                valido
                                        ? Color.WHITE
                                        : Color.GRAY
                        );

                        confirmar.setBackgroundColor(
                                valido
                                        ? AZUL
                                        : Color.rgb(
                                                80,
                                                80,
                                                80
                                        )
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            android.text.Editable s) {
                    }
                }
        );

        confirmar.setOnClickListener(v -> {

            String comando =
                    campo.getText()
                            .toString()
                            .trim();

            if (comando.length() < 4) {
                return;
            }

            prefs.edit()
                    .putString(
                            "comando_voz",
                            comando
                    )
                    .apply();

            mensagem.setText(
                    "novo comando de voz ativo"
            );

            falar(
                    "Novo comando de voz ativo."
            );
        });

        Button voltar =
                botao("VOLTAR", 14);

        voltar.setOnClickListener(
                v -> construirTelaPrincipal()
        );

        tela.addView(voltar);

        setContentView(tela);
    }

    private void abrirVerificacao() {

        LinearLayout tela =
                baseTela(
                        "Verificação do aparelho"
                );

        TextView titulo =
                texto(
                        "\nVerificação básica\n",
                        27,
                        AZUL
                );

        titulo.setGravity(
                Gravity.CENTER
        );

        tela.addView(titulo);

        TextView explicacao =
                texto(
                        "Esta verificação analisa informações básicas dos aplicativos instalados. "
                                + "Ela não substitui o Google Play Protect.",
                        14,
                        AZUL_CLARO
                );

        explicacao.setGravity(
                Gravity.CENTER
        );

        tela.addView(explicacao);

        TextView resultado =
                texto(
                        "Nenhuma verificação realizada.",
                        18,
                        Color.WHITE
                );

        resultado.setGravity(
                Gravity.CENTER
        );

        tela.addView(resultado);

        TextView ultima =
                texto(
                        "",
                        15,
                        AZUL_CLARO
                );

        ultima.setGravity(
                Gravity.CENTER
        );

        tela.addView(ultima);

        Button verificar =
                botao(
                        "VERIFICAR",
                        15
                );

        tela.addView(verificar);

        TextView recentes =
                texto(
                        "\nApps verificados recentemente\n\n"
                                + "A verificação básica não modifica nem remove aplicativos.",
                        16,
                        Color.WHITE
                );

        tela.addView(recentes);

        atualizarUltimaVerificacao(
                ultima
        );

        verificar.setOnClickListener(v -> {

            resultado.setText(
                    "Analisando aplicativos instalados..."
            );

            verificar.setEnabled(false);

            new Thread(() -> {

                ResultadoVerificacao resultadoScan =
                        verificarAplicativos();

                runOnUiThread(() -> {

                    long agora =
                            System.currentTimeMillis();

                    prefs.edit()
                            .putLong(
                                    "ultima_verificacao",
                                    agora
                            )
                            .apply();

                    if (resultadoScan.aplicativosValidos) {

                        resultado.setText(
                                "Verificação básica concluída\n"
                                        + resultadoScan.totalAplicativos
                                        + " aplicativos analisados"
                        );

                    } else {

                        resultado.setText(
                                "Não foi possível concluir "
                                        + "a verificação básica."
                        );
                    }

                    atualizarUltimaVerificacao(
                            ultima
                    );

                    verificar.setEnabled(
                            true
                    );

                    falar(
                            resultadoScan.aplicativosValidos
                                    ? "A verificação básica foi concluída."
                                    : "Não foi possível concluir a verificação."
                    );
                });

            }).start();
        });

        Button voltar =
                botao(
                        "VOLTAR",
                        14
                );

        voltar.setOnClickListener(
                v -> construirTelaPrincipal()
        );

        tela.addView(voltar);

        setContentView(tela);
    }

    private void atualizarUltimaVerificacao(
            TextView texto) {

        long ultima =
                prefs.getLong(
                        "ultima_verificacao",
                        0
                );

        if (ultima == 0) {

            texto.setText(
                    "Verificação ainda não realizada"
            );

            return;
        }

        long minutos =
                (System.currentTimeMillis()
                        - ultima)
                        / 60000;

        if (minutos < 1) {

            texto.setText(
                    "Verificação realizada agora"
            );

        } else {

            texto.setText(
                    "Verificação realizada há "
                            + minutos
                            + " "
                            + (
                            minutos == 1
                                    ? "minuto"
                                    : "minutos"
                    )
            );
        }

        handler.postDelayed(
                () -> atualizarUltimaVerificacao(
                        texto
                ),
                10000
        );
    }

    private ResultadoVerificacao verificarAplicativos() {

        PackageManager pm =
                getPackageManager();

        List<ApplicationInfo> apps =
                pm.getInstalledApplications(
                        PackageManager.GET_META_DATA
                );

        int analisados = 0;

        for (ApplicationInfo app : apps) {

            if (app == null) {
                return new ResultadoVerificacao(
                        false,
                        analisados
                );
            }

            if (app.packageName == null) {
                return new ResultadoVerificacao(
                        false,
                        analisados
                );
            }

            if (app.sourceDir == null) {
                return new ResultadoVerificacao(
                        false,
                        analisados
                );
            }

            analisados++;
        }

        return new ResultadoVerificacao(
                true,
                analisados
        );
    }

    private static class ResultadoVerificacao {

        boolean aplicativosValidos;
        int totalAplicativos;

        ResultadoVerificacao(
                boolean aplicativosValidos,
                int totalAplicativos) {

            this.aplicativosValidos =
                    aplicativosValidos;

            this.totalAplicativos =
                    totalAplicativos;
        }
    }

    private void iniciarReconhecimento() {

        if (checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    PEDIR_MICROFONE
            );

            return;
        }

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

        try {

            startActivityForResult(
                    intent,
                    RECONHECER_VOZ
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Reconhecimento de voz indisponível.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == RECONHECER_VOZ
                && resultCode == RESULT_OK
                && data != null) {

            List<String> resultados =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );

            if (resultados != null
                    && !resultados.isEmpty()) {

                String pergunta =
                        resultados.get(0);

                responder(pergunta);
            }
        }
    }

    private void responder(
            String pergunta) {

        String resposta;

        String p =
                pergunta.toLowerCase(
                        Locale.getDefault()
                );

        if (p.contains("bateria")) {

            BatteryManager bm =
                    (BatteryManager)
                            getSystemService(
                                    BATTERY_SERVICE
                            );

            int nivel =
                    bm.getIntProperty(
                            BatteryManager
                                    .BATTERY_PROPERTY_CAPACITY
                    );

            resposta =
                    "A bateria está em "
                            + nivel
                            + " por cento.";

        } else if (p.contains("hora")) {

            resposta =
                    "Agora são "
                            + new SimpleDateFormat(
                                    "HH:mm",
                                    new Locale(
                                            "pt",
                                            "BR"
                                    )
                            ).format(
                                    new Date()
                            )
                            + ".";

        } else {

            resposta =
                    "Recebi sua mensagem. "
                            + "O cérebro de IA local ainda "
                            + "será conectado ao JARVIS.";
        }

        status.setText(resposta);

        falar(resposta);
    }

    private void falar(
            String texto) {

        if (tts != null) {

            tts.speak(
                    texto,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "JARVIS"
            );
        }
    }

    private void atualizarRelogio() {

        if (relogio != null) {

            Date agora =
                    new Date();

            relogio.setText(
                    new SimpleDateFormat(
                            "HH:mm",
                            new Locale(
                                    "pt",
                                    "BR"
                            )
                    ).format(agora)
            );

            data.setText(
                    new SimpleDateFormat(
                            "dd MMM",
                            new Locale(
                                    "pt",
                                    "BR"
                            )
                    ).format(agora)
                            .toUpperCase()
            );
        }

        handler.postDelayed(
                this::atualizarRelogio,
                1000
        );
    }

    @Override
    protected void onDestroy() {

        handler.removeCallbacksAndMessages(
                null
        );

        if (tts != null) {

            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }

    private class FrameLayoutReator
            extends android.widget.FrameLayout {

        TextView centro;

        TextView[] aneis =
                new TextView[8];

        FrameLayoutReator(
                Context context) {

            super(context);

            setWillNotDraw(false);

            centro =
                    texto(
                            "✦",
                            85,
                            Color.WHITE
                    );

            centro.setGravity(
                    Gravity.CENTER
            );

            centro.setBackgroundColor(
                    Color.TRANSPARENT
            );

            addView(
                    centro,
                    new android.widget.FrameLayout.LayoutParams(
                            145,
                            145,
                            Gravity.CENTER
                    )
            );

            for (int i = 0; i < 8; i++) {

                TextView anel =
                        texto(
                                "◯",
                                125 + (i * 3),
                                AMARELO
                        );

                anel.setGravity(
                        Gravity.CENTER
                );

                android.widget.FrameLayout.LayoutParams lp =
                        new android.widget.FrameLayout.LayoutParams(
                                150 + (i * 18),
                                150 + (i * 18),
                                Gravity.CENTER
                        );

                addView(
                        anel,
                        lp
                );

                aneis[i] =
                        anel;

                RotateAnimation rotacao =
                        new RotateAnimation(
                                i % 2 == 0
                                        ? 0
                                        : 360,
                                i % 2 == 0
                                        ? 360
                                        : 0,
                                Animation.RELATIVE_TO_SELF,
                                0.5f,
                                Animation.RELATIVE_TO_SELF,
                                0.5f
                        );

                rotacao.setDuration(
                        2600 + (i * 250)
                );

                rotacao.setRepeatCount(
                        Animation.INFINITE
                );

                rotacao.setInterpolator(
                        new LinearInterpolator()
                );

                anel.startAnimation(
                        rotacao
                );

                anel.setClickable(false);
                anel.setFocusable(false);
            }

            centro.setClickable(false);
            centro.setFocusable(false);

            bringChildToFront(
                    centro
            );
        }
    }
}
