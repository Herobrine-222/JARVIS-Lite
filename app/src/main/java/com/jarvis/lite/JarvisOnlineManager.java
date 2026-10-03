package com.jarvis.lite;

import android.content.Context;

/**
 * Gerenciador da infraestrutura online do JARVIS.
 *
 * Responsabilidades:
 * - controlar a disponibilidade do modo online;
 * - controlar se a conexão online está habilitada;
 * - preparar a comunicação com um futuro provedor de IA;
 * - manter o modo offline como alternativa;
 *
 * IMPORTANTE:
 * Este módulo não realiza conexão automaticamente.
 * A conexão real será adicionada posteriormente.
 */
public class JarvisOnlineManager {

    private final Context context;

    private boolean onlineEnabled = false;
    private boolean connected = false;

    private String providerName = "";

    public JarvisOnlineManager(Context context) {

        this.context =
                context.getApplicationContext();
    }

    /**
     * Habilita o modo online.
     *
     * Não estabelece uma conexão por si só.
     */
    public synchronized void enableOnline() {

        onlineEnabled = true;
    }

    /**
     * Desabilita o modo online.
     *
     * Também marca a conexão como encerrada.
     */
    public synchronized void disableOnline() {

        onlineEnabled = false;
        connected = false;
        providerName = "";
    }

    /**
     * Verifica se o modo online está habilitado.
     */
    public synchronized boolean isOnlineEnabled() {

        return onlineEnabled;
    }

    /**
     * Verifica se existe uma conexão ativa.
     */
    public synchronized boolean isConnected() {

        return connected;
    }

    /**
     * Define o nome do provedor online.
     *
     * Exemplo:
     * "OpenAI"
     * "Grok"
     * "Gemini"
     *
     * Isto apenas registra a configuração.
     * Não realiza conexão.
     */
    public synchronized void setProviderName(
            String provider) {

        if (provider == null) {
            providerName = "";
            return;
        }

        providerName =
                provider.trim();
    }

    /**
     * Retorna o provedor configurado.
     */
    public synchronized String getProviderName() {

        return providerName;
    }

    /**
     * Marca a conexão como estabelecida.
     *
     * Será utilizado pela futura camada
     * responsável pela comunicação real.
     */
    public synchronized void markConnected(
            String provider) {

        if (!onlineEnabled) {
            connected = false;
            return;
        }

        if (provider != null
                && !provider.trim().isEmpty()) {

            providerName =
                    provider.trim();
        }

        connected = true;
    }

    /**
     * Marca a conexão como encerrada.
     */
    public synchronized void markDisconnected() {

        connected = false;
    }

    /**
     * Retorna se o sistema pode tentar utilizar
     * um provedor online.
     */
    public synchronized boolean canUseOnline() {

        return onlineEnabled
                && connected;
    }

    /**
     * Retorna o estado atual da infraestrutura online.
     */
    public synchronized String getStatus() {

        if (!onlineEnabled) {

            return "Modo online desativado.";
        }

        if (!connected) {

            return "Modo online ativado, "
                    + "mas não conectado.";
        }

        if (providerName.isEmpty()) {

            return "Conectado sem provedor definido.";
        }

        return "Conectado ao provedor: "
                + providerName;
    }

    /**
     * Prepara uma solicitação para o futuro
     * provedor online.
     *
     * Neste estágio não envia nada pela Internet.
     *
     * Retorna null porque ainda não existe
     * um cliente online conectado.
     */
    public synchronized String processOnlineRequest(
            String input,
            String context) {

        if (!canUseOnline()) {

            return null;
        }

        /*
         * A implementação real do provedor
         * será adicionada posteriormente.
         */
        return null;
    }

    /**
     * Encerra toda a infraestrutura online.
     */
    public synchronized void shutdown() {

        connected = false;
        onlineEnabled = false;
        providerName = "";
    }

    /**
     * Retorna o Contexto da aplicação.
     */
    public Context getContext() {

        return context;
    }
}
