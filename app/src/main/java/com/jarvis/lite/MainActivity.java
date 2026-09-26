package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
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

        return normalizar(texto).contains("bateria");
    }

    private boolean ehPerguntaEstadoBateria(String texto) {

        String normalizado = normalizar(texto);

        return normalizado.contains("estado") ||
               normalizado.contains("como esta") ||
               normalizado.contains("como ta") ||
               normalizado.contains("saude") ||
               normalizado.contains("vida") ||
               normalizado.contains("condicao") ||
               normalizado.contains("temperatura") ||
               normalizado.contains("carregando");
    }

    private boolean ehPerguntaPorcentagem(String texto) {

        String normalizado = normalizar(texto);

        return normalizado.contains("porcentagem") ||
               normalizado.contains("percent") ||
               normalizado.contains("quantos por cento") ||
               normalizado.contains("quanto resta") ||
               normalizado.contains("quanto tem") ||
               normalizado.contains("%");
    }

    private String obterEstadoBateria() {

        BatteryManager bateria =
                (BatteryManager) getSystemService(BATTERY_SERVICE);

        int porcentagem = bateria.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CAPACITY
        );

        Intent intentBateria = registerReceiver(
                null,
                new android.content.IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED
                )
        );

        String estado;

        int status = -1;

        if (intentBateria != null) {
            status = intentBateria.getIntExtra(
                    BatteryManager.EXTRA_STATUS,
                    -1
            );
        }

        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            estado = "está carregando";
        } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
            estado = "está com carga completa";
        } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
            estado = "não está carregando";
        } else {
            estado = "não está em carregamento ativo";
        }

        String temperatura = obterTemperaturaBateria(intentBateria);
        String saude = obterSaudeBateria(intentBateria);

        return "Sua bateria está em " +
                porcentagem +
                "%. " +
                "Ela " +
                estado +
                ". " +
                temperatura +
                " " +
                saude;
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

    private String obterTemperaturaBateria(Intent intentBateria) {

        if (intentBateria == null) {
            return "Não foi possível obter a temperatura da bateria.";
        }

        int temperatura = intentBateria.getIntExtra(
                BatteryManager.EXTRA_TEMPERATURE,
                Integer.MIN_VALUE
        );

        if (temperatura == Integer.MIN_VALUE) {
            return "Não foi possível obter a temperatura da bateria.";
        }

        double temperaturaCelsius = temperatura / 10.0;

        return "A temperatura da bateria é de " +
                String.format(
                        Locale.US,
                        "%.1f",
                        temperaturaCelsius
                ) +
                " graus Celsius.";
    }

    private String obterSaudeBateria(Intent intentBateria) {

        if (intentBateria == null) {
            return "Não foi possível determinar a saúde da bateria.";
        }

        int saude = intentBateria.getIntExtra(
                BatteryManager.EXTRA_HEALTH,
                BatteryManager.BATTERY_HEALTH_UNKNOWN
        );

        switch (saude) {

            case BatteryManager.BATTERY_HEALTH_GOOD:
                return "O sistema reporta a saúde da bateria como normal.";

            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                return "O sistema reporta um alerta de superaquecimento.";

            case BatteryManager.BATTERY_HEALTH_DEAD:
                return "O sistema reporta uma falha grave na bateria.";

            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                return "O sistema reporta um alerta de sobretensão.";

            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                return "O sistema reporta uma falha não especificada na bateria.";

            case BatteryManager.BATTERY_HEALTH_COLD:
                return "O sistema reporta que a bateria está muito fria.";

            default:
                return "Não foi possível determinar a saúde da bateria.";
        }
    }

    private void processarComando(String comando) {

        if (!ehComandoBateria(comando)) {
            resposta.setText(
                    "Comando não reconhecido.\n\n" +
                    "Aguardando ativação..."
            );
            return;
        }

        if (ehPerguntaPorcentagem(comando) &&
            !ehPerguntaEstadoBateria(comando)) {

            resposta.setText(
                    obterPorcentagemBateria()
            );

            return;
        }

        resposta.setText(
                obter
