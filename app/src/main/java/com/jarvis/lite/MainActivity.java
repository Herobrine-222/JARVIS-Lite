package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int PEDIR_MICROFONE = 100;
    private static final int OUVIR_VOZ = 101;

    private TextView resposta;
    private Button botaoOuvir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        criarInterface();

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PEDIR_MICROFONE
            );
        }
    }

    private void criarInterface() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 40, 32, 40);

        TextView titulo = new TextView(this);
        titulo.setText("JARVIS Lite");
        titulo.setTextSize(28);

        resposta = new TextView(this);
        resposta.setText(
                "À sua disposição.\n" +
                "Sistemas online.\n" +
                "Toque em OUVIR JARVIS para começar."
        );
        resposta.setTextSize(18);
        resposta.setPadding(0, 30, 0, 30);

        botaoOuvir = new Button(this);
        botaoOuvir.setText("OUVIR JARVIS");

        botaoOuvir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ouvir();
            }
        });

        layout.addView(titulo);
        layout.addView(resposta);
        layout.addView(botaoOuvir);

        setContentView(layout);
    }

    private void ouvir() {

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PEDIR_MICROFONE
            );

            return;
        }

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
                "Diga JARVIS..."
        );

        startActivityForResult(intent, OUVIR_VOZ);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == OUVIR_VOZ
                && resultCode == RESULT_OK
                && data != null) {

            List<String> resultados =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );

            if (resultados != null && !resultados.isEmpty()) {

                String texto = resultados.get(0);

                processarEntrada(texto);
            }
        }
    }

    private void processarEntrada(String texto) {

        if (texto == null) {
            return;
        }

        texto = texto.trim();

        String normalizado = normalizar(texto);

        if (normalizado.equals("jarvis")) {

            resposta.setText(
                    "À sua disposição.\n" +
                    "Sistemas online.\n" +
                    "O que deseja?"
            );

            ouvir();
            return;
        }

        if (normalizado.startsWith("jarvis ")) {

            String comando = texto.substring(6).trim();

            processarComando(comando);
            return;
        }

        if (normalizado.startsWith("jarvis,")) {

            String comando = texto.substring(7).trim();

            processarComando(comando);
            return;
        }

        resposta.setText(
                "Ativação não reconhecida.\n" +
                "Diga: JARVIS"
        );
    }

    private void processarComando(String comando) {

        String texto = normalizar(comando);

        StringBuilder resultado = new StringBuilder();

        boolean reconheceu = false;

        if (ehPorcentagemBateria(texto)) {
            resultado.append(
                    "Bateria: ")
                    .append(obterPorcentagemBateria())
                    .append("%\n\n");

            reconheceu = true;

        } else if (ehEstadoBateria(texto)) {

            resultado.append(obterEstadoBateria())
                    .append("\n\n");

            reconheceu = true;
        }

        if (ehTemperatura(texto)) {

            resultado.append(obterTemperaturaBateria())
                    .append("\n\n");

            reconheceu = true;
        }

        if (ehSaudeBateria(texto)) {

            resultado.append(obterSaudeBateria())
                    .append("\n\n");

            reconheceu = true;
        }

        if (ehModelo(texto)) {

            resultado.append(
                    "Modelo: ")
                    .append(Build.MODEL)
                    .append("\n\n");

            reconheceu = true;
        }

        if (ehAndroid(texto)) {

            resultado.append(obterAndroid())
                    .append("\n\n");

            reconheceu = true;
        }

        if (ehArmazenamento(texto)) {

            resultado.append(obterArmazenamento())
                    .append("\n\n");

            reconheceu = true;
        }

        if (ehRam(texto)) {

            resultado.append(obterRam())
                    .append("\n\n");

            reconheceu = true;
        }

        if (ehDataHora(texto)) {

            resultado.append(obterDataHora())
                    .append("\n\n");

            reconheceu = true;
        }

        if (ehAtualizacao(texto)) {

            resultado.append(
                    "Não tenho permissão para acessar informações mais internas do aparelho."
            ).append("\n\n");

            reconheceu = true;
        }

        if (ehWiFi(texto)) {

            resultado.append(obterWiFi())
                    .append("\n\n");

            reconheceu = true;
        }

        if (ehBluetooth(texto)) {

            resultado.append(
                    "Bluetooth: informação não disponível nesta versão."
            ).append("\n\n");

            reconheceu = true;
        }

        if (ehModoAviao(texto)) {

            resultado.append(
                    "Modo avião: informação não disponível nesta versão."
            ).append("\n\n");

            reconheceu = true;
        }

        if (ehComandoJogo(texto)) {

            resultado.append(
                    "Esse jogo não permite que eu acesse essas informações."
            ).append("\n\n");

            reconheceu = true;
        }

        if (ehAnaliseCompleta(texto)) {

            resultado.append(obterAnaliseCompleta())
                    .append("\n\n");

            reconheceu = true;
        }

        if (ehTudo(texto)) {

            resultado.append(obterTudo())
                    .append("\n\n");

            reconheceu = true;
        }

        if (!reconheceu) {

            resultado.append(
                    "Comando não reconhecido.\n\n"
            );

            resultado.append(
                    "Você pode perguntar sobre bateria, temperatura, " +
                    "RAM, armazenamento, modelo, Android, data e hora."
            );
        }

        resposta.setText(resultado.toString());
    }

    private String normalizar(String texto) {

        texto = texto.toLowerCase(Locale.ROOT);

        texto = java.text.Normalizer
                .normalize(
                        texto,
                        java.text.Normalizer.Form.NFD
                )
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        return texto.trim();
    }

    private boolean ehPorcentagemBateria(String t) {

        return t.contains("porcentagem da bateria")
                || t.contains("porcentagem bateria")
                || t.contains("percentual da bateria")
                || t.contains("quanto de bateria")
                || t.contains("quantos por cento de bateria");
    }

    private boolean ehEstadoBateria(String t) {

        return t.equals("bateria")
                || t.contains("estado da bateria")
                || t.contains("como esta a bateria")
                || t.contains("status da bateria");
    }

    private boolean ehTemperatura(String t) {

        return t.contains("temperatura")
                || t.contains("temperatura da bateria")
                || t.contains("celular esta quente")
                || t.contains("celular esta frio");
    }

    private boolean ehSaudeBateria(String t) {

        return t.contains("saude da bateria")
                || t.contains("saude bateria")
                || t.contains("saude da minha bateria");
    }

    private boolean ehModelo(String t) {

        return t.contains("modelo do celular")
                || t.contains("modelo do aparelho")
                || t.contains("qual meu celular")
                || t.contains("qual e meu celular");
    }

    private boolean ehAndroid(String t) {

        return t.contains("versao do android")
                || t.contains("qual android")
                || t.contains("android estou usando");
    }

    private boolean ehArmazenamento(String t) {

        return t.contains("armazenamento")
                || t.contains("espaco livre")
                || t.contains("memoria interna");
    }

    private boolean ehRam(String t) {

        return t.contains("ram")
                || t.contains("memoria ram");
    }

    private boolean ehDataHora(String t) {

        return t.contains("data")
                || t.contains("hora")
                || t.contains("que horas")
                || t.contains("que dia");
    }

    private boolean ehAtualizacao(String t) {

        return t.contains("atualizacao")
                || t.contains("atualizar o celular")
                || t.contains("atualizacao do sistema");
    }

    private boolean ehWiFi(String t) {

        return t.contains("wifi")
                || t.contains("wi fi")
                || t.contains("internet");
    }

    private boolean ehBluetooth(String t) {

        return t.contains("bluetooth");
    }

    private boolean ehModoAviao(String t) {

        return t.contains("modo aviao")
                || t.contains("modo aviao esta ligado");
    }

    private boolean ehComandoJogo(String t) {

        return t.contains("fps")
                || t.contains("ping")
                || t.contains("upload")
                || t.contains("download")
                || t.contains("velocidade da internet")
                || t.contains("desempenho no jogo");
    }

    private boolean ehAnaliseCompleta(String t) {

        return t.contains("analisar o estado")
                || t.contains("analise do celular")
                || t.contains("analise completa")
                || t.contains("analisar meu celular");
    }

    private boolean ehTudo(String t) {

        return t.equals("tudo")
                || t.contains("todas as informacoes")
                || t.contains("tudo sobre o celular")
                || t.contains("me fale tudo");
    }

    private int obterPorcentagemBateria() {

        BatteryManager bm =
                (BatteryManager) getSystemService(BATTERY_SERVICE);

        return bm.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CAPACITY
        );
    }

    private String obterEstadoBateria() {

        Intent bateria = registerReceiver(
                null,
                new android.content.IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED
                )
        );

        if (bateria == null) {
            return "Não consegui acessar o estado da bateria.";
        }

        int nivel = bateria.getIntExtra(
                BatteryManager.EXTRA_LEVEL,
                -1
        );

        int escala = bateria.getIntExtra(
                BatteryManager.EXTRA_SCALE,
                -1
        );

        int status = bateria.getIntExtra(
                BatteryManager.EXTRA_STATUS,
                -1
        );

        int porcentagem = 0;

        if (nivel >= 0 && escala > 0) {
            porcentagem = (int) (
                    nivel * 100f / escala
            );
        }

        String carregando;

        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {

            carregando = "Está carregando.";

        } else if (status == BatteryManager.BATTERY_STATUS_FULL) {

            carregando = "Está completamente carregada.";

        } else {

            carregando = "Não está carregando.";
        }

        return "Bateria: "
                + porcentagem
                + "%.\n"
                + carregando;
    }

    private String obterTemperaturaBateria() {

        Intent bateria = registerReceiver(
                null,
                new android.content.IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED
                )
        );

        if (bateria == null) {
            return "Não consegui acessar a temperatura.";
        }

        int temperatura = bateria.getIntExtra(
                BatteryManager.EXTRA_TEMPERATURE,
                -1
        );

        if (temperatura < 0) {
            return "Temperatura da bateria não disponível.";
        }

        float graus = temperatura / 10.0f;

        return String.format(
                Locale.US,
                "Temperatura da bateria: %.1f °C.",
                graus
        );
    }

    private String obterSaudeBateria() {

        Intent bateria = registerReceiver(
                null,
                new android.content.IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED
                )
        );

        if (bateria == null) {
            return "Não consegui acessar a saúde da bateria.";
        }

        int saude = bateria.getIntExtra(
                BatteryManager.EXTRA_HEALTH,
                -1
        );

        String texto;

        switch (saude) {

            case BatteryManager.BATTERY_HEALTH_GOOD:
                texto = "Boa";
                break;

            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                texto = "Superaquecimento";
                break;

            case BatteryManager.BATTERY_HEALTH_DEAD:
                texto = "Sem funcionamento";
                break;

            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                texto = "Sobretensão";
                break;

            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                texto = "Falha não especificada";
                break;

            case BatteryManager.BATTERY_HEALTH_COLD:
                texto = "Muito fria";
                break;

            default:
                texto = "Desconhecida";
                break;
        }

        return "Saúde da bateria: " + texto + ".";
    }

    private String obterAndroid() {

        return "Android: "
                + Build.VERSION.RELEASE
                + "\nAPI: "
                + Build.VERSION.SDK_INT;
    }

    private String obterArmazenamento() {

        StatFs stat = new StatFs(
                Environment.getDataDirectory().getPath()
        );

        long totalBytes =
                stat.getTotalBytes();

        long livreBytes =
                stat.getAvailableBytes();

        return "Armazenamento total: "
                + formatarGB(totalBytes)
                + "\n"
                + "Espaço livre: "
                + formatarGB(livreBytes);
    }

    private String obterRam() {

        android.app.ActivityManager am =
                (android.app.ActivityManager)
                        getSystemService(ACTIVITY_SERVICE);

        android.app.ActivityManager.MemoryInfo info =
                new android.app.ActivityManager.MemoryInfo();

        am.getMemoryInfo(info);

        return "RAM total: "
                + formatarGB(info.totalMem)
                + "\n"
                + "RAM disponível: "
                + formatarGB(info.availMem);
    }

    private String obterDataHora() {

        SimpleDateFormat formato =
                new SimpleDateFormat(
                        "dd/MM/yyyy HH:mm:ss",
                        Locale.getDefault()
                );

        return "Data e hora: "
                + formato.format(new Date());
    }

    private String obterWiFi() {

        ConnectivityManager cm =
                (ConnectivityManager)
                        getSystemService(
                                CONNECTIVITY_SERVICE
                        );

        NetworkInfo info =
                cm.getActiveNetworkInfo();

        if (info != null
                && info.getType()
                == ConnectivityManager.TYPE_WIFI) {

            return "Wi-Fi: conectado.";
        }

        return "Wi-Fi: não conectado.";
    }

    private String obterAnaliseCompleta() {

        return "ANÁLISE DO APARELHO\n\n"
                + obterEstadoBateria()
                + "\n\n"
                + obterTemperaturaBateria()
                + "\n\n"
                + obterSaudeBateria()
                + "\n\n"
                + "Modelo: "
                + Build.MODEL
                + "\n\n"
                + obterAndroid()
                + "\n\n"
                + obterArmazenamento()
                + "\n\n"
                + obterRam()
                + "\n\n"
                + obterDataHora();
    }

    private String obterTudo() {

        return obterAnaliseCompleta()
                + "\n\n"
                + obterWiFi()
                + "\n\n"
                + "Bluetooth: informação não disponível nesta versão."
                + "\n\n"
                + "Modo avião: informação não disponível nesta versão.";
    }

    private String formatarGB(long bytes) {

        double gb =
                bytes / (1024.0 * 1024.0 * 1024.0);

        return String.format(
                Locale.US,
                "%.2f GB",
                gb
        );
    }
}
