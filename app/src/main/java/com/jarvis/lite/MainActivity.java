package com.jarvis.lite;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int PEDIDO_VOZ = 1001;

    private TextView textoStatus;
    private TextView textoRelogio;
    private TextView textoData;
    private TextView textoBateria;
    private TextView textoUso;
    private TextView textoLivre;
    private TextView textoTemperatura;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        textoStatus = findViewById(R.id.textoStatus);
        textoRelogio = findViewById(R.id.textoRelogio);
        textoData = findViewById(R.id.textoData);
        textoBateria = findViewById(R.id.textoBateria);
        textoUso = findViewById(R.id.textoUso);
        textoLivre = findViewById(R.id.textoLivre);
        textoTemperatura = findViewById(R.id.textoTemperatura);

        View botaoReator = findViewById(R.id.botaoReator);
        View botaoConfiguracoes = findViewById(R.id.botaoConfiguracoes);
        View painelUso = findViewById(R.id.textoUso);
        View painelTemperatura = findViewById(R.id.textoTemperatura);

        atualizarInterface();

        botaoReator.setOnClickListener(v -> iniciarReconhecimentoVoz());

        botaoConfiguracoes.setOnClickListener(v -> mostrarConfiguracoes());

        painelUso.setOnClickListener(v ->
                textoStatus.setText(
                        "Armazenamento\n\n" +
                        obterArmazenamentoDetalhado() +
                        "\n\nRAM\n" +
                        obterRam()
                )
        );

        painelTemperatura.setOnClickListener(v ->
                textoStatus.setText(
                        "Informações da bateria\n\n" +
                        obterEstadoBateria() +
                        "\n" +
                        obterTemperaturaBateria() +
                        "\n" +
                        obterSaudeBateria()
                )
        );
    }

    private void atualizarInterface() {

        textoRelogio.setText(
                new SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(new Date())
        );

        textoData.setText(
                new SimpleDateFormat("MMM. dd", Locale.getDefault())
                        .format(new Date())
        );

        textoBateria.setText("🔋 " + obterPorcentagemBateria() + "%");

        textoUso.setText(obterPorcentagemArmazenamento() + "% usado");

        textoLivre.setText("Livre " + obterEspacoLivre());

        textoTemperatura.setText(obterTemperaturaBateriaValor());

        textoStatus.setText("À sua disposição.\nSistemas online.");
    }

    private void iniciarReconhecimentoVoz() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Diga JARVIS e seu comando"
        );

        try {
            startActivityForResult(intent, PEDIDO_VOZ);
        } catch (ActivityNotFoundException e) {
            mostrarErroVoz(
                    "O reconhecimento de voz não está disponível neste aparelho."
            );
        } catch (Exception e) {
            mostrarErroVoz(
                    "Não foi possível iniciar o reconhecimento de voz."
            );
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != PEDIDO_VOZ) {
            return;
        }

        if (resultCode != RESULT_OK || data == null) {
            mostrarErroVoz("Não consegui ouvir o comando.");
            return;
        }

        try {

            List<String> resultados =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );

            if (resultados == null || resultados.isEmpty()) {
                mostrarErroVoz("Não consegui entender o que foi dito.");
                return;
            }

            String entrada = resultados.get(0);

            processarEntrada(entrada);

        } catch (Exception e) {
            mostrarErroVoz(
                    "Ocorreu um erro ao processar o comando."
            );
        }
    }

    private void processarEntrada(String entrada) {

        String normalizado = normalizar(entrada);

        if (normalizado.equals("jarvis")) {

            textoStatus.setText(
                    "À sua disposição.\nSistemas online.\nO que deseja?"
            );

            return;
        }

        if (!normalizado.startsWith("jarvis ")) {

            textoStatus.setText(
                    "Diga primeiro: JARVIS."
            );

            return;
        }

        String comando =
                normalizado.substring(7).trim();

        processarComando(comando);
    }

    private void processarComando(String comando) {

        if (comando.isEmpty()) {

            textoStatus.setText(
                    "À sua disposição.\nO que deseja?"
            );

            return;
        }

        StringBuilder resposta =
                new StringBuilder();

        if (ehBateria(comando)) {
            resposta.append(obterEstadoBateria())
                    .append("\n")
                    .append(obterTemperaturaBateria())
                    .append("\n")
                    .append(obterSaudeBateria())
                    .append("\n\n");
        }

        if (ehRam(comando)) {
            resposta.append(obterRam())
                    .append("\n\n");
        }

        if (ehArmazenamento(comando)) {
            resposta.append(obterArmazenamentoDetalhado())
                    .append("\n\n");
        }

        if (ehDataHora(comando)) {
            resposta.append(obterDataHora())
                    .append("\n\n");
        }

        if (ehModelo(comando)) {
            resposta.append(
                    "Modelo do telefone: "
                            + Build.MODEL
            ).append("\n\n");
        }

        if (ehAndroid(comando)) {
            resposta.append(obterAndroid())
                    .append("\n\n");
        }

        if (ehWiFi(comando)) {
            resposta.append(obterWiFi())
                    .append("\n\n");
        }

        if (ehAnaliseCompleta(comando)) {
            resposta.append(
                    obterAnaliseCompleta()
            ).append("\n\n");
        }

        if (ehComandoJogo(comando)) {
            resposta.append(
                    "Esse jogo não permite que eu acesse essas informações."
            ).append("\n\n");
        }

        if (resposta.length() == 0) {

            resposta.append(
                    "Ainda estou aprendendo esse comando.\n\n"
                            + "Posso consultar bateria, temperatura, "
                            + "RAM, armazenamento, data, hora, modelo, "
                            + "Android e conexão."
            );
        }

        textoStatus.setText(
                resposta.toString().trim()
        );

        atualizarInterface();
    }

    private String normalizar(String texto) {

        if (texto == null) {
            return "";
        }

        String resultado =
                texto.toLowerCase(Locale.ROOT)
                        .replace("á", "a")
                        .replace("à", "a")
                        .replace("ã", "a")
                        .replace("â", "a")
                        .replace("é", "e")
                        .replace("ê", "e")
                        .replace("í", "i")
                        .replace("ó", "o")
                        .replace("ô", "o")
                        .replace("õ", "o")
                        .replace("ú", "u")
                        .replace("ç", "c");

        resultado = resultado.replaceAll(
                "[\\p{Punct}]",
                " "
        );

        resultado = resultado.replaceAll(
                "\\s+",
                " "
        ).trim();

        return resultado;
    }

    private boolean ehBateria(String t) {

        return t.contains("bateria")
                || t.contains("carga")
                || t.contains("carregando");
    }

    private boolean ehRam(String t) {

        return t.matches(".*\\bram\\b.*")
                || t.contains("memoria ram")
                || t.contains("memoria de ram");
    }

    private boolean ehArmazenamento(String t) {

        return t.contains("armazenamento")
                || t.contains("espaco de armazenamento")
                || t.contains("memoria interna")
                || t.contains("espaco livre");
    }

    private boolean ehDataHora(String t) {

        return t.contains("data e hora")
                || t.contains("data hora")
                || t.equals("data")
                || t.equals("hora")
                || t.contains("que horas");
    }

    private boolean ehModelo(String t) {

        return t.contains("modelo")
                || t.contains("qual celular")
                || t.contains("qual telefone");
    }

    private boolean ehAndroid(String t) {

        return t.contains("versao do android")
                || t.equals("android")
                || t.contains("qual android");
    }

    private boolean ehWiFi(String t) {

        return t.equals("wifi")
                || t.contains("wi fi");
    }

    private boolean ehAnaliseCompleta(String t) {

        return t.contains("analise meu telefone")
                || t.contains("analise o meu telefone")
                || t.contains("estado do meu telefone")
                || t.contains("status do meu telefone");
    }

    private boolean ehComandoJogo(String t) {

        return t.contains("ping")
                || t.contains("fps")
                || t.contains("upload")
                || t.contains("download")
                || t.contains("velocidade do jogo");
    }

    private String obterPorcentagemBateria() {

        BatteryManager bm =
                (BatteryManager) getSystemService(
                        BATTERY_SERVICE
                );

        if (bm == null) {
            return "?";
        }

        int nivel =
                bm.getIntProperty(
                        BatteryManager.BATTERY_PROPERTY_CAPACITY
                );

        return String.valueOf(nivel);
    }

    private String obterEstadoBateria() {

        BatteryManager bm =
                (BatteryManager) getSystemService(
                        BATTERY_SERVICE
                );

        if (bm == null) {
            return "Bateria indisponível.";
        }

        int nivel =
                bm.getIntProperty(
                        BatteryManager.BATTERY_PROPERTY_CAPACITY
                );

        return "Bateria: " + nivel + "%";
    }

    private String obterTemperaturaBateria() {

        Intent intent =
                registerReceiver(
                        null,
                        new android.content.IntentFilter(
                                Intent.ACTION_BATTERY_CHANGED
                        )
                );

        if (intent == null) {
            return "Temperatura: indisponível.";
        }

        int temperatura =
                intent.getIntExtra(
                        BatteryManager.EXTRA_TEMPERATURE,
                        Integer.MIN_VALUE
                );

        if (temperatura == Integer.MIN_VALUE) {
            return "Temperatura: indisponível.";
        }

        float celsius =
                temperatura / 10.0f;

        return String.format(
                Locale.getDefault(),
                "Temperatura: %.1f°C",
                celsius
        );
    }

    private String obterTemperaturaBateriaValor() {

        Intent intent =
                registerReceiver(
                        null,
                        new android.content.IntentFilter(
                                Intent.ACTION_BATTERY_CHANGED
                        )
                );

        if (intent == null) {
            return "--°C";
        }

        int temperatura =
                intent.getIntExtra(
                        BatteryManager.EXTRA_TEMPERATURE,
                        Integer.MIN_VALUE
                );

        if (temperatura == Integer.MIN_VALUE) {
            return "--°C";
        }

        return String.format(
                Locale.getDefault(),
                "%.1f°C",
                temperatura / 10.0f
        );
    }

    private String obterSaudeBateria() {

        Intent intent =
                registerReceiver(
                        null,
                        new android.content.IntentFilter(
                                Intent.ACTION_BATTERY_CHANGED
                        )
                );

        if (intent == null) {
            return "Saúde da bateria: indisponível.";
        }

        int saude =
                intent.getIntExtra(
                        BatteryManager.EXTRA_HEALTH,
                        -1
                );

        String texto;

        switch (saude) {

            case BatteryManager.BATTERY_HEALTH_GOOD:
                texto = "Boa";
                break;

            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                texto = "Superaquecida";
                break;

            case BatteryManager.BATTERY_HEALTH_DEAD:
                texto = "Falha";
                break;

            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                texto = "Sobretensão";
                break;

            case BatteryManager.BATTERY_HEALTH_COLD:
                texto = "Fria";
                break;

            default:
                texto = "Desconhecida";
                break;
        }

        return "Saúde da bateria: " + texto;
    }

    private String obterRam() {

        ActivityManager manager =
                (ActivityManager)
                        getSystemService(
                                Context.ACTIVITY_SERVICE
                        );

        if (manager == null) {
            return "RAM indisponível.";
        }

        ActivityManager.MemoryInfo info =
                new ActivityManager.MemoryInfo();

        manager.getMemoryInfo(info);

        long total =
                info.totalMem / (1024 * 1024);

        long disponivel =
                info.availMem / (1024 * 1024);

        long usada =
                total - disponivel;

        return "RAM: "
                + usada
                + " MB usada de "
                + total
                + " MB";
    }

    private String obterArmazenamentoDetalhado() {

        StatFs stat =
                new StatFs(
                        Environment.getDataDirectory()
                                .getPath()
                );

        long totalBytes =
                stat.getTotalBytes();

        long livreBytes =
                stat.getAvailableBytes();

        long usadoBytes =
                totalBytes - livreBytes;

        return "Armazenamento: "
                + formatarGB(usadoBytes)
                + " usados de "
                + formatarGB(totalBytes)
                + "\nLivre: "
                + formatarGB(livreBytes);
    }

    private String obterPorcentagemArmazenamento() {

        StatFs stat =
                new StatFs(
                        Environment.getDataDirectory()
                                .getPath()
                );

        long total =
                stat.getTotalBytes();

        long livre =
                stat.getAvailableBytes();

        if (total <= 0) {
            return "0";
        }

        long usado =
                total - livre;

        return String.valueOf(
                (usado * 100L) / total
        );
    }

    private String obterEspacoLivre() {

        StatFs stat =
                new StatFs(
                        Environment.getDataDirectory()
                                .getPath()
                );

        return formatarGB(
                stat.getAvailableBytes()
        );
    }

    private String formatarGB(long bytes) {

        double gb =
                bytes / 1073741824.0;

        return String.format(
                Locale.getDefault(),
                "%.1f GB",
                gb
        );
    }

    private String obterDataHora() {

        return new SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());
    }

    private String obterAndroid() {

        return "Android "
                + Build.VERSION.RELEASE
                + " (API "
                + Build.VERSION.SDK_INT
                + ")";
    }

    private String obterWiFi() {

        ConnectivityManager cm =
                (ConnectivityManager)
                        getSystemService(
                                CONNECTIVITY_SERVICE
                        );

        if (cm == null) {
            return "Conexão indisponível.";
        }

        Network rede =
                cm.getActiveNetwork();

        if (rede == null) {
            return "Nenhuma conexão ativa.";
        }

        NetworkCapabilities capacidades =
                cm.getNetworkCapabilities(rede);

        if (capacidades == null) {
            return "Conexão desconhecida.";
        }

        if (capacidades.hasTransport(
                NetworkCapabilities.TRANSPORT_WIFI)) {

            return "Conexão: Wi-Fi";
        }

        if (capacidades.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR)) {

            return "Conexão: rede móvel";
        }

        return "Conexão ativa.";
    }

    private String obterAnaliseCompleta() {

        return "ANÁLISE DO TELEFONE\n\n"
                + obterEstadoBateria()
                + "\n"
                + obterTemperaturaBateria()
                + "\n"
                + obterSaudeBateria()
                + "\n\n"
                + obterRam()
                + "\n\n"
                + obterArmazenamentoDetalhado();
    }

    private void mostrarConfiguracoes() {

        textoStatus.setText(
                "CONFIGURAÇÕES JARVIS\n\n"
                        + "Gerenciar JARVIS\n"
                        + "Privacidade\n"
                        + "Alterar voz do JARVIS\n\n"
                        + "Essas funções serão conectadas "
                        + "nas próximas etapas."
        );
    }

    private void mostrarErroVoz(String mensagem) {

        textoStatus.setText(
                mensagem
                        + "\n\nToque no reator para tentar novamente."
        );
    }
}
