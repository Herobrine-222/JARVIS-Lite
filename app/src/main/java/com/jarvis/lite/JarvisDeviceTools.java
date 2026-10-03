package com.jarvis.lite;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import java.util.Locale;

/**
 * Ferramentas locais do aparelho para o JARVIS.
 *
 * Este módulo não utiliza Internet.
 *
 * Funções:
 * - Bateria
 * - Temperatura da bateria
 * - Estado de carregamento
 * - RAM
 * - Armazenamento
 * - Modelo do aparelho
 * - Fabricante
 * - Versão do Android
 * - Informações gerais do sistema
 */
public class JarvisDeviceTools {

    private final Context context;

    public JarvisDeviceTools(Context context) {
        this.context = context.getApplicationContext();
    }

    // ============================================================
    // BATERIA
    // ============================================================

    /**
     * Retorna a porcentagem atual da bateria.
     *
     * @return valor entre 0 e 100.
     *         Retorna -1 caso não seja possível obter o valor.
     */
    public int obterNivelBateria() {

        try {

            IntentFilter filtro =
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

            Intent bateria =
                    context.registerReceiver(null, filtro);

            if (bateria == null) {
                return -1;
            }

            int nivel =
                    bateria.getIntExtra(
                            BatteryManager.EXTRA_LEVEL,
                            -1
                    );

            int escala =
                    bateria.getIntExtra(
                            BatteryManager.EXTRA_SCALE,
                            -1
                    );

            if (nivel < 0 || escala <= 0) {
                return -1;
            }

            return Math.round(
                    (nivel * 100f) / escala
            );

        } catch (Exception e) {

            return -1;
        }
    }

    /**
     * Retorna a porcentagem da bateria formatada.
     */
    public String obterBateriaTexto() {

        int bateria =
                obterNivelBateria();

        if (bateria < 0) {
            return "Não foi possível obter o nível da bateria.";
        }

        return bateria + "%";
    }

    /**
     * Verifica se o aparelho está conectado a uma fonte
     * de carregamento.
     */
    public boolean estaCarregando() {

        try {

            IntentFilter filtro =
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

            Intent bateria =
                    context.registerReceiver(null, filtro);

            if (bateria == null) {
                return false;
            }

            int status =
                    bateria.getIntExtra(
                            BatteryManager.EXTRA_STATUS,
                            -1
                    );

            return status ==
                    BatteryManager.BATTERY_STATUS_CHARGING
                    || status ==
                    BatteryManager.BATTERY_STATUS_FULL;

        } catch (Exception e) {

            return false;
        }
    }

    /**
     * Retorna uma descrição do estado da bateria.
     */
    public String obterEstadoBateria() {

        try {

            IntentFilter filtro =
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

            Intent bateria =
                    context.registerReceiver(null, filtro);

            if (bateria == null) {
                return "Estado da bateria indisponível.";
            }

            int status =
                    bateria.getIntExtra(
                            BatteryManager.EXTRA_STATUS,
                            -1
                    );

            switch (status) {

                case BatteryManager.BATTERY_STATUS_CHARGING:
                    return "Carregando";

                case BatteryManager.BATTERY_STATUS_FULL:
                    return "Carga completa";

                case BatteryManager.BATTERY_STATUS_DISCHARGING:
                    return "Descarregando";

                case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                    return "Não está carregando";

                default:
                    return "Estado desconhecido";
            }

        } catch (Exception e) {

            return "Estado da bateria indisponível.";
        }
    }

    /**
     * Retorna a temperatura da bateria.
     *
     * Android normalmente fornece o valor em décimos de grau Celsius.
     *
     * Exemplo:
     * 365 -> 36,5 °C
     */
    public float obterTemperaturaBateria() {

        try {

            IntentFilter filtro =
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

            Intent bateria =
                    context.registerReceiver(null, filtro);

            if (bateria == null) {
                return -1f;
            }

            int temperatura =
                    bateria.getIntExtra(
                            BatteryManager.EXTRA_TEMPERATURE,
                            -1
                    );

            if (temperatura < 0) {
                return -1f;
            }

            return temperatura / 10.0f;

        } catch (Exception e) {

            return -1f;
        }
    }

    /**
     * Retorna a temperatura formatada.
     */
    public String obterTemperaturaBateriaTexto() {

        float temperatura =
                obterTemperaturaBateria();

        if (temperatura < 0) {
            return "Temperatura indisponível.";
        }

        return String.format(
                Locale.getDefault(),
                "%.1f °C",
                temperatura
        );
    }

    // ============================================================
    // RAM
    // ============================================================

    /**
     * Retorna a RAM total em bytes.
     */
    public long obterRamTotalBytes() {

        try {

            ActivityManager manager =
                    (ActivityManager)
                            context.getSystemService(
                                    Context.ACTIVITY_SERVICE
                            );

            if (manager == null) {
                return -1L;
            }

            ActivityManager.MemoryInfo info =
                    new ActivityManager.MemoryInfo();

            manager.getMemoryInfo(info);

            return info.totalMem;

        } catch (Exception e) {

            return -1L;
        }
    }

    /**
     * Retorna a RAM disponível em bytes.
     */
    public long obterRamDisponivelBytes() {

        try {

            ActivityManager manager =
                    (ActivityManager)
                            context.getSystemService(
                                    Context.ACTIVITY_SERVICE
                            );

            if (manager == null) {
                return -1L;
            }

            ActivityManager.MemoryInfo info =
                    new ActivityManager.MemoryInfo();

            manager.getMemoryInfo(info);

            return info.availMem;

        } catch (Exception e) {

            return -1L;
        }
    }

    /**
     * Retorna a RAM utilizada em bytes.
     */
    public long obterRamUsadaBytes() {

        long total =
                obterRamTotalBytes();

        long disponivel =
                obterRamDisponivelBytes();

        if (total < 0 || disponivel < 0) {
            return -1L;
        }

        return Math.max(
                0L,
                total - disponivel
        );
    }

    /**
     * Converte bytes para GB.
     */
    private double bytesParaGb(long bytes) {

        if (bytes < 0) {
            return -1;
        }

        return bytes /
                (1024.0 * 1024.0 * 1024.0);
    }

    /**
     * Retorna a RAM total formatada.
     */
    public String obterRamTotalTexto() {

        double gb =
                bytesParaGb(
                        obterRamTotalBytes()
                );

        if (gb < 0) {
            return "RAM indisponível.";
        }

        return String.format(
                Locale.getDefault(),
                "%.2f GB",
                gb
        );
    }

    /**
     * Retorna a RAM disponível formatada.
     */
    public String obterRamDisponivelTexto() {

        double gb =
                bytesParaGb(
                        obterRamDisponivelBytes()
                );

        if (gb < 0) {
            return "RAM disponível indisponível.";
        }

        return String.format(
                Locale.getDefault(),
                "%.2f GB",
                gb
        );
    }

    /**
     * Retorna a RAM utilizada formatada.
     */
    public String obterRamUsadaTexto() {

        double gb =
                bytesParaGb(
                        obterRamUsadaBytes()
                );

        if (gb < 0) {
            return "RAM utilizada indisponível.";
        }

        return String.format(
                Locale.getDefault(),
                "%.2f GB",
                gb
        );
    }

    // ============================================================
    // ARMAZENAMENTO
    // ============================================================

    /**
     * Retorna o espaço total do armazenamento interno
     * acessível pelo sistema de arquivos.
     */
    public long obterArmazenamentoTotalBytes() {

        try {

            StatFs stat =
                    new StatFs(
                            Environment
                                    .getDataDirectory()
                                    .getPath()
                    );

            return stat.getTotalBytes();

        } catch (Exception e) {

            return -1L;
        }
    }

    /**
     * Retorna o espaço disponível.
     */
    public long obterArmazenamentoDisponivelBytes() {

        try {

            StatFs stat =
                    new StatFs(
                            Environment
                                    .getDataDirectory()
                                    .getPath()
                    );

            return stat.getAvailableBytes();

        } catch (Exception e) {

            return -1L;
        }
    }

    /**
     * Retorna o armazenamento utilizado.
     */
    public long obterArmazenamentoUsadoBytes() {

        long total =
                obterArmazenamentoTotalBytes();

        long disponivel =
                obterArmazenamentoDisponivelBytes();

        if (total < 0 || disponivel < 0) {
            return -1L;
        }

        return Math.max(
                0L,
                total - disponivel
        );
    }

    /**
     * Retorna o armazenamento total formatado.
     */
    public String obterArmazenamentoTotalTexto() {

        double gb =
                bytesParaGb(
                        obterArmazenamentoTotalBytes()
                );

        if (gb < 0) {
            return "Armazenamento indisponível.";
        }

        return String.format(
                Locale.getDefault(),
                "%.2f GB",
                gb
        );
    }

    /**
     * Retorna o armazenamento disponível formatado.
     */
    public String obterArmazenamentoDisponivelTexto() {

        double gb =
                bytesParaGb(
                        obterArmazenamentoDisponivelBytes()
                );

        if (gb < 0) {
            return "Armazenamento disponível indisponível.";
        }

        return String.format(
                Locale.getDefault(),
                "%.2f GB",
                gb
        );
    }

    /**
     * Retorna o armazenamento utilizado formatado.
     */
    public String obterArmazenamentoUsadoTexto() {

        double gb =
                bytesParaGb(
                        obterArmazenamentoUsadoBytes()
                );

        if (gb < 0) {
            return "Armazenamento utilizado indisponível.";
        }

        return String.format(
                Locale.getDefault(),
                "%.2f GB",
                gb
        );
    }

    // ============================================================
    // APARELHO
    // ============================================================

    /**
     * Retorna o fabricante.
     */
    public String obterFabricante() {

        try {

            String fabricante =
                    Build.MANUFACTURER;

            if (fabricante == null
                    || fabricante.trim().isEmpty()) {

                return "Fabricante desconhecido";
            }

            return fabricante;

        } catch (Exception e) {

            return "Fabricante desconhecido";
        }
    }

    /**
     * Retorna o modelo.
     */
    public String obterModelo() {

        try {

            String modelo =
                    Build.MODEL;

            if (modelo == null
                    || modelo.trim().isEmpty()) {

                return "Modelo desconhecido";
            }

            return modelo;

        } catch (Exception e) {

            return "Modelo desconhecido";
        }
    }

    /**
     * Retorna o fabricante + modelo.
     */
    public String obterNomeAparelho() {

        return obterFabricante()
                + " "
                + obterModelo();
    }

    /**
     * Retorna a versão do Android.
     */
    public String obterVersaoAndroid() {

        try {

            return Build.VERSION.RELEASE;

        } catch (Exception e) {

            return "Versão desconhecida";
        }
    }

    /**
     * Retorna o nível da API Android.
     */
    public int obterNivelApiAndroid() {

        return Build.VERSION.SDK_INT;
    }

    /**
     * Retorna a versão completa do sistema.
     */
    public String obterSistemaAndroid() {

        return "Android "
                + obterVersaoAndroid()
                + " (API "
                + obterNivelApiAndroid()
                + ")";
    }

    // ============================================================
    // RESUMOS PRONTOS PARA O JARVIS
    // ============================================================

    /**
     * Retorna um resumo da bateria.
     */
    public String resumoBateria() {

        StringBuilder texto =
                new StringBuilder();

        texto.append(
                "Bateria: "
        );

        texto.append(
                obterBateriaTexto()
        );

        texto.append(
                ". Estado: "
        );

        texto.append(
                obterEstadoBateria()
        );

        String temperatura =
                obterTemperaturaBateriaTexto();

        if (!temperatura.equals(
                "Temperatura indisponível."
        )) {

            texto.append(
                    ". Temperatura: "
            );

            texto.append(
                    temperatura
            );
        }

        return texto.toString();
    }

    /**
     * Retorna um resumo da memória RAM.
     */
    public String resumoRam() {

        return "RAM total: "
                + obterRamTotalTexto()
                + ". RAM utilizada: "
                + obterRamUsadaTexto()
                + ". RAM disponível: "
                + obterRamDisponivelTexto()
                + ".";
    }

    /**
     * Retorna um resumo do armazenamento.
     */
    public String resumoArmazenamento() {

        return "Armazenamento total: "
                + obterArmazenamentoTotalTexto()
                + ". Utilizado: "
                + obterArmazenamentoUsadoTexto()
                + ". Disponível: "
                + obterArmazenamentoDisponivelTexto()
                + ".";
    }

    /**
     * Retorna informações gerais do aparelho.
     */
    public String resumoAparelho() {

        return "Aparelho: "
                + obterNomeAparelho()
                + ". Sistema: "
                + obterSistemaAndroid()
                + ". "
                + resumoBateria()
                + " "
                + resumoRam()
                + " "
                + resumoArmazenamento();
    }

    /**
     * Retorna apenas as informações que normalmente
     * seriam usadas pelo comando "informações do aparelho".
     */
    public String obterInformacoesAparelho() {

        StringBuilder texto =
                new StringBuilder();

        texto.append(
                "INFORMAÇÕES DO APARELHO\n\n"
        );

        texto.append(
                "Fabricante: "
        );

        texto.append(
                obterFabricante()
        );

        texto.append(
                "\nModelo: "
        );

        texto.append(
                obterModelo()
        );

        texto.append(
                "\nSistema: "
        );

        texto.append(
                obterSistemaAndroid()
        );

        texto.append(
                "\n\n"
        );

        texto.append(
                resumoBateria()
        );

        texto.append(
                "\n"
        );

        texto.append(
                resumoRam()
        );

        texto.append(
                "\n"
        );

        texto.append(
                resumoArmazenamento()
        );

        return texto.toString();
    }
}
