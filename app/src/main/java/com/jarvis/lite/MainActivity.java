package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQUEST_MIC = 1001;

    private static final String PREFS = "jarvis_state";
    private static final String KEY_ONLINE = "online_mode";

    private SharedPreferences prefs;

    private TextView modeText;
    private Button modeButton;

    private final int BG = Color.rgb(8, 11, 18);
    private final int CYAN = Color.rgb(70, 220, 255);
    private final int GREEN = Color.rgb(60, 230, 150);
    private final int YELLOW = Color.rgb(255, 205, 50);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(165, 175, 190);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        // O JARVIS sempre inicia em modo offline.
        prefs.edit().putBoolean(KEY_ONLINE, false).apply();

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        criarTela();
    }

    private void criarTela() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(BG);

        root.setPadding(
                dp(18),
                dp(12),
                dp(18),
                dp(12)
        );

        /*
         * Ajuste para Android 15:
         * evita que o conteúdo fique escondido
         * atrás da barra de status ou navegação.
         */
        root.setOnApplyWindowInsetsListener((view, insets) -> {

            android.graphics.Insets barras =
                    insets.getInsets(WindowInsets.Type.systemBars());

            view.setPadding(
                    dp(18),
                    dp(12) + barras.top,
                    dp(18),
                    dp(12) + barras.bottom
            );

            return insets;
        });

        // =========================================================
        // TÍTULO
        // =========================================================

        TextView titulo = criarTexto(
                "J.A.R.V.I.S",
                26,
                CYAN,
                Typeface.BOLD
        );

        titulo.setGravity(Gravity.CENTER);

        root.addView(
                titulo,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(55)
                )
        );

        // =========================================================
        // SUBTÍTULO
        // =========================================================

        TextView subtitulo = criarTexto(
                "NÚCLEO PESSOAL",
                12,
                MUTED,
                Typeface.NORMAL
        );

        subtitulo.setGravity(Gravity.CENTER);

        root.addView(
                subtitulo,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(30)
                )
        );

        // =========================================================
        // REATOR ARC — BASE
        // =========================================================

        TextView reactor = criarTexto(
                "◉",
                105,
                YELLOW,
                Typeface.NORMAL
        );

        reactor.setGravity(Gravity.CENTER);

        reactor.setContentDescription(
                "Reator ARC. Toque para ativar o sistema de voz."
        );

        LinearLayout.LayoutParams reactorParams =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(190)
                );

        root.addView(reactor, reactorParams);

        // =========================================================
        // ESTADO DO SISTEMA
        // =========================================================

        TextView estado = criarTexto(
                "Sistema pronto.",
                15,
                WHITE,
                Typeface.NORMAL
        );

        estado.setGravity(Gravity.CENTER);

        root.addView(
                estado,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(42)
                )
        );

        // =========================================================
        // MODO OFFLINE / ONLINE
        // =========================================================

        modeText = criarTexto(
                "",
                16,
                GREEN,
                Typeface.BOLD
        );

        modeText.setGravity(Gravity.CENTER);

        root.addView(
                modeText,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(42)
                )
        );

        // =========================================================
        // BOTÃO DE MODO
        // =========================================================

        modeButton = new Button(this);

        modeButton.setTextSize(14);
        modeButton.setAllCaps(false);

        modeButton.setOnClickListener(
                view -> alternarModo()
        );

        root.addView(
                modeButton,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(52)
                )
        );

        // =========================================================
        // BOTÃO DE PERMISSÃO
        // =========================================================

        Button permissao = new Button(this);

        permissao.setText(
                "Gerenciar permissão do microfone"
        );

        permissao.setTextSize(14);
        permissao.setAllCaps(false);

        permissao.setOnClickListener(
                view -> gerenciarMicrofone()
        );

        root.addView(
                permissao,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(52)
                )
        );

        // =========================================================
        // AVISO DE SEGURANÇA
        // =========================================================

        TextView seguranca = criarTexto(
                "OFFLINE é o estado padrão. " +
                "Nesta etapa o aplicativo não possui " +
                "permissão de INTERNET e não envia dados para a rede.",
                12,
                MUTED,
                Typeface.NORMAL
        );

        seguranca.setGravity(Gravity.CENTER);

        root.addView(
                seguranca,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(70)
                )
        );

        // =========================================================
        // ATUALIZA INTERFACE
        // =========================================================

        atualizarModo();

        // =========================================================
        // TOQUE NO REATOR
        // =========================================================

        reactor.setOnClickListener(view -> {

            if (checkSelfPermission(
                    Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.RECORD_AUDIO
                        },
                        REQUEST_MIC
                );

                Toast.makeText(
                        this,
                        "Permissão do microfone solicitada.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            estado.setText(
                    "Microfone autorizado."
            );

            Toast.makeText(
                    this,
                    "Sistema de voz preparado para a próxima etapa.",
                    Toast.LENGTH_SHORT
            ).show();
        });

        setContentView(root);

        root.requestApplyInsets();
    }

    // =============================================================
    // ALTERAR MODO
    // =============================================================

    private void alternarModo() {

        boolean atual =
                prefs.getBoolean(KEY_ONLINE, false);

        boolean novoModo = !atual;

        /*
         * IMPORTANTE:
         *
         * Nesta versão ainda não existe conexão de Internet.
         *
         * O botão apenas controla o estado da interface.
         *
         * O módulo online real será adicionado
         * posteriormente e ficará bloqueado enquanto
         * o usuário não ativar o modo online.
         */

        prefs.edit()
                .putBoolean(KEY_ONLINE, novoModo)
                .apply();

        atualizarModo();

        if (novoModo) {

            Toast.makeText(
                    this,
                    "Modo online selecionado. " +
                    "O módulo de Internet será adicionado posteriormente.",
                    Toast.LENGTH_LONG
            ).show();

        } else {

            Toast.makeText(
                    this,
                    "Modo offline ativo.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =============================================================
    // ATUALIZAR MODO NA TELA
    // =============================================================

    private void atualizarModo() {

        boolean online =
                prefs.getBoolean(KEY_ONLINE, false);

        if (online) {

            modeText.setText(
                    "🟢 MODO ONLINE SELECIONADO"
            );

            modeText.setTextColor(GREEN);

            modeButton.setText(
                    "Desativar modo online"
            );

        } else {

            modeText.setText(
                    "🔴 MODO OFFLINE ATIVO"
            );

            modeText.setTextColor(YELLOW);

            modeButton.setText(
                    "Ativar modo online"
            );
        }
    }

    // =============================================================
    // MICROFONE
    // =============================================================

    private void gerenciarMicrofone() {

        if (checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    REQUEST_MIC
            );

        } else {

            Intent intent =
                    new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    );

            Uri uri =
                    Uri.parse(
                            "package:" + getPackageName()
                    );

            intent.setData(uri);

            startActivity(intent);
        }
    }

    // =============================================================
    // RESULTADO DA PERMISSÃO
    // =============================================================

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == REQUEST_MIC) {

            if (
                    grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED
            ) {

                Toast.makeText(
                        this,
                        "Microfone autorizado.",
                        Toast.LENGTH_SHORT
                ).show();

            } else {

                Toast.makeText(
                        this,
                        "Microfone não autorizado. " +
                        "O JARVIS continuará sem escuta.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    // =============================================================
    // CRIAR TEXTVIEW
    // =============================================================

    private TextView criarTexto(
            String texto,
            float tamanho,
            int cor,
            int estilo
    ) {

        TextView textView =
                new TextView(this);

        textView.setText(texto);
        textView.setTextSize(tamanho);
        textView.setTextColor(cor);

        textView.setTypeface(
                Typeface.DEFAULT,
                estilo
        );

        textView.setGravity(
                Gravity.CENTER
        );

        return textView;
    }

    // =============================================================
    // DP
    // =============================================================

    private int dp(int valor) {

        return Math.round(
                valor *
                getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}
