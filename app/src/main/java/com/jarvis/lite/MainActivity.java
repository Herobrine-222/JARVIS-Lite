package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int PEDIDO_AUDIO = 100;
    private TextView resposta;
    private BatteryManager bateria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bateria = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView titulo = new TextView(this);
        titulo.setText("JARVIS Lite");
        titulo.setTextSize(28);
        titulo.setGravity(Gravity.CENTER);
        layout.addView(titulo);

        resposta = new TextView(this);
        resposta.setText("Sistemas online.\nPressione o botão e diga \"JARVIS\".");
        resposta.setTextSize(18);
        resposta.setPadding(10, 30, 10, 30);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(resposta);

        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                );

        layout.addView(scroll, scrollParams);

        Button ouvir = new Button(this);
        ouvir.setText("OUVIR JARVIS");
        ouvir.setTextSize(18);

        ouvir.setOnClickListener(v -> ouvir());

        layout.addView(ouvir);

        setContentView(layout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    200
            );
        }
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
                "Diga seu comando..."
        );

        try {
            startActivityForResult(intent, PEDIDO_AUDIO);
        } catch (Exception e) {
            resposta.setText(
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

        if (requestCode != PEDIDO_AUDIO ||
                resultCode != RESULT_OK ||
                data == null) {
            return;
        }

        ArrayList<String> resultados =
                data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                );

        if (resultados == null || resultados.isEmpty()) {
            resposta.setText("Não consegui entender.");
            return;
        }

        String comando = resultados.get(0);
        processarEntrada(comando);
    }

    private void processarEntrada(String comandoOriginal) {

        String comando = normalizar(comandoOriginal);

        /*
         * Apenas "JARVIS" = ativação.
         */
        if (comando.equals("jarvis")) {

            resposta.setText(
                    "À sua disposição.\n" +
                    "Sistemas online.\n" +
                    "O que deseja?"
            );

            ouvir();
            return;
        }

        /*
         * Permite também:
         * "JARVIS qual minha bateria?"
         * "JARVIS, qual meu Android?"
         */
        if (comando.startsWith("jarvis ")) {
            comando = comando.substring(7).trim();
        } else if (comando.startsWith("jarvis,")) {
            comando = comando.substring(7).trim();
        }

        if (comando.isEmpty()) {
            resposta.setText(
                    "À sua disposição.\n" +
                    "Sistemas online.\n" +
                    "O que deseja?"
            );
            return;
        }

        processarComando(comando);
    }

    private void processarComando(String comando) {

        StringBuilder resultado = new StringBuilder();

        boolean respondeu = false;

        /*
         * ANÁLISE COMPLETA
         */
        if (ehAnaliseCompleta(comando)) {

            resultado.append(analiseCompleta());
            respondeu = true;

        } else if (ehTudoQueSabe(comando)) {

            resultado.append(tudoQueSabe());
            respondeu = true;

        } else {

            /*
             * BATERIA
             */
            if (ehTemperaturaBateria(comando)) {

                resultado.append(
                        "Temperatura da bateria: "
                                + temperaturaBateria()
                                + "\n"
                );

                respondeu = true;

            } else if (ehPorcentagemBateria(comando)) {

                resultado.append(
                        "Bateria: "
                                + porcentagemBateria()
                                + "%\n"
                );

                respondeu = true;

            } else if (ehCarregando(comando)) {

                resultado.append(
                        estadoCarregamento()
                                + "\n"
                );

                respondeu = true;

            } else if (ehSaudeBateria(comando)) {

                resultado.append(
                        saudeBateria()
                                + "\n"
                );

                respondeu = true;

            } else if (ehEstadoBateria(comando)) {

                resultado.append(
                        estadoCompletoBateria()
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * CELULAR
             */
            if (ehModelo(comando)) {

                resultado.append(
                        "Celular: "
                                + Build.MANUFACTURER
                                + " "
                                + Build.MODEL
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * ANDROID
             */
            if (ehAndroid(comando)) {

                resultado.append(
                        "Android: "
                                + Build.VERSION.RELEASE
                                + "\n"
                                + "API: "
                                + Build.VERSION.SDK_INT
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * ARMAZENAMENTO
             */
            if (ehArmazenamento(comando)) {

                resultado.append(
                        armazenamento()
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * RAM
             */
            if (ehRAM(comando)) {

                resultado.append(
                        memoriaRAM()
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * HORA
             */
            if (ehHora(comando)) {

                resultado.append(
                        "Hora atual: "
                                + horaAtual()
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * DATA
             */
            if (ehData(comando)) {

                resultado.append(
                        "Data de hoje: "
                                + dataAtual()
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * ATUALIZAÇÕES
             */
            if (ehAtualizacao(comando)) {

                resultado.append(
                        "Não tenho permissão para acessar "
                                + "informações mais internas do aparelho.\n"
                );

                respondeu = true;
            }

            /*
             * WI-FI
             */
            if (ehWiFi(comando)) {

                resultado.append(
                        "O estado detalhado da conexão Wi-Fi "
                                + "depende das informações que o Android "
                                + "disponibiliza ao aplicativo.\n"
                );

                respondeu = true;
            }

            /*
             * BLUETOOTH
             */
            if (ehBluetooth(comando)) {

                resultado.append(
                        "O estado detalhado do Bluetooth "
                                + "será adicionado em uma etapa específica.\n"
                );

                respondeu = true;
            }

            /*
             * MODO AVIÃO
             */
            if (ehModoAviao(comando)) {

                resultado.append(
                        "O estado detalhado do modo avião "
                                + "será adicionado em uma etapa específica.\n"
                );

                respondeu = true;
            }

            /*
             * JOGOS
             */
            if (ehComandoJogo(comando)) {

                resultado.append(
                        "Esse jogo não permite que eu acesse "
                                + "essas informações.\n"
                );

                respondeu = true;
            }
        }

        if (!respondeu) {

            resultado.append(
                    "Ainda não tenho um comando para isso."
            );
        }

        resposta.setText(resultado.toString());
    }

    /*
     * ============================
     * NORMALIZAÇÃO
     * ============================
     */

    private String normalizar(String texto) {

        texto = texto.toLowerCase(Locale.ROOT).trim();

        texto = texto
                .replace("á", "a")
                .replace("à", "a")
                .replace("ã", "a")
                .replace("â", "a")
                .replace("ä", "a")
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e")
                .replace("ë", "e")
                .replace("í", "i")
                .replace("ì", "i")
                .replace("î", "i")
                .replace("ï", "i")
                .replace("ó", "o")
                .replace("ò", "o")
                .replace("õ", "o")
                .replace("ô", "o")
                .replace("ö", "o")
                .replace("ú", "u")
                .replace("ù", "u")
                .replace("û", "u")
                .replace("ü", "u")
                .replace("ç", "c");

        return texto;
    }

    /*
     * ============================
     * COMANDOS ESPECIAIS
     * ============================
     */

    private boolean ehAnaliseCompleta(String t) {

        return t.contains("analise completa do meu telefone")
                || t.contains("analise completa do meu celular")
                || t.contains("analise completa do aparelho")
                || t.contains("analise completa do telefone")
                || t.contains("analisa completamente meu telefone")
                || t.contains("analise meu telefone completamente");
    }

    private boolean ehTudoQueSabe(String t) {

        return t.contains("tudo que voce sabe sobre meu telefone")
                || t.contains("tudo que voce sabe sobre meu celular")
                || t.contains("o que voce sabe sobre meu celular")
                || t.contains("o que voce sabe sobre meu telefone")
                || t.contains("todas as informacoes que voce tem sobre meu aparelho")
                || t.contains("tudo sobre o meu telefone")
                || t.contains("tudo sobre meu celular");
    }

    /*
     * ============================
     * BATERIA
     * ============================
     */

    private boolean ehTemperaturaBateria(String t) {

        return t.contains("temperatura da bateria")
                || t.contains("temperatura bateria")
                || t.contains("temperatura da minha bateria")
                || t.contains("quanto esta a temperatura da bateria");
    }

    private boolean ehPorcentagemBateria(String t) {

        return t.contains("porcentagem da bateria")
                || t.contains("porcentagem de bateria")
                || t.contains("quanto de bateria tenho")
                || t.contains("quanto resta de bateria")
                || t.contains("quanto de carga tenho")
                || t.equals("bateria")
                || t.contains("percentual da bateria");
    }

    private boolean ehCarregando(String t) {

        return t.contains("esta carregando")
                || t.contains("esta meu celular carregando")
                || t.contains("meu celular esta carregando")
                || t.contains("esta carregando o celular");
    }

    private boolean ehSaudeBateria(String t) {

        return t.contains("saude da bateria")
                || t.contains("saude bateria")
                || t.contains("bateria esta saudavel")
                || t.contains("bateria saudavel");
    }

    private boolean ehEstadoBateria(String t) {

        return t.contains("estado da bateria")
                || t.contains("como esta a bateria")
                || t.contains("status da bateria")
                || t.contains("situacao da bateria");
    }

    private String porcentagemBateria() {

        if (bateria == null) {
            return "Não consegui acessar a porcentagem da bateria.";
        }

        int nivel = bateria.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CAPACITY
        );

        return String.valueOf(nivel);
    }

    private String temperaturaBateria() {

        Intent intent = registerReceiver(
                null,
                new android.content.IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED
                )
        );

        if (intent == null) {
            return "Não consegui acessar a temperatura da bateria.";
        }

        int temperatura =
                intent.getIntExtra(
                        BatteryManager.EXTRA_TEMPERATURE,
                        -1
                );

        if (temperatura < 0) {
            return "Temperatura da bateria indisponível.";
        }

        double celsius = temperatura / 10.0;

        return String.format(
                Locale.US,
                "%.1f °C",
                celsius
        );
    }

    private String estadoCarregamento() {

        Intent intent = registerReceiver(
                null,
                new android.content.IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED
                )
        );

        if (intent == null) {
            return "Não consegui verificar o carregamento.";
        }

        int status =
                intent.getIntExtra(
                        BatteryManager.EXTRA_STATUS,
                        -1
                );

        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            return "Sim. O celular está carregando.";
        }

        if (status == BatteryManager.BATTERY_STATUS_FULL) {
            return "A bateria está cheia.";
        }

        return "Não. O celular não está carregando.";
    }

    private String saudeBateria() {

        Intent intent = registerReceiver(
                null,
                new android.content.IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED
                )
        );

        if (intent == null) {
            return "Não consegui acessar o estado da bateria.";
        }

        int status =
                intent.getIntExtra(
                        BatteryManager.EXTRA_HEALTH,
                        -1
                );

        switch (status) {

            case BatteryManager.BATTERY_HEALTH_GOOD:
                return "Saúde da bateria: boa.";

            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                return "Saúde da bateria: temperatura elevada.";

            case BatteryManager.BATTERY_HEALTH_DEAD:
                return "Saúde da bateria: estado crítico.";

            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                return "Saúde da bateria: tensão elevada.";

            case BatteryManager.BATTERY_HEALTH_COLD:
                return "Saúde da bateria: temperatura muito baixa.";

            default:
                return "Saúde da bateria: informação indisponível.";
        }
    }

    private String estadoCompletoBateria() {

        return "ESTADO DA BATERIA\n"
                + "Carga: " + porcentagemBateria() + "%\n"
                + "Temperatura: " + temperaturaBateria() + "\n"
                + estadoCarregamento() + "\n"
                + saudeBateria();
    }

    /*
     * ============================
     * CELULAR
     * ============================
     */

    private boolean ehModelo(String t) {

        return t.contains("qual e meu celular")
                || t.contains("qual meu celular")
                || t.contains("qual o modelo do aparelho")
                || t.contains("qual o modelo do celular")
                || t.contains("qual o modelo do telefone")
                || t.contains("modelo do aparelho")
                || t.contains("modelo do celular");
    }

    private boolean ehAndroid(String t) {

        return t.contains("qual android eu tenho")
                || t.contains("qual a versao do meu android")
                || t.contains("que android esta instalado")
                || t.contains("qual versao do android")
                || t.contains("android instalado")
                || t.contains("versao android");
    }

    /*
     * ============================
     * ARMAZENAMENTO
     * ============================
     */

    private boolean ehArmazenamento(String t) {

        return t.contains("quanto espaco tenho")
                || t.contains("quanto armazenamento")
                || t.contains("armazenamento disponivel")
                || t.contains("armazenamento interno")
                || t.contains("espaco livre")
                || t.contains("espaco disponivel")
                || t.contains("quanto de armazenamento")
                || t.contains("memoria de armazenamento");
    }

    private String armazenamento() {

        StatFs statFs = new StatFs(
                Environment.getDataDirectory().getPath()
        );

        long total = statFs.getTotalBytes();
        long livre = statFs.getAvailableBytes();

        return "ARMAZENAMENTO\n"
                + "Total: " + formatarGB(total) + "\n"
                + "Disponível: " + formatarGB(livre);
    }

    /*
     * ============================
     * RAM
     * ============================
     */

    private boolean ehRAM(String t) {

        return t.contains("quanto de ram")
                || t.contains("quanta ram")
                || t.contains("memoria ram")
                || t.contains("quantidade de ram")
                || t.contains("ram disponivel")
                || t.contains("memoria ram disponivel");
    }

    private String memoriaRAM() {

        android.app.ActivityManager manager =
                (android.app.ActivityManager)
                        getSystemService(
                                Context.ACTIVITY_SERVICE
                        );

        if (manager == null) {
            return "Não consegui acessar a memória RAM.";
        }

        android.app.ActivityManager.MemoryInfo info =
                new android.app.ActivityManager.MemoryInfo();

        manager.getMemoryInfo(info);

        long total = info.totalMem;
        long disponivel = info.availMem;

        return "MEMÓRIA RAM\n"
                + "Total: " + formatarGB(total) + "\n"
                + "Disponível: " + formatarGB(disponivel);
    }

    /*
     * ============================
     * DATA E HORA
     * ============================
     */

    private boolean ehHora(String t) {

        return t.contains("que horas sao")
                || t.contains("qual e a hora")
                || t.equals("hora")
                || t.contains("hora atual");
    }

    private boolean ehData(String t) {

        return t.contains("que dia e hoje")
                || t.contains("qual e a data de hoje")
                || t.contains("data de hoje")
                || t.equals("data");
    }

    private String horaAtual() {

        return new SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());
    }

    private String dataAtual() {

        return new SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
        ).format(new Date());
    }

    /*
     * ============================
     * ATUALIZAÇÕES
     * ============================
     */

    private boolean ehAtualizacao(String t) {

        return t.contains("tem atualizacao do android")
                || t.contains("tem atualizacao do software")
                || t.contains("meu celular tem atualizacao")
                || t.contains("existe alguma atualizacao")
                || t.contains("atualizacao do sistema");
    }

    /*
     * ============================
     * CONECTIVIDADE
     * ============================
     */

    private boolean ehWiFi(String t) {

        return t.contains("wifi")
                || t.contains("wi-fi")
                || t.contains("estado do wifi")
                || t.contains("wifi conectado");
    }

    private boolean ehBluetooth(String t) {

        return t.contains("bluetooth")
                || t.contains("estado do bluetooth");
    }

    private boolean ehModoAviao(String t) {

        return t.contains("modo aviao")
                || t.contains("modo avião");
    }

    /*
     * ============================
     * JOGOS
     * ============================
     */

    private boolean ehComandoJogo(String t) {

        return t.contains("fps")
                || t.contains("frames por segundo")
                || t.contains("taxa de quadros")
                || t.contains("ping")
                || t.contains("latencia")
                || t.contains("velocidade de envio")
                || t.contains("upload")
                || t.contains("velocidade de recebimento")
                || t.contains("download")
                || t.contains("analise o jogo")
                || t.contains("analisa meu jogo")
                || t.contains("dados do jogo")
                || t.contains("analise do jogo")
                || t.contains("qual meu fps")
                || t.contains("qual meu ping");
    }

    /*
     * ============================
     * ANÁLISE COMPLETA
     * ============================
     */

    private String analiseCompleta() {

        return "ANÁLISE COMPLETA DO TELEFONE\n\n"
                + "Modelo: "
                + Build.MANUFACTURER
                + " "
                + Build.MODEL
                + "\n"
                + "Android: "
                + Build.VERSION.RELEASE
                + "\n"
                + "API: "
                + Build.VERSION.SDK_INT
                + "\n\n"
                + estadoCompletoBateria()
                + "\n\n"
                + armazenamento()
                + "\n\n"
                + memoriaRAM()
                + "\n\n"
                + "Data: "
                + dataAtual()
                + "\n"
                + "Hora: "
                + horaAtual()
                + "\n\n"
                + "INFORMAÇÕES NÃO ACESSÍVEIS\n"
                + "Não tenho permissão para acessar "
                + "informações mais internas do aparelho.";
    }

    private String tudoQueSabe() {

        return "TUDO QUE CONSIGO ACESSAR SOBRE O TELEFONE\n\n"
                + "Fabricante: "
                + Build.MANUFACTURER
                + "\n"
                + "Modelo: "
                + Build.MODEL
                + "\n"
                + "Android: "
                + Build.VERSION.RELEASE
                + "\n"
                + "API: "
                + Build.VERSION.SDK_INT
                + "\n\n"
                + estadoCompletoBateria()
                + "\n\n"
                + armazenamento()
                + "\n\n"
                + memoriaRAM()
                + "\n\n"
                + "Data: "
                + dataAtual()
                + "\n"
                + "Hora: "
                + horaAtual()
                + "\n\n"
                + "Limitação: não tenho acesso "
                + "a informações internas protegidas "
                + "por outros aplicativos ou pelo sistema.";
    }

    /*
     * ============================
     * UTILITÁRIOS
     * ============================
     */

    private String formatarGB(long bytes) {

        double gb =
                bytes / (1024.0 * 1024.0 * 1024.0);

        return String.format(
                Locale.US,
                "%.2f GB",
                gb
        );
    }
                          }package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int PEDIDO_AUDIO = 100;
    private TextView resposta;
    private BatteryManager bateria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bateria = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView titulo = new TextView(this);
        titulo.setText("JARVIS Lite");
        titulo.setTextSize(28);
        titulo.setGravity(Gravity.CENTER);
        layout.addView(titulo);

        resposta = new TextView(this);
        resposta.setText("Sistemas online.\nPressione o botão e diga \"JARVIS\".");
        resposta.setTextSize(18);
        resposta.setPadding(10, 30, 10, 30);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(resposta);

        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                );

        layout.addView(scroll, scrollParams);

        Button ouvir = new Button(this);
        ouvir.setText("OUVIR JARVIS");
        ouvir.setTextSize(18);

        ouvir.setOnClickListener(v -> ouvir());

        layout.addView(ouvir);

        setContentView(layout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    200
            );
        }
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
                "Diga seu comando..."
        );

        try {
            startActivityForResult(intent, PEDIDO_AUDIO);
        } catch (Exception e) {
            resposta.setText(
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

        if (requestCode != PEDIDO_AUDIO ||
                resultCode != RESULT_OK ||
                data == null) {
            return;
        }

        ArrayList<String> resultados =
                data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                );

        if (resultados == null || resultados.isEmpty()) {
            resposta.setText("Não consegui entender.");
            return;
        }

        String comando = resultados.get(0);
        processarEntrada(comando);
    }

    private void processarEntrada(String comandoOriginal) {

        String comando = normalizar(comandoOriginal);

        /*
         * Apenas "JARVIS" = ativação.
         */
        if (comando.equals("jarvis")) {

            resposta.setText(
                    "À sua disposição.\n" +
                    "Sistemas online.\n" +
                    "O que deseja?"
            );

            ouvir();
            return;
        }

        /*
         * Permite também:
         * "JARVIS qual minha bateria?"
         * "JARVIS, qual meu Android?"
         */
        if (comando.startsWith("jarvis ")) {
            comando = comando.substring(7).trim();
        } else if (comando.startsWith("jarvis,")) {
            comando = comando.substring(7).trim();
        }

        if (comando.isEmpty()) {
            resposta.setText(
                    "À sua disposição.\n" +
                    "Sistemas online.\n" +
                    "O que deseja?"
            );
            return;
        }

        processarComando(comando);
    }

    private void processarComando(String comando) {

        StringBuilder resultado = new StringBuilder();

        boolean respondeu = false;

        /*
         * ANÁLISE COMPLETA
         */
        if (ehAnaliseCompleta(comando)) {

            resultado.append(analiseCompleta());
            respondeu = true;

        } else if (ehTudoQueSabe(comando)) {

            resultado.append(tudoQueSabe());
            respondeu = true;

        } else {

            /*
             * BATERIA
             */
            if (ehTemperaturaBateria(comando)) {

                resultado.append(
                        "Temperatura da bateria: "
                                + temperaturaBateria()
                                + "\n"
                );

                respondeu = true;

            } else if (ehPorcentagemBateria(comando)) {

                resultado.append(
                        "Bateria: "
                                + porcentagemBateria()
                                + "%\n"
                );

                respondeu = true;

            } else if (ehCarregando(comando)) {

                resultado.append(
                        estadoCarregamento()
                                + "\n"
                );

                respondeu = true;

            } else if (ehSaudeBateria(comando)) {

                resultado.append(
                        saudeBateria()
                                + "\n"
                );

                respondeu = true;

            } else if (ehEstadoBateria(comando)) {

                resultado.append(
                        estadoCompletoBateria()
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * CELULAR
             */
            if (ehModelo(comando)) {

                resultado.append(
                        "Celular: "
                                + Build.MANUFACTURER
                                + " "
                                + Build.MODEL
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * ANDROID
             */
            if (ehAndroid(comando)) {

                resultado.append(
                        "Android: "
                                + Build.VERSION.RELEASE
                                + "\n"
                                + "API: "
                                + Build.VERSION.SDK_INT
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * ARMAZENAMENTO
             */
            if (ehArmazenamento(comando)) {

                resultado.append(
                        armazenamento()
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * RAM
             */
            if (ehRAM(comando)) {

                resultado.append(
                        memoriaRAM()
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * HORA
             */
            if (ehHora(comando)) {

                resultado.append(
                        "Hora atual: "
                                + horaAtual()
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * DATA
             */
            if (ehData(comando)) {

                resultado.append(
                        "Data de hoje: "
                                + dataAtual()
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * ATUALIZAÇÕES
             */
            if (ehAtualizacao(comando)) {

                resultado.append(
                        "Não tenho permissão para acessar "
                                + "informações mais internas do aparelho.\n"
                );

                respondeu = true;
            }

            /*
             * WI-FI
             */
            if (ehWiFi(comando)) {

                resultado.append(
                        "O estado detalhado da conexão Wi-Fi "
                                + "depende das informações que o Android "
                                + "disponibiliza ao aplicativo.\n"
                );

                respondeu = true;
            }

            /*
             * BLUETOOTH
             */
            if (ehBluetooth(comando)) {

                resultado.append(
                        "O estado detalhado do Bluetooth "
                                + "será adicionado em uma etapa específica.\n"
                );

                respondeu = true;
            }

            /*
             * MODO AVIÃO
             */
            if (ehModoAviao(comando)) {

                resultado.append(
                        "O estado detalhado do modo avião "
                                + "será adicionado em uma etapa específica.\n"
                );

                respondeu = true;
            }

            /*
             * JOGOS
             */
            if (ehComandoJogo(comando)) {

                resultado.append(
                        "Esse jogo não permite que eu acesse "
                                + "essas informações.\n"
                );

                respondeu = true;
            }
        }

        if (!respondeu) {

            resultado.append(
                    "Ainda não tenho um comando para isso."
            );
        }

        resposta.setText(resultado.toString());
    }

    /*
     * ============================
     * NORMALIZAÇÃO
     * ============================
     */

    private String normalizar(String texto) {

        texto = texto.toLowerCase(Locale.ROOT).trim();

        texto = texto
                .replace("á", "a")
                .replace("à", "a")
                .replace("ã", "a")
                .replace("â", "a")
                .replace("ä", "a")
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e")
                .replace("ë", "e")
                .replace("í", "i")
                .replace("ì", "i")
                .replace("î", "i")
                .replace("ï", "i")
                .replace("ó", "o")
                .replace("ò", "o")
                .replace("õ", "o")
                .replace("ô", "o")
                .replace("ö", "o")
                .replace("ú", "u")
                .replace("ù", "u")
                .replace("û", "u")
                .replace("ü", "u")
                .replace("ç", "c");

        return texto;
    }

    /*
     * ============================
     * COMANDOS ESPECIAIS
     * ============================
     */

    private boolean ehAnaliseCompleta(String t) {

        return t.contains("analise completa do meu telefone")
                || t.contains("analise completa do meu celular")
                || t.contains("analise completa do aparelho")
                || t.contains("analise completa do telefone")
                || t.contains("analisa completamente meu telefone")
                || t.contains("analise meu telefone completamente");
    }

    private boolean ehTudoQueSabe(String t) {

        return t.contains("tudo que voce sabe sobre meu telefone")
                || t.contains("tudo que voce sabe sobre meu celular")
                || t.contains("o que voce sabe sobre meu celular")
                || t.contains("o que voce sabe sobre meu telefone")
                || t.contains("todas as informacoes que voce tem sobre meu aparelho")
                || t.contains("tudo sobre o meu telefone")
                || t.contains("tudo sobre meu celular");
    }

    /*
     * ============================
     * BATERIA
     * ============================
     */

    private boolean ehTemperaturaBateria(String t) {

        return t.contains("temperatura da bateria")
                || t.contains("temperatura bateria")
                || t.contains("temperatura da minha bateria")
                || t.contains("quanto esta a temperatura da bateria");
    }

    private boolean ehPorcentagemBateria(String t) {

        return t.contains("porcentagem da bateria")
                || t.contains("porcentagem de bateria")
                || t.contains("quanto de bateria tenho")
                || t.contains("quanto resta de bateria")
                || t.contains("quanto de carga tenho")
                || t.equals("bateria")
                || t.contains("percentual da bateria");
    }

    private boolean ehCarregando(String t) {

        return t.contains("esta carregando")
                || t.contains("esta meu celular carregando")
                || t.contains("meu celular esta carregando")
                || t.contains("esta carregando o celular");
    }

    private boolean ehSaudeBateria(String t) {

        return t.contains("saude da bateria")
                || t.contains("saude bateria")
                || t.contains("bateria esta saudavel")
                || t.contains("bateria saudavel");
    }

    private boolean ehEstadoBateria(String t) {

        return t.contains("estado da bateria")
                || t.contains("como esta a bateria")
                || t.contains("status da bateria")
                || t.contains("situacao da bateria");
    }

    private String porcentagemBateria() {

        if (bateria == null) {
            return "Não consegui acessar a porcentagem da bateria.";
        }

        int nivel = bateria.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CAPACITY
        );

        return String.valueOf(nivel);
    }

    private String temperaturaBateria() {

        Intent intent = registerReceiver(
                null,
                new android.content.IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED
                )
        );

        if (intent == null) {
            return "Não consegui acessar a temperatura da bateria.";
        }

        int temperatura =
                intent.getIntExtra(
                        BatteryManager.EXTRA_TEMPERATURE,
                        -1
                );

        if (temperatura < 0) {
            return "Temperatura da bateria indisponível.";
        }

        double celsius = temperatura / 10.0;

        return String.format(
                Locale.US,
                "%.1f °C",
                celsius
        );
    }

    private String estadoCarregamento() {

        Intent intent = registerReceiver(
                null,
                new android.content.IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED
                )
        );

        if (intent == null) {
            return "Não consegui verificar o carregamento.";
        }

        int status =
                intent.getIntExtra(
                        BatteryManager.EXTRA_STATUS,
                        -1
                );

        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            return "Sim. O celular está carregando.";
        }

        if (status == BatteryManager.BATTERY_STATUS_FULL) {
            return "A bateria está cheia.";
        }

        return "Não. O celular não está carregando.";
    }

    private String saudeBateria() {

        Intent intent = registerReceiver(
                null,
                new android.content.IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED
                )
        );

        if (intent == null) {
            return "Não consegui acessar o estado da bateria.";
        }

        int status =
                intent.getIntExtra(
                        BatteryManager.EXTRA_HEALTH,
                        -1
                );

        switch (status) {

            case BatteryManager.BATTERY_HEALTH_GOOD:
                return "Saúde da bateria: boa.";

            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                return "Saúde da bateria: temperatura elevada.";

            case BatteryManager.BATTERY_HEALTH_DEAD:
                return "Saúde da bateria: estado crítico.";

            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                return "Saúde da bateria: tensão elevada.";

            case BatteryManager.BATTERY_HEALTH_COLD:
                return "Saúde da bateria: temperatura muito baixa.";

            default:
                return "Saúde da bateria: informação indisponível.";
        }
    }

    private String estadoCompletoBateria() {

        return "ESTADO DA BATERIA\n"
                + "Carga: " + porcentagemBateria() + "%\n"
                + "Temperatura: " + temperaturaBateria() + "\n"
                + estadoCarregamento() + "\n"
                + saudeBateria();
    }

    /*
     * ============================
     * CELULAR
     * ============================
     */

    private boolean ehModelo(String t) {

        return t.contains("qual e meu celular")
                || t.contains("qual meu celular")
                || t.contains("qual o modelo do aparelho")
                || t.contains("qual o modelo do celular")
                || t.contains("qual o modelo do telefone")
                || t.contains("modelo do aparelho")
                || t.contains("modelo do celular");
    }

    private boolean ehAndroid(String t) {

        return t.contains("qual android eu tenho")
                || t.contains("qual a versao do meu android")
                || t.contains("que android esta instalado")
                || t.contains("qual versao do android")
                || t.contains("android instalado")
                || t.contains("versao android");
    }

    /*
     * ============================
     * ARMAZENAMENTO
     * ============================
     */

    private boolean ehArmazenamento(String t) {

        return t.contains("quanto espaco tenho")
                || t.contains("quanto armazenamento")
                || t.contains("armazenamento disponivel")
                || t.contains("armazenamento interno")
                || t.contains("espaco livre")
                || t.contains("espaco disponivel")
                || t.contains("quanto de armazenamento")
                || t.contains("memoria de armazenamento");
    }

    private String armazenamento() {

        StatFs statFs = new StatFs(
                Environment.getDataDirectory().getPath()
        );

        long total = statFs.getTotalBytes();
        long livre = statFs.getAvailableBytes();

        return "ARMAZENAMENTO\n"
                + "Total: " + formatarGB(total) + "\n"
                + "Disponível: " + formatarGB(livre);
    }

    /*
     * ============================
     * RAM
     * ============================
     */

    private boolean ehRAM(String t) {

        return t.contains("quanto de ram")
                || t.contains("quanta ram")
                || t.contains("memoria ram")
                || t.contains("quantidade de ram")
                || t.contains("ram disponivel")
                || t.contains("memoria ram disponivel");
    }

    private String memoriaRAM() {

        android.app.ActivityManager manager =
                (android.app.ActivityManager)
                        getSystemService(
                                Context.ACTIVITY_SERVICE
                        );

        if (manager == null) {
            return "Não consegui acessar a memória RAM.";
        }

        android.app.ActivityManager.MemoryInfo info =
                new android.app.ActivityManager.MemoryInfo();

        manager.getMemoryInfo(info);

        long total = info.totalMem;
        long disponivel = info.availMem;

        return "MEMÓRIA RAM\n"
                + "Total: " + formatarGB(total) + "\n"
                + "Disponível: " + formatarGB(disponivel);
    }

    /*
     * ============================
     * DATA E HORA
     * ============================
     */

    private boolean ehHora(String t) {

        return t.contains("que horas sao")
                || t.contains("qual e a hora")
                || t.equals("hora")
                || t.contains("hora atual");
    }

    private boolean ehData(String t) {

        return t.contains("que dia e hoje")
                || t.contains("qual e a data de hoje")
                || t.contains("data de hoje")
                || t.equals("data");
    }

    private String horaAtual() {

        return new SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());
    }

    private String dataAtual() {

        return new SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
        ).format(new Date());
    }

    /*
     * ============================
     * ATUALIZAÇÕES
     * ============================
     */

    private boolean ehAtualizacao(String t) {

        return t.contains("tem atualizacao do android")
                || t.contains("tem atualizacao do software")
                || t.contains("meu celular tem atualizacao")
                || t.contains("existe alguma atualizacao")
                || t.contains("atualizacao do sistema");
    }

    /*
     * ============================
     * CONECTIVIDADE
     * ============================
     */

    private boolean ehWiFi(String t) {

        return t.contains("wifi")
                || t.contains("wi-fi")
                || t.contains("estado do wifi")
                || t.contains("wifi conectado");
    }

    private boolean ehBluetooth(String t) {

        return t.contains("bluetooth")
                || t.contains("estado do bluetooth");
    }

    private boolean ehModoAviao(String t) {

        return t.contains("modo aviao")
                || t.contains("modo avião");
    }

    /*
     * ============================
     * JOGOS
     * ============================
     */

    private boolean ehComandoJogo(String t) {

        return t.contains("fps")
                || t.contains("frames por segundo")
                || t.contains("taxa de quadros")
                || t.contains("ping")
                || t.contains("latencia")
                || t.contains("velocidade de envio")
                || t.contains("upload")
                || t.contains("velocidade de recebimento")
                || t.contains("download")
                || t.contains("analise o jogo")
                || t.contains("analisa meu jogo")
                || t.contains("dados do jogo")
                || t.contains("analise do jogo")
                || t.contains("qual meu fps")
                || t.contains("qual meu ping");
    }

    /*
     * ============================
     * ANÁLISE COMPLETA
     * ============================
     */

    private String analiseCompleta() {

        return "ANÁLISE COMPLETA DO TELEFONE\n\n"
                + "Modelo: "
                + Build.MANUFACTURER
                + " "
                + Build.MODEL
                + "\n"
                + "Android: "
                + Build.VERSION.RELEASE
                + "\n"
                + "API: "
                + Build.VERSION.SDK_INT
                + "\n\n"
                + estadoCompletoBateria()
                + "\n\n"
                + armazenamento()
                + "\n\n"
                + memoriaRAM()
                + "\n\n"
                + "Data: "
                + dataAtual()
                + "\n"
                + "Hora: "
                + horaAtual()
                + "\n\n"
                + "INFORMAÇÕES NÃO ACESSÍVEIS\n"
                + "Não tenho permissão para acessar "
                + "informações mais internas do aparelho.";
    }

    private String tudoQueSabe() {

        return "TUDO QUE CONSIGO ACESSAR SOBRE O TELEFONE\n\n"
                + "Fabricante: "
                + Build.MANUFACTURER
                + "\n"
                + "Modelo: "
                + Build.MODEL
                + "\n"
                + "Android: "
                + Build.VERSION.RELEASE
                + "\n"
                + "API: "
                + Build.VERSION.SDK_INT
                + "\n\n"
                + estadoCompletoBateria()
                + "\n\n"
                + armazenamento()
                + "\n\n"
                + memoriaRAM()
                + "\n\n"
                + "Data: "
                + dataAtual()
                + "\n"
                + "Hora: "
                + horaAtual()
                + "\n\n"
                + "Limitação: não tenho acesso "
                + "a informações internas protegidas "
                + "por outros aplicativos ou pelo sistema.";
    }

    /*
     * ============================
     * UTILITÁRIOS
     * ============================
     */

    private String formatarGB(long bytes) {

        double gb =
                bytes / (1024.0 * 1024.0 * 1024.0);

        return String.format(
                Locale.US,
                "%.2f GB",
                gb
        );
    }
    }package com.jarvis.lite;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int PEDIDO_AUDIO = 100;
    private TextView resposta;
    private BatteryManager bateria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bateria = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView titulo = new TextView(this);
        titulo.setText("JARVIS Lite");
        titulo.setTextSize(28);
        titulo.setGravity(Gravity.CENTER);
        layout.addView(titulo);

        resposta = new TextView(this);
        resposta.setText("Sistemas online.\nPressione o botão e diga \"JARVIS\".");
        resposta.setTextSize(18);
        resposta.setPadding(10, 30, 10, 30);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(resposta);

        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                );

        layout.addView(scroll, scrollParams);

        Button ouvir = new Button(this);
        ouvir.setText("OUVIR JARVIS");
        ouvir.setTextSize(18);

        ouvir.setOnClickListener(v -> ouvir());

        layout.addView(ouvir);

        setContentView(layout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    200
            );
        }
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
                "Diga seu comando..."
        );

        try {
            startActivityForResult(intent, PEDIDO_AUDIO);
        } catch (Exception e) {
            resposta.setText(
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

        if (requestCode != PEDIDO_AUDIO ||
                resultCode != RESULT_OK ||
                data == null) {
            return;
        }

        ArrayList<String> resultados =
                data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                );

        if (resultados == null || resultados.isEmpty()) {
            resposta.setText("Não consegui entender.");
            return;
        }

        String comando = resultados.get(0);
        processarEntrada(comando);
    }

    private void processarEntrada(String comandoOriginal) {

        String comando = normalizar(comandoOriginal);

        /*
         * Apenas "JARVIS" = ativação.
         */
        if (comando.equals("jarvis")) {

            resposta.setText(
                    "À sua disposição.\n" +
                    "Sistemas online.\n" +
                    "O que deseja?"
            );

            ouvir();
            return;
        }

        /*
         * Permite também:
         * "JARVIS qual minha bateria?"
         * "JARVIS, qual meu Android?"
         */
        if (comando.startsWith("jarvis ")) {
            comando = comando.substring(7).trim();
        } else if (comando.startsWith("jarvis,")) {
            comando = comando.substring(7).trim();
        }

        if (comando.isEmpty()) {
            resposta.setText(
                    "À sua disposição.\n" +
                    "Sistemas online.\n" +
                    "O que deseja?"
            );
            return;
        }

        processarComando(comando);
    }

    private void processarComando(String comando) {

        StringBuilder resultado = new StringBuilder();

        boolean respondeu = false;

        /*
         * ANÁLISE COMPLETA
         */
        if (ehAnaliseCompleta(comando)) {

            resultado.append(analiseCompleta());
            respondeu = true;

        } else if (ehTudoQueSabe(comando)) {

            resultado.append(tudoQueSabe());
            respondeu = true;

        } else {

            /*
             * BATERIA
             */
            if (ehTemperaturaBateria(comando)) {

                resultado.append(
                        "Temperatura da bateria: "
                                + temperaturaBateria()
                                + "\n"
                );

                respondeu = true;

            } else if (ehPorcentagemBateria(comando)) {

                resultado.append(
                        "Bateria: "
                                + porcentagemBateria()
                                + "%\n"
                );

                respondeu = true;

            } else if (ehCarregando(comando)) {

                resultado.append(
                        estadoCarregamento()
                                + "\n"
                );

                respondeu = true;

            } else if (ehSaudeBateria(comando)) {

                resultado.append(
                        saudeBateria()
                                + "\n"
                );

                respondeu = true;

            } else if (ehEstadoBateria(comando)) {

                resultado.append(
                        estadoCompletoBateria()
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * CELULAR
             */
            if (ehModelo(comando)) {

                resultado.append(
                        "Celular: "
                                + Build.MANUFACTURER
                                + " "
                                + Build.MODEL
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * ANDROID
             */
            if (ehAndroid(comando)) {

                resultado.append(
                        "Android: "
                                + Build.VERSION.RELEASE
                                + "\n"
                                + "API: "
                                + Build.VERSION.SDK_INT
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * ARMAZENAMENTO
             */
            if (ehArmazenamento(comando)) {

                resultado.append(
                        armazenamento()
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * RAM
             */
            if (ehRAM(comando)) {

                resultado.append(
                        memoriaRAM()
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * HORA
             */
            if (ehHora(comando)) {

                resultado.append(
                        "Hora atual: "
                                + horaAtual()
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * DATA
             */
            if (ehData(comando)) {

                resultado.append(
                        "Data de hoje: "
                                + dataAtual()
                                + "\n"
                );

                respondeu = true;
            }

            /*
             * ATUALIZAÇÕES
             */
            if (ehAtualizacao(comando)) {

                resultado.append(
                        "Não tenho permissão para acessar "
                                + "informações mais internas do aparelho.\n"
                );

                respondeu = true;
            }

            /*
             * WI-FI
             */
            if (ehWiFi(comando)) {

                resultado.append(
                        "O estado detalhado da conexão Wi-Fi "
                                + "depende das informações que o Android "
                                + "disponibiliza ao aplicativo.\n"
                );

                respondeu = true;
            }

            /*
             * BLUETOOTH
             */
            if (ehBluetooth(comando)) {

                resultado.append(
                        "O estado detalhado do Bluetooth "
                                + "será adicionado em uma etapa específica.\n"
                );

                respondeu = true;
            }

            /*
             * MODO AVIÃO
             */
            if (ehModoAviao(comando)) {

                resultado.append(
                        "O estado detalhado do modo avião "
                                + "será adicionado em uma etapa específica.\n"
                );

                respondeu = true;
            }

            /*
             * JOGOS
             */
            if (ehComandoJogo(comando)) {

                resultado.append(
                        "Esse jogo não permite que eu acesse "
                                + "essas informações.\n"
                );

                respondeu = true;
            }
        }

        if (!respondeu) {

            resultado.append(
                    "Ainda não tenho um comando para isso."
            );
        }

        resposta.setText(resultado.toString());
    }

    /*
     * ============================
     * NORMALIZAÇÃO
     * ============================
     */

    private String normalizar(String texto) {

        texto = texto.toLowerCase(Locale.ROOT).trim();

        texto = texto
                .replace("á", "a")
                .replace("à", "a")
                .replace("ã", "a")
                .replace("â", "a")
                .replace("ä", "a")
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e")
                .replace("ë", "e")
                .replace("í", "i")
                .replace("ì", "i")
                .replace("î", "i")
                .replace("ï", "i")
                .replace("ó", "o")
                .replace("ò", "o")
                .replace("õ", "o")
                .replace("ô", "o")
                .replace("ö", "o")
                .replace("ú", "u")
                .replace("ù", "u")
                .replace("û", "u")
                .replace("ü", "u")
                .replace("ç", "c");

        return texto;
    }

    /*
     * ============================
     * COMANDOS ESPECIAIS
     * ============================
     */

    private boolean ehAnaliseCompleta(String t) {

        return t.contains("analise completa do meu telefone")
                || t.contains("analise completa do meu celular")
                || t.contains("analise completa do aparelho")
                || t.contains("analise completa do telefone")
                || t.contains("analisa completamente meu telefone")
                || t.contains("analise meu telefone completamente");
    }

    private boolean ehTudoQueSabe(String t) {

        return t.contains("tudo que voce sabe sobre meu telefone")
                || t.contains("tudo que voce sabe sobre meu celular")
                || t.contains("o que voce sabe sobre meu celular")
                || t.contains("o que voce sabe sobre meu telefone")
                || t.contains("todas as informacoes que voce tem sobre meu aparelho")
                || t.contains("tudo sobre o meu telefone")
                || t.contains("tudo sobre meu celular");
    }

    /*
     * ============================
     * BATERIA
     * ============================
     */

    private boolean ehTemperaturaBateria(String t) {

        return t.contains("temperatura da bateria")
                || t.contains("temperatura bateria")
                || t.contains("temperatura da minha bateria")
                || t.contains("quanto esta a temperatura da bateria");
    }

    private boolean ehPorcentagemBateria(String t) {

        return t.contains("porcentagem da bateria")
                || t.contains("porcentagem de bateria")
                || t.contains("quanto de bateria tenho")
                || t.contains("quanto resta de bateria")
                || t.contains("quanto de carga tenho")
                || t.equals("bateria")
                || t.contains("percentual da bateria");
    }

    private boolean ehCarregando(String t) {

        return t.contains("esta carregando")
                || t.contains("esta meu celular carregando")
                || t.contains("meu celular esta carregando")
                || t.contains("esta carregando o celular");
    }

    private boolean ehSaudeBateria(String t) {

        return t.contains("saude da bateria")
                || t.contains("saude bateria")
                || t.contains("bateria esta saudavel")
                || t.contains("bateria saudavel");
    }

    private boolean ehEstadoBateria(String t) {

        return t.contains("estado da bateria")
                || t.contains("como esta a bateria")
                || t.contains("status da bateria")
                || t.contains("situacao da bateria");
    }

    private String porcentagemBateria() {

        if (bateria == null) {
            return "Não consegui acessar a porcentagem da bateria.";
        }

        int nivel = bateria.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CAPACITY
        );

        return String.valueOf(nivel);
    }

    private String temperaturaBateria() {

        Intent intent = registerReceiver(
                null,
                new android.content.IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED
                )
        );

        if (intent == null) {
            return "Não consegui acessar a temperatura da bateria.";
        }

        int temperatura =
                intent.getIntExtra(
                        BatteryManager.EXTRA_TEMPERATURE,
                        -1
                );

        if (temperatura < 0) {
            return "Temperatura da bateria indisponível.";
        }

        double celsius = temperatura / 10.0;

        return String.format(
                Locale.US,
                "%.1f °C",
                celsius
        );
    }

    private String estadoCarregamento() {

        Intent intent = registerReceiver(
                null,
                new android.content.IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED
                )
        );

        if (intent == null) {
            return "Não consegui verificar o carregamento.";
        }

        int status =
                intent.getIntExtra(
                        BatteryManager.EXTRA_STATUS,
                        -1
                );

        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            return "Sim. O celular está carregando.";
        }

        if (status == BatteryManager.BATTERY_STATUS_FULL) {
            return "A bateria está cheia.";
        }

        return "Não. O celular não está carregando.";
    }

    private String saudeBateria() {

        Intent intent = registerReceiver(
                null,
                new android.content.IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED
                )
        );

        if (intent == null) {
            return "Não consegui acessar o estado da bateria.";
        }

        int status =
                intent.getIntExtra(
                        BatteryManager.EXTRA_HEALTH,
                        -1
                );

        switch (status) {

            case BatteryManager.BATTERY_HEALTH_GOOD:
                return "Saúde da bateria: boa.";

            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                return "Saúde da bateria: temperatura elevada.";

            case BatteryManager.BATTERY_HEALTH_DEAD:
                return "Saúde da bateria: estado crítico.";

            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                return "Saúde da bateria: tensão elevada.";

            case BatteryManager.BATTERY_HEALTH_COLD:
                return "Saúde da bateria: temperatura muito baixa.";

            default:
                return "Saúde da bateria: informação indisponível.";
        }
    }

    private String estadoCompletoBateria() {

        return "ESTADO DA BATERIA\n"
                + "Carga: " + porcentagemBateria() + "%\n"
                + "Temperatura: " + temperaturaBateria() + "\n"
                + estadoCarregamento() + "\n"
                + saudeBateria();
    }

    /*
     * ============================
     * CELULAR
     * ============================
     */

    private boolean ehModelo(String t) {

        return t.contains("qual e meu celular")
                || t.contains("qual meu celular")
                || t.contains("qual o modelo do aparelho")
                || t.contains("qual o modelo do celular")
                || t.contains("qual o modelo do telefone")
                || t.contains("modelo do aparelho")
                || t.contains("modelo do celular");
    }

    private boolean ehAndroid(String t) {

        return t.contains("qual android eu tenho")
                || t.contains("qual a versao do meu android")
                || t.contains("que android esta instalado")
                || t.contains("qual versao do android")
                || t.contains("android instalado")
                || t.contains("versao android");
    }

    /*
     * ============================
     * ARMAZENAMENTO
     * ============================
     */

    private boolean ehArmazenamento(String t) {

        return t.contains("quanto espaco tenho")
                || t.contains("quanto armazenamento")
                || t.contains("armazenamento disponivel")
                || t.contains("armazenamento interno")
                || t.contains("espaco livre")
                || t.contains("espaco disponivel")
                || t.contains("quanto de armazenamento")
                || t.contains("memoria de armazenamento");
    }

    private String armazenamento() {

        StatFs statFs = new StatFs(
                Environment.getDataDirectory().getPath()
        );

        long total = statFs.getTotalBytes();
        long livre = statFs.getAvailableBytes();

        return "ARMAZENAMENTO\n"
                + "Total: " + formatarGB(total) + "\n"
                + "Disponível: " + formatarGB(livre);
    }

    /*
     * ============================
     * RAM
     * ============================
     */

    private boolean ehRAM(String t) {

        return t.contains("quanto de ram")
                || t.contains("quanta ram")
                || t.contains("memoria ram")
                || t.contains("quantidade de ram")
                || t.contains("ram disponivel")
                || t.contains("memoria ram disponivel");
    }

    private String memoriaRAM() {

        android.app.ActivityManager manager =
                (android.app.ActivityManager)
                        getSystemService(
                                Context.ACTIVITY_SERVICE
                        );

        if (manager == null) {
            return "Não consegui acessar a memória RAM.";
        }

        android.app.ActivityManager.MemoryInfo info =
                new android.app.ActivityManager.MemoryInfo();

        manager.getMemoryInfo(info);

        long total = info.totalMem;
        long disponivel = info.availMem;

        return "MEMÓRIA RAM\n"
                + "Total: " + formatarGB(total) + "\n"
                + "Disponível: " + formatarGB(disponivel);
    }

    /*
     * ============================
     * DATA E HORA
     * ============================
     */

    private boolean ehHora(String t) {

        return t.contains("que horas sao")
                || t.contains("qual e a hora")
                || t.equals("hora")
                || t.contains("hora atual");
    }

    private boolean ehData(String t) {

        return t.contains("que dia e hoje")
                || t.contains("qual e a data de hoje")
                || t.contains("data de hoje")
                || t.equals("data");
    }

    private String horaAtual() {

        return new SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());
    }

    private String dataAtual() {

        return new SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
        ).format(new Date());
    }

    /*
     * ============================
     * ATUALIZAÇÕES
     * ============================
     */

    private boolean ehAtualizacao(String t) {

        return t.contains("tem atualizacao do android")
                || t.contains("tem atualizacao do software")
                || t.contains("meu celular tem atualizacao")
                || t.contains("existe alguma atualizacao")
                || t.contains("atualizacao do sistema");
    }

    /*
     * ============================
     * CONECTIVIDADE
     * ============================
     */

    private boolean ehWiFi(String t) {

        return t.contains("wifi")
                || t.contains("wi-fi")
                || t.contains("estado do wifi")
                || t.contains("wifi conectado");
    }

    private boolean ehBluetooth(String t) {

        return t.contains("bluetooth")
                || t.contains("estado do bluetooth");
    }

    private boolean ehModoAviao(String t) {

        return t.contains("modo aviao")
                || t.contains("modo avião");
    }

    /*
     * ============================
     * JOGOS
     * ============================
     */

    private boolean ehComandoJogo(String t) {

        return t.contains("fps")
                || t.contains("frames por segundo")
                || t.contains("taxa de quadros")
                || t.contains("ping")
                || t.contains("latencia")
                || t.contains("velocidade de envio")
                || t.contains("upload")
                || t.contains("velocidade de recebimento")
                || t.contains("download")
                || t.contains("analise o jogo")
                || t.contains("analisa meu jogo")
                || t.contains("dados do jogo")
                || t.contains("analise do jogo")
                || t.contains("qual meu fps")
                || t.contains("qual meu ping");
    }

    /*
     * ============================
     * ANÁLISE COMPLETA
     * ============================
     */

    private String analiseCompleta() {

        return "ANÁLISE COMPLETA DO TELEFONE\n\n"
                + "Modelo: "
                + Build.MANUFACTURER
                + " "
                + Build.MODEL
                + "\n"
                + "Android: "
                + Build.VERSION.RELEASE
                + "\n"
                + "API: "
                + Build.VERSION.SDK_INT
                + "\n\n"
                + estadoCompletoBateria()
                + "\n\n"
                + armazenamento()
                + "\n\n"
                + memoriaRAM()
                + "\n\n"
                + "Data: "
                + dataAtual()
                + "\n"
                + "Hora: "
                + horaAtual()
                + "\n\n"
                + "INFORMAÇÕES NÃO ACESSÍVEIS\n"
                + "Não tenho permissão para acessar "
                + "informações mais internas do aparelho.";
    }

    private String tudoQueSabe() {

        return "TUDO QUE CONSIGO ACESSAR SOBRE O TELEFONE\n\n"
                + "Fabricante: "
                + Build.MANUFACTURER
                + "\n"
                + "Modelo: "
                + Build.MODEL
                + "\n"
                + "Android: "
                + Build.VERSION.RELEASE
                + "\n"
                + "API: "
                + Build.VERSION.SDK_INT
                + "\n\n"
                + estadoCompletoBateria()
                + "\n\n"
                + armazenamento()
                + "\n\n"
                + memoriaRAM()
                + "\n\n"
                + "Data: "
                + dataAtual()
                + "\n"
                + "Hora: "
                + horaAtual()
                + "\n\n"
                + "Limitação: não tenho acesso "
                + "a informações internas protegidas "
                + "por outros aplicativos ou pelo sistema.";
    }

    /*
     * ============================
     * UTILITÁRIOS
     * ============================
     */

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
