package com.jarvis.lite;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Central de ferramentas do JARVIS.
 *
 * O JarvisBrain pode utilizar esta classe para executar
 * funções locais do aparelho sem precisar conhecer os
 * detalhes internos de cada ferramenta.
 *
 * Este módulo não utiliza Internet.
 */
public class JarvisToolManager {

    private final Context context;
    private final JarvisDeviceTools deviceTools;

    public JarvisToolManager(Context context) {
        this.context = context.getApplicationContext();
        this.deviceTools = new JarvisDeviceTools(this.context);
    }

    /**
     * Retorna acesso às ferramentas do aparelho.
     */
    public JarvisDeviceTools getDeviceTools() {
        return deviceTools;
    }

    /**
     * Retorna a data atual.
     */
    public String obterData() {

        SimpleDateFormat formato =
                new SimpleDateFormat(
                        "dd/MM/yyyy",
                        new Locale("pt", "BR")
                );

        return formato.format(new Date());
    }

    /**
     * Retorna a hora atual.
     */
    public String obterHora() {

        SimpleDateFormat formato =
                new SimpleDateFormat(
                        "HH:mm",
                        new Locale("pt", "BR")
                );

        return formato.format(new Date());
    }

    /**
     * Retorna informações completas do aparelho.
     */
    public String obterInformacoesAparelho() {

        return deviceTools.obterInformacoesAparelho();
    }

    /**
     * Retorna informações da bateria.
     */
    public String obterBateria() {

        return deviceTools.resumoBateria();
    }

    /**
     * Retorna informações da RAM.
     */
    public String obterRam() {

        return deviceTools.resumoRam();
    }

    /**
     * Retorna informações do armazenamento.
     */
    public String obterArmazenamento() {

        return deviceTools.resumoArmazenamento();
    }

    /**
     * Retorna informações do sistema Android.
     */
    public String obterSistema() {

        return deviceTools.obterSistemaAndroid();
    }

    /**
     * Retorna fabricante e modelo.
     */
    public String obterModeloAparelho() {

        return deviceTools.obterNomeAparelho();
    }

    /**
     * Retorna temperatura da bateria.
     */
    public String obterTemperaturaBateria() {

        return deviceTools.obterTemperaturaBateriaTexto();
    }

    /**
     * Executa uma ferramenta pelo nome.
     *
     * Esta função será usada posteriormente pelo JarvisBrain.
     */
    public String executar(String ferramenta) {

        if (ferramenta == null) {
            return "";
        }

        String comando = ferramenta
                .trim()
                .toLowerCase(Locale.ROOT);

        switch (comando) {

            case "hora":
            case "horas":
            case "que horas são":
            case "qual a hora":
                return "Agora são " + obterHora() + ".";

            case "data":
            case "qual a data":
            case "data de hoje":
                return "Hoje é " + obterData() + ".";

            case "bateria":
            case "nivel da bateria":
            case "nível da bateria":
                return obterBateria();

            case "temperatura":
            case "temperatura da bateria":
            case "temperatura bateria":
                return obterTemperaturaBateria();

            case "ram":
            case "memoria ram":
            case "memória ram":
                return obterRam();

            case "armazenamento":
            case "espaco":
            case "espaço":
            case "memoria interna":
            case "memória interna":
                return obterArmazenamento();

            case "aparelho":
            case "informacoes do aparelho":
            case "informações do aparelho":
                return obterInformacoesAparelho();

            case "android":
            case "sistema":
            case "versao android":
            case "versão android":
                return obterSistema();

            case "modelo":
            case "modelo do aparelho":
                return obterModeloAparelho();

            default:
                return null;
        }
    }

    /**
     * Verifica se uma ferramenta existe.
     */
    public boolean possuiFerramenta(String ferramenta) {

        if (ferramenta == null) {
            return false;
        }

        String comando = ferramenta
                .trim()
                .toLowerCase(Locale.ROOT);

        switch (comando) {

            case "hora":
            case "horas":
            case "que horas são":
            case "qual a hora":

            case "data":
            case "qual a data":
            case "data de hoje":

            case "bateria":
            case "nivel da bateria":
            case "nível da bateria":

            case "temperatura":
            case "temperatura da bateria":
            case "temperatura bateria":

            case "ram":
            case "memoria ram":
            case "memória ram":

            case "armazenamento":
            case "espaco":
            case "espaço":
            case "memoria interna":
            case "memória interna":

            case "aparelho":
            case "informacoes do aparelho":
            case "informações do aparelho":

            case "android":
            case "sistema":
            case "versao android":
            case "versão android":

            case "modelo":
            case "modelo do aparelho":
                return true;

            default:
                return false;
        }
    }

    /**
     * Retorna uma lista simples das ferramentas locais disponíveis.
     */
    public String listarFerramentas() {

        return "Ferramentas locais disponíveis: "
                + "hora, data, bateria, temperatura da bateria, "
                + "RAM, armazenamento, informações do aparelho, "
                + "Android e modelo do aparelho.";
    }

    /**
     * Libera recursos.
     *
     * Atualmente não há recursos externos que precisem ser
     * liberados aqui, mas o método deixa a estrutura preparada
     * para futuras ferramentas.
     */
    public void destroy() {
        // Reservado para futuras ferramentas.
    }
}
