package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.speech.RecognizerIntent;
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
                "\nSistemas online.\n\n" +
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

        String t = normalizar(texto).trim();

        return t.equals("jarvis") ||
               t.startsWith("jarvis ") ||
               t.startsWith("jarvis,");
    }

    private boolean ehAnaliseCompleta(String texto) {

        String t = normalizar(texto);

        return (t.contains("analise completa") &&
                (t.contains("telefone") ||
                 t.contains("celular") ||
                 t.contains("aparelho"))) ||

               (t.contains("analisa") &&
                (t.contains("telefone") ||
                 t.contains("celular") ||
                 t.contains("aparelho")) &&
                t.contains("tudo"));
    }

    private boolean ehTudoQueSabe(String texto) {

        String t = normalizar(texto);

        return (t.contains("tudo que voce sabe") &&
                (t.contains("telefone") ||
                 t.contains("celular") ||
                 t.contains("aparelho"))) ||

               (t.contains("tudo sobre meu telefone")) ||
               (t.contains("tudo sobre meu celular")) ||
               (t.contains("todas as informacoes") &&
                (t.contains("telefone") ||
                 t.contains("celular") ||
                 t.contains("aparelho"))) ||

               (t.contains("o que voce sabe sobre meu celular")) ||
               (t.contains("o que voce sabe sobre meu telefone"));
    }

    private boolean ehTemperaturaBateria(String texto) {

        String t = normalizar(texto);

        return t.contains("temperatura da bateria") ||
               t.contains("temperatura da minha bateria") ||
               t.contains("temperatura bateria") ||
               t.contains("temperatura da bateria") ||
               t.contains("quao quente esta a bateria") ||
               t.contains("quao quente esta minha bateria");
    }

    private boolean ehPorcentagemBateria(String texto) {

        String t = normalizar(texto);

        return t.contains("porcentagem da bateria") ||
               t.contains("porcentagem da minha bateria") ||
               t.contains("percentual da bateria") ||
               t.contains("quanto de bateria tenho") ||
               t.contains("quanto de carga tenho") ||
               t.contains("quanto resta de bateria") ||
               t.contains("quanto ainda tenho de bateria") ||
               t.contains("qual o nivel da bateria") ||
               t.contains("qual o nivel de bateria") ||
               t.contains("quantos por cento de bateria") ||
               t.equals("qual a porcentagem") ||
               t.equals("qual a porcentagem da bateria");
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
               t.contains("que celular eu tenho") ||
               t.contains("qual meu modelo") ||
               t.contains("qual o modelo do meu telefone");
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
               t.contains("qual android esta instalado") ||
               t.contains("qual versao do sistema");
    }

    private boolean ehArmazenamento(String texto) {

        String t = normalizar(texto);

        return t.contains("armazenamento") ||
               t.contains("armazenamento interno") ||
               t.contains("espaco de armazenamento") ||
               t.contains("memoria de armazenamento") ||
               t.contains("memoria do celular") ||
               t.contains("memoria interna") ||
               t.contains("espaco livre") ||
               t.contains("espaco disponivel") ||
               t.contains("quanto espaco") ||
               t.contains("quanto armazenamento") ||
               t.contains("quanto de espaco") ||
               t.contains("quanto de armazenamento");
    }

    private boolean ehRAM(String texto) {

        String t = normalizar(texto);

        return t.contains("ram") ||
               t.contains("memoria ram") ||
               t.contains("memoria de ram") ||
               t.contains("quanto de memoria") ||
               t.contains("quanta memoria") ||
               t.contains("quantidade de ram") ||
               t.contains("memoria disponivel");
    }

    private boolean ehHora(String texto) {

        String t = normalizar(texto);

        return t.contains("que horas sao") ||
               t.contains("qual e a hora") ||
               t.contains("qual a hora") ||
               t.contains("que hora e") ||
               t.contains("me diga a hora") ||
               t.contains("horario atual") ||
               t.contains("hora atual");
    }

    private boolean ehData(String texto) {

        String t = normalizar(texto);

        return t.contains("que dia e hoje") ||
               t.contains("qual e a data de hoje") ||
               t.contains("qual a data de hoje") ||
               t.contains("qual a data") ||
               t.contains("data de hoje") ||
               t.contains("me diga a data") ||
               t.contains("dia de hoje") ||
               t.contains("data atual");
    }

    private boolean ehAtualizacao(String texto) {

        String t = normalizar(texto);

        return t.contains("atualizacao do android") ||
               t.contains("atualizacao do software") ||
               t.contains("atualizacao do sistema") ||
               t.contains("atualizacao disponivel") ||
               t.contains("tem atualizacao") ||
               t.contains("existe atualizacao") ||
               t.contains("meu celular tem atualizacao") ||
               t.contains("meu telefone tem atualizacao");
    }

    private boolean ehWiFi(String texto) {

        String t = normalizar(texto);

        return t.contains("wifi") ||
               t.contains("wi fi") ||
               t.contains("wi-fi");
    }

    private boolean ehBluetooth(String texto) {

        return normalizar(texto).contains("bluetooth");
    }

    private boolean ehModoAviao(String texto) {

        String t = normalizar(texto);

        return t.contains("modo aviao") ||
               t.contains("modo avião");
    }

    private boolean ehJogo(String texto) {

        String t = normalizar(texto);

        return t.contains("analise o jogo") ||
               t.contains("analisa o jogo") ||
               t.contains("analise meu jogo") ||
               t.contains("analisa meu jogo") ||
               t.contains("verifica o jogo") ||
               t.contains("dados do jogo") ||
               t.contains("meu jogo");
    }

    private boolean ehFPS(String texto) {

        String t = normalizar(texto);

        return t.contains("fps") ||
               t.contains("frames por segundo") ||
               t.contains("frame por segundo") ||
               t.contains("taxa de quadros");
    }

    private boolean ehPing(String texto) {

        String t = normalizar(texto);

        return t.contains("ping") ||
               t.contains("latencia") ||
               t.contains("latência");
    }

    private boolean ehEnvio(String texto) {

        String t = normalizar(texto);

        return t.contains("velocidade de envio") ||
               t.contains("taxa de envio") ||
               t.contains("velocidade de upload") ||
               t.contains("taxa de upload") ||
               t.contains("quanto estou enviando");
    }

    private boolean ehRecebimento(String texto) {

        String t = normalizar(texto);

        return t.contains("velocidade de recebimento") ||
               t.contains("taxa de recebimento") ||
               t.contains("velocidade de download") ||
               t.contains("taxa de download") ||
               t.contains("quanto estou recebendo");
    }

    private String obterPorcentagemBateria() {

        BatteryManager bateria =
                (BatteryManager) getSystemService(BATTERY_SERVICE);

        int porcentagem = bateria.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CAPACITY
        );

        return "Bateria: " + porcentagem + "%.";
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

        return "Temperatura da bateria: " +
                String.format(
                        Locale.US,
                        "%.1f",
                        temperatura / 10.0
                ) +
                " °C.";
    }

    private String obterCarregamento() {

        Intent intentBateria = obterInformacoesBateria();

        if (intentBateria == null) {
            return "Não foi possível determinar o carregamento.";
        }

        int status = intentBateria.getIntExtra(
                BatteryManager.EXTRA_STATUS,
                -1
        );

        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            return "A bateria está carregando.";
        }

        if (status == BatteryManager.BATTERY_STATUS_FULL) {
            return "A bateria está com carga completa.";
        }

        return "A bateria não está carregando.";
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
                return "Saúde da bateria: normal.";

            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                return "O sistema reporta superaquecimento da bateria.";

            case BatteryManager.BATTERY_HEALTH_DEAD:
                return "O sistema reporta uma falha grave na bateria.";

            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                return "O sistema reporta sobretensão.";

            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                return "O sistema reporta uma falha não especificada.";

            case BatteryManager.BATTERY_HEALTH_COLD:
                return "O sistema reporta que a bateria está muito fria.";

            default:
                return "Não foi possível determinar a saúde da bateria.";
        }
    }

    private String obterEstadoBateria() {

        return obterPorcentagemBateria() + "\n" +
                obterCarregamento() + "\n" +
                obterTemperaturaBateria() + "\n" +
                obterSaudeBateria();
    }

    private String obterModelo() {

        return "Modelo: " + Build.MODEL + ".\n" +
               "Fabricante: " + Build.MANUFACTURER + ".";
    }

    private String obterAndroid() {

        return "Android: " + Build.VERSION.RELEASE + ".\n" +
               "API: " + Build.VERSION.SDK_INT + ".";
    }

    private String formatarGB(long bytes) {

        double gb = bytes /
                (1024.0 * 1024.0 * 1024.0);

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

        return "Armazenamento livre: " +
                formatarGB(livre) +
                ".\nArmazenamento total: " +
                formatarGB(total) +
                ".";
    }

    private String obterRAM() {

        ActivityManager gerenciador =
                (ActivityManager) getSystemService(
                        ACTIVITY_SERVICE
                );

        ActivityManager.MemoryInfo memoria =
                new ActivityManager.MemoryInfo();

        gerenciador.getMemoryInfo(memoria);

        return "RAM disponível: " +
                formatarGB(memoria.availMem) +
                ".\nRAM total: " +
                formatarGB(memoria.totalMem) +
                ".";
    }

    private String obterHora() {

        return "Hora atual: " +
                new SimpleDateFormat(
                        "HH:mm",
                        Locale.getDefault()
                ).format(new Date()) +
                ".";
    }

    private String obterData() {

        return "Data: " +
                new SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                ).format(new Date()) +
                ".";
    }

    private String obterWiFi() {

        return "O estado detalhado da conexão Wi-Fi depende das informações que o Android disponibiliza ao aplicativo.";
    }

    private String obterBluetooth() {

        return "O estado detalhado do Bluetooth será adicionado em uma etapa específica.";
    }

    private String obterModoAviao() {

        return "O estado detalhado do modo avião será adicionado em uma etapa específica.";
    }

    private String respostaAtualizacao() {

        return "Não tenho permissão para acessar informações mais internas do aparelho.";
    }

    private String respostaJogoIndisponivel() {

        return "Esse jogo não permite que eu acesse essas informações.";
    }

    private String analiseCompleta() {

        return "ANÁLISE COMPLETA DO TELEFONE\n\n" +
                obterModelo() + "\n\n" +
                obterAndroid() + "\n\n" +
                obterEstadoBateria() + "\n\n" +
                obterRAM() + "\n\n" +
                obterArmazenamento() + "\n\n" +
                obterHora() + "\n" +
                obterData() + "\n\n" +
                "Algumas informações internas do aparelho não podem ser acessadas por mim.";
    }

    private String tudoQueSabe() {

        return "TUDO QUE CONSIGO ACESSAR SOBRE O TELEFONE\n\n" +
                obterModelo() + "\n\n" +
                obterAndroid() + "\n\n" +
                obterEstadoBateria() + "\n\n" +
                obterRAM() + "\n\n" +
                obterArmazenamento() + "\n\n" +
                obterHora() + "\n" +
                obterData() + "\n\n" +
                "Algumas informações internas do aparelho não podem ser acessadas por mim.";
    }

    private void adicionarResposta(
            StringBuilder resultado,
            String texto) {

        if (resultado.length() > 0) {
            resultado.append("\n\n");
        }

        resultado.append(texto);
    }

    private void processarComando(String comando) {

        String t = normalizar(comando);

        StringBuilder resultado = new StringBuilder();

        if (ehAtualizacao(t)) {
            adicionarResposta(
                    resultado,
                    respostaAtualizacao()
            );
        }

        if (ehAnaliseCompleta(t) || ehTudoQueSabe(t)) {

            if (ehTudoQueSabe(t)) {
                responder(tudoQueSabe());
            } else {
                responder(analiseCompleta());
            }

            return;
        }

        if (ehJogo(t)) {
            adicionarResposta(
                    resultado,
                    respostaJogoIndisponivel()
            );
        }

        if (ehTemperaturaBateria(t)) {
            adicionarResposta(
                    resultado,
                    obterTemperaturaBateria()
            );
        }

        if (ehPorcentagemBateria(t)) {
            adicionarResposta(
                    resultado,
                    obterPorcentagemBateria()
            );
        }

        if (ehCarregamento(t)) {
            adicionarResposta(
                    resultado,
                    obterCarregamento()
            );
        }

        if (ehSaudeBateria(t)) {
            adicionarResposta(
                    resultado,
                    obterSaudeBateria()
            );
        }

        if (ehEstadoBateria(t)) {
            adicionarResposta(
                    resultado,
                    obterEstadoBateria()
            );
        }

        if (ehModelo(t)) {
            adicionarResposta(
                    resultado,
                    obterModelo()
            );
        }

        if (ehAndroid(t)) {
            adicionarResposta(
                    resultado,
                    obterAndroid()
            );
        }

        if (ehArmazenamento(t)) {
            adicionarResposta(
                    resultado,
                    obterArmazenamento()
            );
        }

        if (ehRAM(t)) {
            adicionarResposta(
                    resultado,
                    obterRAM()
            );
        }

        if (ehHora(t)) {
            adicionarResposta(
                    resultado,
                    obterHora()
            );
        }

        if (ehData(t)) {
            adicionarResposta(
                    resultado,
                    obterData()
            );
        }

        if (ehWiFi(t)) {
            adicionarResposta(
                    resultado,
                    obterWiFi()
            );
        }

        if (ehBluetooth(t)) {
            adicionarResposta(
                    resultado,
                    obterBluetooth()
            );
        }

        if (ehModoAviao(t)) {
            adicionarResposta(
                    resultado,
                    obterModoAviao()
            );
        }

        /*
         * Os comandos de FPS, ping, envio e recebimento
         * ficam preparados, mas não inventam dados.
         */
        if (ehFPS(t) ||
            ehPing(t) ||
            ehEnvio(t) ||
            ehRecebimento(t)) {

            adicionarResposta(
                    resultado,
                    respostaJogoIndisponivel()
            );
        }

        if (resultado.length() == 0) {

            responder(
                    "Comando não reconhecido.\n\n" +
                    "Aguardando ativação..."
            );

        } else {

            responder(resultado.toString());
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

                } else {

                    processarComando(comando);
                }
            }
        }
    }
}
