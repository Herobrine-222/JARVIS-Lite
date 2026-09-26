package com.jarvis.lite;

import android.app.Activity;
import android.content.ActivityNotFoundException;
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

    private static final int REQUEST_VOZ = 1001;

    private TextView textoStatus;
    private Button botaoOuvir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        textoStatus = findViewById(R.id.textoStatus);
        botaoOuvir = findViewById(R.id.botaoOuvir);

        textoStatus.setText(
                "À sua disposição.\n" +
                "Sistemas online.\n\n" +
                "Toque em OUVIR JARVIS para começar."
        );

        botaoOuvir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iniciarReconhecimento();
            }
        });
    }

    private void iniciarReconhecimento() {

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
                "Diga seu comando para JARVIS"
        );

        try {

            startActivityForResult(intent, REQUEST_VOZ);

        } catch (ActivityNotFoundException e) {

            mostrarErroVoz(
                    "Não encontrei um serviço de reconhecimento de voz neste telefone."
            );

        } catch (Exception e) {

            mostrarErroVoz(
                    "O reconhecimento de voz não pôde ser iniciado."
            );
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_VOZ) {
            return;
        }

        if (resultCode != RESULT_OK || data == null) {

            mostrarErroVoz(
                    "Não consegui ouvir o comando. Toque novamente em OUVIR JARVIS."
            );

            return;
        }

        try {

            List<String> resultados =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );

            if (resultados == null || resultados.isEmpty()) {

                mostrarErroVoz(
                        "Não consegui entender o que foi dito."
                );

                return;
            }

            String textoReconhecido = resultados.get(0);

            processarEntrada(textoReconhecido);

        } catch (Exception e) {

            mostrarErroVoz(
                    "Ocorreu um erro ao processar o comando de voz."
            );
        }
    }

    private void mostrarErroVoz(String mensagem) {

        textoStatus.setText(
                "JARVIS\n\n" +
                mensagem +
                "\n\nToque em OUVIR JARVIS para tentar novamente."
        );
    }

    private void processarEntrada(String entrada) {

        if (entrada == null || entrada.trim().isEmpty()) {

            mostrarErroVoz("Não recebi nenhum comando.");

            return;
        }

        String normalizado = normalizar(entrada);

        if (normalizado.equals("jarvis")) {

            textoStatus.setText(
                    "À sua disposição.\n" +
                    "Sistemas online.\n" +
                    "O que deseja?"
            );

            return;
        }

        if (!normalizado.startsWith("jarvis ")) {

            textoStatus.setText(
                    "Não detectei o comando JARVIS.\n\n" +
                    "Diga, por exemplo:\n" +
                    "\"JARVIS, analise meu telefone\""
            );

            return;
        }

        String comando = normalizado.substring(7).trim();

        processarComando(comando);
    }

    private String normalizar(String texto) {

        String resultado = texto
                .toLowerCase(Locale.getDefault())
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

        resultado = resultado.replaceAll("[\\p{Punct}]", " ");

        resultado = resultado.replaceAll("\\s+", " ");

        return resultado.trim();
    }

    private void processarComando(String comando) {

        StringBuilder resposta = new StringBuilder();

        boolean encontrou = false;

        if (ehAnaliseCompleta(comando)) {

            resposta.append(obterAnaliseCompleta());
            encontrou = true;

        } else {

            if (ehInformacoesBateria(comando)) {

                resposta.append(obterEstadoBateria())
                        .append("\n\n")
                        .append(obterTemperaturaBateria())
                        .append("\n\n")
                        .append(obterSaudeBateria());

                encontrou = true;

            } else if (ehPorcentagemBateria(comando)) {

                resposta.append(obterPorcentagemBateria());

                encontrou = true;
            }

            if (ehModelo(comando)) {

                if (encontrou) {
                    resposta.append("\n\n");
                }

                resposta.append(
                        "Modelo do telefone: "
                ).append(Build.MODEL);

                encontrou = true;
            }

            if (ehAndroid(comando)) {

                if (encontrou) {
                    resposta.append("\n\n");
                }

                resposta.append(obterAndroid());

                encontrou = true;
            }

            if (ehRam(comando)) {

                if (encontrou) {
                    resposta.append("\n\n");
                }

                resposta.append(obterRam());

                encontrou = true;
            }

            if (ehArmazenamento(comando)) {

                if (encontrou) {
                    resposta.append("\n\n");
                }

                resposta.append(obterArmazenamento());

                encontrou = true;
            }

            if (ehTemperatura(comando)) {

                if (encontrou) {
                    resposta.append("\n\n");
                }

                resposta.append(obterTemperaturaBateria());

                encontrou = true;
            }

            if (ehDataHora(comando)) {

                if (encontrou) {
                    resposta.append("\n\n");
                }

                resposta.append(obterDataHora());

                encontrou = true;
            }

            if (ehWiFi(comando)) {

                if (encontrou) {
                    resposta.append("\n\n");
                }

                resposta.append(obterWiFi());

                encontrou = true;
            }

            if (ehComandoJogo(comando)) {

                if (encontrou) {
                    resposta.append("\n\n");
                }

                resposta.append(
                        "Esse jogo não permite que eu acesse essas informações."
                );

                encontrou = true;
            }
        }

        if (!encontrou) {

            resposta.append(
                    "Comando não reconhecido.\n\n" +
                    "Você pode dizer:\n" +
                    "• JARVIS, analise meu telefone\n" +
                    "• JARVIS, informações da bateria\n" +
                    "• JARVIS, porcentagem da bateria\n" +
                    "• JARVIS, modelo do telefone\n" +
                    "• JARVIS, memória RAM\n" +
                    "• JARVIS, armazenamento\n" +
                    "• JARVIS, temperatura da bateria\n" +
                    "• JARVIS, data e hora\n" +
                    "• JARVIS, Wi-Fi"
            );
        }

        textoStatus.setText(resposta.toString());
    }

    private boolean ehAnaliseCompleta(String t) {

        return t.contains("analise meu telefone")
                || t.contains("analise meu celular")
                || t.contains("analisar meu telefone")
                || t.contains("analisar meu celular")
                || t.contains("analise completa")
                || t.contains("analise completa do telefone")
                || t.contains("analise completa do celular");
    }

    private boolean ehInformacoesBateria(String t) {

        return t.contains("informacoes da bateria")
                || t.contains("informacao da bateria")
                || t.contains("dados da bateria")
                || t.contains("detalhes da bateria")
                || t.contains("como esta a bateria");
    }

    private boolean ehPorcentagemBateria(String t) {

        return t.contains("porcentagem da bateria")
                || t.contains("porcentagem bateria")
                || t.contains("percentual da bateria")
                || t.contains("percentual bateria")
                || t.equals("bateria");
    }

    private boolean ehModelo(String t) {

        return t.contains("modelo do telefone")
                || t.contains("modelo telefone")
                || t.contains("modelo do celular")
                || t.contains("modelo celular");
    }

    private boolean ehAndroid(String t) {

        return t.contains("versao do android")
                || t.contains("versao android")
                || t.contains("android");
    }

    private boolean ehRam(String t) {

        return t.equals("ram")
                || t.contains("memoria ram")
                || t.contains("memoria de ram");
    }

    private boolean ehArmazenamento(String t) {

        return t.contains("armazenamento")
                || t.contains("espaco de armazenamento")
                || t.contains("memoria interna")
                || t.contains("espaco livre");
    }

    private boolean ehTemperatura(String t) {

        return t.contains("temperatura da bateria")
                || t.contains("temperatura bateria");
    }

    private boolean ehDataHora(String t) {

        return t.contains("data e hora")
                || t.contains("data hora")
                || t.equals("data")
                || t.equals("hora")
                || t.contains("que horas");
    }

    private boolean ehWiFi(String t) {

        return t.contains("wifi")
                || t.contains("wi fi");
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
                (BatteryManager) getSystemService(BATTERY_SERVICE);

        int porcentagem = bm.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CAPACITY
        );

        return "Bateria: " + porcentagem + "%";
    }

    private String obterEstadoBateria() {

        Intent intent = registerReceiver(
                null,
                new android.content.IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED
                )
        );

        if (intent == null) {
            return "Bateria: não foi possível consultar o estado.";
        }

        int status = intent.getIntExtra(
                BatteryManager.EXTRA_STATUS,
                -1
        );

        String estado;

        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {

            estado = "carregando";

        } else if (status == BatteryManager.BATTERY_STATUS_FULL) {

            estado = "com carga completa";

        } else {

            estado = "não está carregando";
        }

        return "Bateria: "
                + obterPorcentagemBateria().replace("Bateria: ", "")
                + "\nEstado: "
                + estado;
    }

    private String obterTemperaturaBateria() {

        Intent intent = registerReceiver(
                null,
                new android.content.IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED
                )
        );

        if (intent == null) {
            return "Temperatura da bateria: não disponível.";
        }

        int temperatura = intent.getIntExtra(
                BatteryManager.EXTRA_TEMPERATURE,
                -1
        );

        if (temperatura == -1) {
            return "Temperatura da bateria: não disponível.";
        }

        double temperaturaCelsius = temperatura / 10.0;

        return "Temperatura da bateria: "
                + temperaturaCelsius
                + " °C";
    }

    private String obterSaudeBateria() {

        Intent intent = registerReceiver(
                null,
                new android.content.IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED
                )
        );

        if (intent == null) {
            return "Saúde da bateria: informação não disponível.";
        }

        int health = intent.getIntExtra(
                BatteryManager.EXTRA_HEALTH,
                -1
        );

        String resultado;

        switch (health) {

            case BatteryManager.BATTERY_HEALTH_GOOD:
                resultado = "Boa";
                break;

            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                resultado = "Superaquecida";
                break;

            case BatteryManager.BATTERY_HEALTH_DEAD:
                resultado = "Sem funcionamento";
                break;

            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                resultado = "Sobretensão";
                break;

            case BatteryManager.BATTERY_HEALTH_COLD:
                resultado = "Muito fria";
                break;

            default:
                resultado = "Não disponível";
                break;
        }

        return "Saúde da bateria: " + resultado;
    }

    private String obterAndroid() {

        return "Android: " + Build.VERSION.RELEASE
                + "\nAPI: " + Build.VERSION.SDK_INT;
    }

    private String obterRam() {

        android.app.ActivityManager am =
                (android.app.ActivityManager)
                        getSystemService(ACTIVITY_SERVICE);

        if (am == null) {
            return "RAM: não foi possível consultar.";
        }

        android.app.ActivityManager.MemoryInfo info =
                new android.app.ActivityManager.MemoryInfo();

        am.getMemoryInfo(info);

        double totalGB =
                info.totalMem / (1024.0 * 1024.0 * 1024.0);

        double disponivelGB =
                info.availMem / (1024.0 * 1024.0 * 1024.0);

        double usadaGB = totalGB - disponivelGB;

        return String.format(
                Locale.getDefault(),
                "RAM total: %.2f GB\nRAM usada: %.2f GB\nRAM disponível: %.2f GB",
                totalGB,
                usadaGB,
                disponivelGB
        );
    }

    private String obterArmazenamento() {

        StatFs statFs =
                new StatFs(
                        Environment.getDataDirectory().getPath()
                );

        long totalBytes =
                statFs.getTotalBytes();

        long livreBytes =
                statFs.getAvailableBytes();

        long usadoBytes =
                totalBytes - livreBytes;

        return String.format(
                Locale.getDefault(),
                "Armazenamento total: %s\n" +
                "Armazenamento usado: %s\n" +
                "Armazenamento livre: %s",
                formatarGB(totalBytes),
                formatarGB(usadoBytes),
                formatarGB(livreBytes)
        );
    }

    private String formatarGB(long bytes) {

        double gb =
                bytes / (1024.0 * 1024.0 * 1024.0);

        return String.format(
                Locale.getDefault(),
                "%.2f GB",
                gb
        );
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
                        getSystemService(CONNECTIVITY_SERVICE);

        if (cm == null) {

            return "Wi-Fi: não foi possível consultar o estado.";
        }

        Network network =
                cm.getActiveNetwork();

        if (network == null) {

            return "Wi-Fi: não conectado.";
        }

        NetworkCapabilities capabilities =
                cm.getNetworkCapabilities(network);

        if (capabilities == null) {

            return "Wi-Fi: não foi possível determinar o estado.";
        }

        if (capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_WIFI)) {

            return "Wi-Fi: conectado.";
        }

        return "Wi-Fi: não conectado.";
    }

    private String obterAnaliseCompleta() {

        return "ANÁLISE DO TELEFONE\n\n"
                + obterEstadoBateria()
                + "\n\n"
                + obterTemperaturaBateria()
                + "\n\n"
                + obterSaudeBateria()
                + "\n\n"
                + obterRam()
                + "\n\n"
                + obterArmazenamento();
    }
}
