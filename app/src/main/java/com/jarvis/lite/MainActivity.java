package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.speech.RecognizerIntent;
import android.app.ActivityManager;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

        Intent intent = new Intent(
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

        String t = normalizar(texto);

        return t.contains("jarvis") &&
               t.contains("esta ai");
    }

    private boolean ehBateria(String texto) {

        return normalizar(texto).contains("bateria");
    }

    private boolean ehTemperaturaBateria(String texto) {

        String t = normalizar(texto);

        return t.contains("temperatura da bateria") ||
               t.contains("temperatura da minha bateria") ||
               t.contains("temperatura bateria") ||
               t.contains("quao quente esta a bateria") ||
               t.contains("quao quente esta minha bateria");
    }

    private boolean ehPorcentagemBateria(String texto) {

        String t = normalizar(texto);

        return t.contains("porcentagem da bateria") ||
               t.contains("porcentagem da minha bateria") ||
               t.contains("quanto de bateria tenho") ||
               t.contains("quanto de carga tenho") ||
               t.contains("quanto resta de bateria") ||
               t.contains("quanto ainda tenho de bateria") ||
               t.contains("qual o nivel da bateria") ||
               t.contains("qual o nivel de bateria") ||
               t.contains("quantos por cento de bateria") ||
               t.contains("percentual da bateria");
    }

    private boolean ehCarregamento(String texto) {

        String t = normalizar(texto);

        return t.contains("esta carregando") ||
               t.contains("ta carregando") ||
               t.contains("esta carregado") ||
               t.contains("celular esta carregando") ||
               t.contains("meu celular esta carregando") ||
               t.contains("o celular esta carregando");
    }

    private boolean ehSaudeBateria(String texto) {

        String t = normalizar(texto);

        return t.contains("saude da bateria") ||
               t.contains("saude da minha bateria") ||
               t.contains("bateria esta saudavel") ||
               t.contains("bateria esta normal") ||
               t.contains("condicao da bateria") ||
               t.contains("vida da bateria");
    }

    private boolean ehEstadoBateria(String texto) {

        String t = normalizar(texto);

        return t.contains("estado da bateria") ||
               t.contains("estado da minha bateria") ||
               t.contains("como esta a bateria") ||
               t.contains("como ta a bateria") ||
               t.contains("como esta minha bateria") ||
               t.contains("como ta minha bateria");
    }

    private boolean ehModelo(String texto) {

        String t = normalizar(texto);

        return t.contains("qual e meu celular") ||
               t.contains("qual e o meu celular") ||
               t.contains("qual e o modelo do celular") ||
               t.contains("qual o modelo do celular") ||
               t.contains("qual o modelo do aparelho") ||
               t.contains("qual modelo eu tenho") ||
               t.contains("que celular eu tenho");
    }

    private boolean ehAndroid(String texto) {

        String t = normalizar(texto);

        return t.contains("qual android eu tenho") ||
               t.contains("qual e o meu android") ||
               t.contains("qual e meu android") ||
               t.contains("qual a versao do meu android") ||
               t.contains("qual versao do meu android") ||
               t.contains("qual versao do android eu tenho") ||
               t.contains("que android eu tenho") ||
               t.contains("que versao do android eu tenho") ||
               t.contains("qual android esta instalado");
    }

    private boolean ehArmazenamento(String texto) {

        String t = normalizar(texto);

        return t.contains("quanto armazenamento tenho") ||
               t.contains("quanto espaco tenho") ||
               t.contains("quanto espaco esta disponivel") ||
               t.contains("quanto armazenamento esta disponivel") ||
               t.contains("quanto de armazenamento tenho") ||
               t.contains("quanto de espaco tenho") ||
               t.contains("espaco livre") ||
               t.contains("armazenamento livre");
    }

    private boolean ehRAM(String texto) {

        String t = normalizar(texto);

        return t.contains("quanto de ram tenho") ||
               t.contains("quanta ram tenho") ||
               t.contains("quanto de memoria ram tenho") ||
               t.contains("quanta memoria ram tenho") ||
               t.contains("quanto de ram esta disponivel") ||
               t.contains("quanta ram esta disponivel") ||
               t.contains("memoria ram") ||
               t.contains("ram disponivel");
    }

    private boolean ehHora(String texto) {

        String t = normalizar(texto);

        return t.contains("que horas sao") ||
               t.contains("qual e a hora") ||
               t.contains("qual a hora") ||
               t.contains("que hora e") ||
               t.contains("me diga a hora") ||
               t.contains("horario atual");
    }

    private boolean ehData(String texto) {

        String t = normalizar(texto);

        return t.contains("que dia e hoje") ||
               t.contains("qual e a data de hoje") ||
               t.contains("qual a data de hoje") ||
               t.contains("qual a data") ||
               t.contains("data de hoje") ||
               t.contains("me diga a data") ||
               t.contains("dia de hoje");
    }

    private boolean ehAtualizacao(String texto) {

        String t = normalizar(texto);

        return t.contains("atualizacao do android") ||
               t.contains("atualizacao do software") ||
               t.contains("atualizacao do sistema") ||
               t.contains("atualizacao disponivel") ||
               t.contains("tem atualizacao") ||
               t.contains("existe atualizacao") ||
               t.contains("meu celular tem atualizacao");
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

    private Intent obterInformacoesBateria() {

        return registerReceiver(
                null,
                new android.content.IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED
                )
        );
    }

    private String obterTemperaturaBateria() {

        Intent intentBateria = obterInformacoesBateria();

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

    private String obterCarregamento() {

        Intent intentBateria = obterInformacoesBateria();

        if (intentBateria == null) {
            return "Não foi possível determinar o estado do carregamento.";
        }

        int status = intentBateria.getIntExtra(
                BatteryManager.EXTRA_STATUS,
                -1
        );

        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            return "Sim, a bateria está carregando.";
        }

        if (status == BatteryManager.BATTERY_STATUS_FULL) {
            return "A bateria está com carga completa.";
        }

        return "Não, a bateria não está carregando.";
    }

    private String obterSaudeBateria() {

        Intent intentBateria = obterInformacoesBateria();

        if (intentBateria == null) {
            return "Não foi possível determinar a saúde da bateria.";
        }

        int saude = intentBateria.getIntExtra(
                BatteryManager.EXTRA_HEALTH,
                BatteryManager.BATTERY_HEALTH_UNKNOWN
        );

        switch (saude) {

            case BatteryManager.BATTERY_HEALTH_GOOD:
                return "A saúde reportada pelo sistema está normal.";

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

    private String obterEstadoBateria() {

        return obterPorcentagemBateria() + " " +
                obterCarregamento() + " " +
                obterTemperaturaBateria() + " " +
                obterSaudeBateria();
    }

    private String obterModelo() {

        return "O modelo do seu celular é " +
                Build.MODEL +
                ".";
    }

    private String obterAndroid() {

        return "Seu celular está usando o Android " +
                Build.VERSION.RELEASE +
                ".";
    }

    private String formatarGB(long bytes) {

        double gb = bytes / (1024.0 * 1024.0 * 1024.0);

        return String.format(
                Locale.US,
                "%.2f GB",
                gb
        );
    }

    private String obterArmazenamento() {

        StatFs armazenamento =
                new StatFs(
                        Environment.getDataDirectory().getPath()
                );

        long total = armazenamento.getTotalBytes();
        long livre = armazenamento.getAvailableBytes();

        return "Você tem " +
                formatarGB(livre) +
                " livres de " +
                formatarGB(total) +
                " de armazenamento.";
    }

    private String obterRAM() {

        ActivityManager gerenciador =
                (ActivityManager) getSystemService(
                        ACTIVITY_SERVICE
                );

        ActivityManager.MemoryInfo memoria =
                new ActivityManager.MemoryInfo();

        gerenciador.getMemoryInfo(memoria);

        return "Você tem " +
                formatarGB(memoria.availMem) +
                " de RAM disponível de " +
                formatarGB(memoria.totalMem) +
                ".";
    }

    private String obterHora() {

        return "Agora são " +
                new SimpleDateFormat(
                        "HH:mm",
                        Locale.getDefault()
                ).format(new Date()) +
                ".";
    }

    private String obterData() {

        return "Hoje é " +
                new SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                ).format(new Date()) +
                ".";
    }

    private String respostaAtualizacao() {

        return "Não tenho permissão para acessar informações mais internas do aparelho.";
    }

    private void responder(String texto) {

        resposta.setText(texto);
    }

    private void processarComando(String comando) {

        String t = normalizar(comando);

        if (ehAtualizacao(t)) {
            responder(respostaAtualizacao());
            return;
        }

        if (ehTemperaturaBateria(t)) {
            responder(obterTemperaturaBateria());
            return;
        }

        if (ehPorcentagemBateria(t)) {
            responder(obterPorcentagemBateria());
            return;
        }

        if (ehCarregamento(t)) {
            responder(obterCarregamento());
            return;
        }

        if (ehSaudeBateria(t)) {
            responder(obterSaudeBateria());
            return;
        }

        if (ehEstadoBateria(t)) {
            responder(obterEstadoBateria());
            return;
        }

        if (ehModelo(t)) {
            responder(obterModelo());
            return;
        }

        if (ehAndroid(t)) {
            responder(obterAndroid());
            return;
        }

        if (ehArmazenamento(t)) {
            responder(obterArmazenamento());
            return;
        }

        if (ehRAM(t)) {
            responder(obterRAM());
            return;
        }

        if (ehHora(t)) {
            responder(obterHora());
            return;
        }

        if (ehData(t)) {
            responder(obterData());
            return;
        }

        responder(
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

                    responder(
                            "À sua disposição.\n\n" +
                            "Sistemas online.\n" +
                            "O que deseja?"
                    );

                    ouvir();

                } else {

                    processarComando(comando);
                }
            }
        }
    }
}
