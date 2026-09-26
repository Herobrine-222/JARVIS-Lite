package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.RecognizerIntent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int PEDIR_MICROFONE = 100;
    private static final int RECONHECER_VOZ = 101;

    private TextView resposta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout tela = new LinearLayout(this);
        tela.setOrientation(LinearLayout.VERTICAL);
        tela.setPadding(40, 80, 40, 40);

        TextView titulo = new TextView(this);
        titulo.setText("JARVIS LITE");
        titulo.setTextSize(28);

        resposta = new TextView(this);
        resposta.setText(
                "\nSistemas online.\n" +
                "Modo offline ativado.\n\n" +
                "Aguardando ativação..."
        );
        resposta.setTextSize(20);

        Button ativar = new Button(this);
        ativar.setText("🎙️ Ativar JARVIS");

        ativar.setOnClickListener(v -> iniciarVoz());

        tela.addView(titulo);
        tela.addView(resposta);
        tela.addView(ativar);

        setContentView(tela);
    }

    private void iniciarVoz() {

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PEDIR_MICROFONE
            );

            return;
        }

        ouvir();
    }

    private void ouvir() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "pt-BR"
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "JARVIS está ouvindo..."
        );

        startActivityForResult(intent, RECONHECER_VOZ);
    }

    private String normalizar(String texto) {

        return Normalizer
                .normalize(
                        texto.toLowerCase(Locale.ROOT),
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "");
    }

    private boolean ehAtivacao(String texto) {

        String normalizado = normalizar(texto);

        return normalizado.contains("jarvis") &&
               normalizado.contains("esta ai");
    }

    private boolean ehComandoBateria(String texto) {

        String normalizado = normalizar(texto);

        return normalizado.contains("bateria");
    }

    private String obterEstadoBateria() {

        BatteryManager bateria =
                (BatteryManager) getSystemService(BATTERY_SERVICE);

        int porcentagem = bateria.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CAPACITY
        );

        int status = bateria.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_STATUS
        );

        String estado;

        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            estado = "carregando";
        } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
            estado = "com carga completa";
        } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
            estado = "não está carregando";
        } else {
            estado = "sem carregamento ativo";
        }

        return "Sua bateria está em " +
                porcentagem +
                "% e " +
                estado +
                ".";
    }

    private String obterPorcentagemBateria() {

        BatteryManager bateria =
                (BatteryManager) getSystemService(BATTERY_SERVICE);

        int porcentagem = bateria.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CAPACITY
        );

        return "Sua bateria está em " +
                porcentagem +
                "%.";
    }

    private void processarComando(String comando) {

        String normalizado = normalizar(comando);

        if (ehComandoBateria(comando)) {

            if (normalizado.contains("estado") ||
                normalizado.contains("como esta") ||
                normalizado.contains("como ta")) {

                resposta.setText(
                        obterEstadoBateria()
                );

            } else {

                resposta.setText(
                        obterPorcentagemBateria()
                );
            }

            return;
        }

        resposta.setText(
                "Comando não reconhecido.\n\n" +
                "Aguardando ativação..."
        );
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

        if (requestCode == RECONHECER_VOZ &&
                resultCode == RESULT_OK &&
                data != null) {

            ArrayList<String> resultados =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );

            if (resultados != null &&
                    !resultados.isEmpty()) {

                String comando = resultados.get(0);

                if (ehAtivacao(comando)) {

                    resposta.setText(
                            "À sua disposição.\n\n" +
                            "Sistemas online.\n" +
                            "O que deseja?"
                    );

                    // Abre uma segunda escuta para receber o comando.
                    ouvir();

                } else if (ehComandoBateria(comando)) {

                    processarComando(comando);

                } else {

                    resposta.setText(
                            "Aguardando ativação...\n\n" +
                            "Frase de ativação:\n" +
                            "\"JARVIS, está aí?\""
                    );
                }
            }
        }
    }
            }
