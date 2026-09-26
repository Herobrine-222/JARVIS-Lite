package com.jarvis.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

private static final int PEDIDO_VOZ = 1001;

private TextView textoRelogio;
private TextView textoData;
private TextView textoStatus;
private TextView textoClima;
private TextView textoPrevisao;

private View anelExterno;
private View anelInterno;
private View botaoReator;

private LinearLayout menuConfiguracoes;

private TextToSpeech sintetizador;

private final Handler handler =
        new Handler(Looper.getMainLooper());

private final Runnable atualizador = new Runnable() {
    @Override
    public void run() {
        atualizarDataHora();
        handler.postDelayed(this, 1000);
    }
};

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);

    textoRelogio = findViewById(R.id.textoRelogio);
    textoData = findViewById(R.id.textoData);
    textoStatus = findViewById(R.id.textoStatus);
    textoClima = findViewById(R.id.textoClima);
    textoPrevisao = findViewById(R.id.textoPrevisao);

    anelExterno = findViewById(R.id.anelExterno);
    anelInterno = findViewById(R.id.anelInterno);
    botaoReator = findViewById(R.id.botaoReator);

    menuConfiguracoes =
            findViewById(R.id.menuConfiguracoes);

    Button botaoConfiguracoes =
            findViewById(R.id.botaoConfiguracoes);

    Button botaoVerificacao =
            findViewById(R.id.botaoVerificacao);

    Button opcaoGerenciar =
            findViewById(R.id.opcaoGerenciar);

    Button opcaoPrivacidade =
            findViewById(R.id.opcaoPrivacidade);

    Button opcaoVerificacao =
            findViewById(R.id.opcaoVerificacao);

    inicializarVoz();

    atualizarDataHora();

    iniciarAnimacaoReator();

    botaoConfiguracoes.setOnClickListener(v ->
            alternarMenu()
    );

    botaoReator.setOnClickListener(v ->
            iniciarReconhecimentoVoz()
    );

    botaoVerificacao.setOnClickListener(v ->
            iniciarVerificacao()
    );

    opcaoGerenciar.setOnClickListener(v -> {
        menuConfiguracoes.setVisibility(View.GONE);
        mostrarGerenciamento();
    });

    opcaoPrivacidade.setOnClickListener(v -> {
        menuConfiguracoes.setVisibility(View.GONE);
        mostrarPrivacidade();
    });

    opcaoVerificacao.setOnClickListener(v -> {
        menuConfiguracoes.setVisibility(View.GONE);
        iniciarVerificacao();
    });

    textoClima.setText("☁  Sua região");
    textoPrevisao.setText(
            "Previsão do tempo será conectada aqui"
    );
}

@Override
protected void onResume() {
    super.onResume();

    handler.removeCallbacks(atualizador);
    handler.post(atualizador);
}

@Override
protected void onPause() {
    super.onPause();

    handler.removeCallbacks(atualizador);
}

@Override
protected void onDestroy() {
    handler.removeCallbacksAndMessages(null);

    if (sintetizador != null) {
        sintetizador.stop();
        sintetizador.shutdown();
    }

    super.onDestroy();
}

private void atualizarDataHora() {

    Date agora = new Date();

    textoRelogio.setText(
            new SimpleDateFormat(
                    "HH:mm:ss",
                    Locale.getDefault()
            ).format(agora)
    );

    textoData.setText(
            new SimpleDateFormat(
                    "dd MMM yyyy",
                    Locale.getDefault()
            ).format(agora)
    );
}

private void alternarMenu() {

    if (menuConfiguracoes.getVisibility()
            == View.VISIBLE) {

        menuConfiguracoes.setVisibility(
                View.GONE
        );

    } else {

        menuConfiguracoes.setVisibility(
                View.VISIBLE
        );
    }
}

private void iniciarAnimacaoReator() {

    RotateAnimation rotacaoExterna =
            new RotateAnimation(
                    0,
                    360,
                    Animation.RELATIVE_TO_SELF,
                    0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f
            );

    rotacaoExterna.setDuration(9000);
    rotacaoExterna.setRepeatCount(
            Animation.INFINITE
    );
    rotacaoExterna.setInterpolator(
            new LinearInterpolator()
    );

    anelExterno.startAnimation(
            rotacaoExterna
    );

    RotateAnimation rotacaoInterna =
            new RotateAnimation(
                    360,
                    0,
                    Animation.RELATIVE_TO_SELF,
                    0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f
            );

    rotacaoInterna.setDuration(6000);
    rotacaoInterna.setRepeatCount(
            Animation.INFINITE
    );
    rotacaoInterna.setInterpolator(
            new LinearInterpolator()
    );

    anelInterno.startAnimation(
            rotacaoInterna
    );
}

private void inicializarVoz() {

    sintetizador =
            new TextToSpeech(
                    this,
                    status -> {

                        if (status ==
                                TextToSpeech.SUCCESS) {

                            sintetizador.setLanguage(
                                    new Locale("pt", "BR")
                            );
                        }
                    }
            );
}

private void falar(String texto) {

    if (sintetizador == null) {
        return;
    }

    sintetizador.speak(
            texto,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "jarvis_resposta"
    );
}

private void iniciarReconhecimentoVoz() {

    Intent intent =
            new Intent(
                    RecognizerIntent
                            .ACTION_RECOGNIZE_SPEECH
            );

    intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent
                    .LANGUAGE_MODEL_FREE_FORM
    );

    intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            "pt-BR"
    );

    intent.putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            "Diga JARVIS e seu comando"
    );

    try {

        startActivityForResult(
                intent,
                PEDIDO_VOZ
        );

    } catch (ActivityNotFoundException e) {

        mostrarStatus(
                "O reconhecimento de voz não está disponível."
        );

    } catch (Exception e) {

        mostrarStatus(
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

    super.onActivityResult(
            requestCode,
            resultCode,
            data
    );

    if (requestCode != PEDIDO_VOZ) {
        return;
    }

    if (resultCode != RESULT_OK ||
            data == null) {

        mostrarStatus(
                "Não consegui ouvir o comando."
        );

        return;
    }

    try {

        List<String> resultados =
                data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                );

        if (resultados == null ||
                resultados.isEmpty()) {

            mostrarStatus(
                    "Não consegui entender."
            );

            return;
        }

        processarEntrada(
                resultados.get(0)
        );

    } catch (Exception e) {

        mostrarStatus(
                "Ocorreu um erro ao processar o comando."
        );
    }
}

private void processarEntrada(
        String entrada
) {

    String comando =
            normalizar(entrada);

    if (comando.equals("jarvis")) {

        responder(
                "À sua disposição. Sistemas online. O que deseja?"
        );

        return;
    }

    if (comando.startsWith("jarvis ")) {

        comando =
                comando.substring(7).trim();

        processarComando(comando);

        return;
    }

    responder(
            "Diga primeiro JARVIS."
    );
}

private void processarComando(
        String comando
) {

    if (comando.contains("bateria")) {

        responder(
                "Bateria em "
                        + obterBateria()
                        + " por cento."
        );

        return;
    }

    if (comando.contains("ram") ||
            comando.contains("memoria ram")) {

        responder(
                obterRam()
        );

        return;
    }

    if (comando.contains("armazenamento") ||
            comando.contains("espaco livre")) {

        responder(
                obterArmazenamento()
        );

        return;
    }

    if (comando.contains("temperatura")) {

        responder(
                obterTemperatura()
        );

        return;
    }

    if (comando.contains("analise") ||
            comando.contains("estado do telefone") ||
            comando.contains("status do telefone")) {

        responder(
                "Análise básica concluída. "
                        + "Bateria em "
                        + obterBateria()
                        + " por cento. "
                        + obterRam()
                        + ". "
                        + obterArmazenamento()
        );

        return;
    }

    responder(
            "Ainda estou aprendendo esse comando."
    );
}

private void responder(String texto) {

    mostrarStatus(texto);

    falar(texto);
}

private void mostrarStatus(String texto) {

    textoStatus.setText(texto);
}

private String normalizar(
        String texto
) {

    if (texto == null) {
        return "";
    }

    return texto.toLowerCase(
                    Locale.ROOT
            )
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
            .replace("ç", "c")
            .replaceAll(
                    "[\\p{Punct}]",
                    " "
            )
            .replaceAll(
                    "\\s+",
                    " "
            )
            .trim();
}

private int obterBateria() {

    BatteryManager bateria =
            (BatteryManager)
                    getSystemService(
                            BATTERY_SERVICE
                    );

    if (bateria == null) {
        return 0;
    }

    return bateria.getIntProperty(
            BatteryManager
                    .BATTERY_PROPERTY_CAPACITY
    );
}

private String obterTemperatura() {

    Intent bateria =
            registerReceiver(
                    null,
                    new android.content.IntentFilter(
                            Intent.ACTION_BATTERY_CHANGED
                    )
            );

    if (bateria == null) {
        return "Temperatura indisponível.";
    }

    int valor =
            bateria.getIntExtra(
                    BatteryManager.EXTRA_TEMPERATURE,
                    Integer.MIN_VALUE
            );

    if (valor == Integer.MIN_VALUE) {
        return "Temperatura indisponível.";
    }

    return String.format(
            Locale.getDefault(),
            "Temperatura da bateria: %.1f graus Celsius",
            valor / 10.0f
    );
}

private String obterRam() {

    ActivityManager manager =
            (ActivityManager)
                    getSystemService(
                            Context.ACTIVITY_SERVICE
                    );

    if (manager == null) {
        return "RAM indisponível";
    }

    ActivityManager.MemoryInfo info =
            new ActivityManager.MemoryInfo();

    manager.getMemoryInfo(info);

    long total =
            info.totalMem /
                    (1024 * 1024);

    long livre =
            info.availMem /
                    (1024 * 1024);

    long usada =
            total - livre;

    return "RAM: "
            + usada
            + " MB usada de "
            + total
            + " MB";
}

private String obterArmazenamento() {

    android.os.StatFs stat =
            new android.os.StatFs(
                    Environment
                            .getDataDirectory()
                            .getPath()
            );

    long total =
            stat.getTotalBytes();

    long livre =
            stat.getAvailableBytes();

    return String.format(
            Locale.getDefault(),
            "Armazenamento: %.1f GB livres de %.1f GB",
            livre / 1073741824.0,
            total / 1073741824.0
    );
}

private void mostrarGerenciamento() {

    new AlertDialog.Builder(this)
            .setTitle("Gerenciar JARVIS")
            .setMessage(
                    "Recursos do JARVIS\n\n"
                            + "Microfone: usado somente "
                            + "quando você inicia o reconhecimento "
                            + "de voz.\n\n"
                            + "Internet: não utilizada pelo "
                            + "JARVIS nesta versão.\n\n"
                            + "O JARVIS não possui acesso root "
                            + "nem pode alterar o aparelho "
                            + "automaticamente."
            )
            .setPositiveButton(
                    "OK",
                    null
            )
            .show();
}

private void mostrarPrivacidade() {

    new AlertDialog.Builder(this)
            .setTitle("Privacidade")
            .setMessage(
                    "CONTROLE DE PRIVACIDADE\n\n"
                            + "🎙️ Microfone\n"
                            + "Usado somente quando você "
                            + "inicia o reconhecimento de voz.\n\n"
                            + "📱 Informações do aparelho\n"
                            + "Usadas para mostrar dados "
                            + "básicos do dispositivo.\n\n"
                            + "🌐 Internet\n"
                            + "Não utilizada nesta versão "
                            + "do JARVIS."
            )
            .setPositiveButton(
                    "OK",
                    null
            )
            .show();
}

private void iniciarVerificacao() {

    mostrarStatus(
            "INICIANDO VERIFICAÇÃO..."
    );

    falar(
            "Iniciando verificação básica do aparelho."
    );

    new Thread(() -> {

        boolean encontrouIndicador =
                verificarAplicativos();

        runOnUiThread(() -> {

            if (encontrouIndicador) {

                String mensagem =
                        "Foi encontrado um item que "
                                + "merece atenção. "
                                + "Verifique se foi você "
                                + "que instalou.";

                mostrarStatus(mensagem);

                falar(mensagem);

            } else {

                String mensagem =
                        "Seu telefone parece estar "
                                + "seguro, tenha um bom dia.";

                mostrarStatus(mensagem);

                falar(mensagem);
            }
        });

    }).start();
}

private boolean verificarAplicativos() {

    PackageManager pm =
            getPackageManager();

    List<ApplicationInfo> aplicativos =
            pm.getInstalledApplications(
                    PackageManager
                            .GET_META_DATA
            );

    /*
     * Esta é uma verificação básica.
     * Ela NÃO afirma que um aplicativo é malware.
     *
     * O objetivo é verificar se os aplicativos
     * instalados possuem informações básicas
     * válidas no sistema.
     */

    for (ApplicationInfo app :
            aplicativos) {

        if (app == null ||
                app.packageName == null) {

            return true;
        }

        if (app.sourceDir == null) {

            return true;
        }
    }

    return false;
}

}
