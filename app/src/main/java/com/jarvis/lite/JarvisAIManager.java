package com.jarvis.lite;

import android.content.Context;

/**
 * Gerenciador central da inteligência do JARVIS.
 *
 * Responsabilidades:
 * - controlar o modo OFFLINE/ONLINE;
 * - utilizar o JarvisBrain no modo offline;
 * - utilizar o JarvisOnlineManager quando houver
 *   um provedor online conectado;
 * - manter o offline como modo padrão;
 * - permitir troca de modo sem alterar o restante
 *   da arquitetura.
 *
 * IMPORTANTE:
 * Este arquivo não cria uma conexão com a Internet.
 * A conexão real depende do JarvisOnlineManager
 * e de um provedor que será conectado posteriormente.
 */
public class JarvisAIManager {

    public enum AIMode {
        OFFLINE,
        ONLINE
    }

    private final Context context;

    private final JarvisBrain offlineBrain;
    private final JarvisOnlineManager onlineManager;

    private AIMode currentMode = AIMode.OFFLINE;

    public JarvisAIManager(Context context) {

        this.context =
                context.getApplicationContext();

        /*
         * Cérebro local.
         */
        this.offlineBrain =
                new JarvisBrain(
                        this.context
                );

        /*
         * Gerenciador da infraestrutura online.
         */
        this.onlineManager =
                new JarvisOnlineManager(
                        this.context
                );
    }

    /**
     * Retorna o modo atual da IA.
     */
    public synchronized AIMode getMode() {

        return currentMode;
    }

    /**
     * Retorna o nome do modo atual.
     */
    public synchronized String getModeName() {

        if (currentMode == AIMode.ONLINE) {
            return "ONLINE";
        }

        return "OFFLINE";
    }

    /**
     * Ativa o modo offline.
     *
     * O JARVIS utilizará o cérebro local.
     */
    public synchronized void enableOfflineMode() {

        currentMode =
                AIMode.OFFLINE;
    }

    /**
     * Ativa o modo online.
     *
     * IMPORTANTE:
     * isso apenas seleciona o modo online.
     *
     * Não cria conexão automaticamente.
     */
    public synchronized void enableOnlineMode() {

        currentMode =
                AIMode.ONLINE;

        onlineManager.enableOnline();
    }

    /**
     * Desativa o modo online e retorna
     * ao modo offline.
     */
    public synchronized void disableOnlineMode() {

        onlineManager.disableOnline();

        currentMode =
                AIMode.OFFLINE;
    }

    /**
     * Verifica se o modo atual é offline.
     */
    public synchronized boolean isOfflineMode() {

        return currentMode ==
                AIMode.OFFLINE;
    }

    /**
     * Verifica se o modo atual é online.
     */
    public synchronized boolean isOnlineMode() {

        return currentMode ==
                AIMode.ONLINE;
    }

    /**
     * Verifica se a infraestrutura online
     * está habilitada.
     */
    public synchronized boolean isOnlineEnabled() {

        return onlineManager
                .isOnlineEnabled();
    }

    /**
     * Verifica se existe conexão com
     * um provedor online.
     */
    public synchronized boolean isOnlineConnected() {

        return onlineManager
                .isConnected();
    }

    /**
     * Retorna o gerenciador online.
     */
    public JarvisOnlineManager
    getOnlineManager() {

        return onlineManager;
    }

    /**
     * Retorna o cérebro offline.
     */
    public JarvisBrain getOfflineBrain() {

        return offlineBrain;
    }

    /**
     * Processa uma mensagem usando o modo
     * atualmente selecionado.
     *
     * OFFLINE:
     * utiliza JarvisBrain.
     *
     * ONLINE:
     * tenta utilizar o provedor online.
     *
     * Caso o provedor online não esteja
     * realmente conectado, utiliza o cérebro
     * offline como fallback.
     */
    public synchronized String process(
            String input,
            String context) {

        if (input == null
                || input.trim().isEmpty()) {

            return "";
        }

        /*
         * MODO OFFLINE
         */
        if (currentMode ==
                AIMode.OFFLINE) {

            return offlineBrain.process(
                    input,
                    context
            );
        }

        /*
         * MODO ONLINE
         */
        if (currentMode ==
                AIMode.ONLINE) {

            /*
             * Só tenta o provedor online se
             * realmente houver conexão.
             */
            if (onlineManager.canUseOnline()) {

                String onlineResponse =
                        onlineManager
                                .processOnlineRequest(
                                        input,
                                        context
                                );

                /*
                 * Se o provedor retornar uma
                 * resposta válida, utilizamos ela.
                 */
                if (onlineResponse != null
                        && !onlineResponse
                                .trim()
                                .isEmpty()) {

                    return onlineResponse;
                }
            }

            /*
             * FALLBACK OFFLINE
             *
             * Se o modo online estiver selecionado,
             * mas ainda não houver um provedor
             * conectado, o cérebro local continua
             * disponível.
             */
            return offlineBrain.process(
                    input,
                    context
            );
        }

        /*
         * Segurança:
         * o modo padrão sempre será offline.
         */
        return offlineBrain.process(
                input,
                context
        );
    }

    /**
     * Processa explicitamente utilizando
     * somente o cérebro offline.
     */
    public synchronized String processOffline(
            String input,
            String context) {

        return offlineBrain.process(
                input,
                context
        );
    }

    /**
     * Processa explicitamente utilizando
     * somente o provedor online.
     *
     * Se não houver conexão, retorna null.
     *
     * Não faz fallback neste método porque
     * ele foi criado para quem deseja exigir
     * processamento online.
     */
    public synchronized String processOnline(
            String input,
            String context) {

        if (input == null
                || input.trim().isEmpty()) {

            return "";
        }

        if (!onlineManager.canUseOnline()) {

            return null;
        }

        return onlineManager
                .processOnlineRequest(
                        input,
                        context
                );
    }

    /**
     * Define o provedor online.
     *
     * Isso apenas configura o nome.
     * Não estabelece conexão.
     */
    public synchronized void setOnlineProvider(
            String providerName) {

        onlineManager.setProviderName(
                providerName
        );
    }

    /**
     * Retorna o provedor online configurado.
     */
    public synchronized String
    getOnlineProvider() {

        return onlineManager
                .getProviderName();
    }

    /**
     * Informa ao sistema que um provedor
     * online foi conectado.
     */
    public synchronized void markOnlineConnected(
            String providerName) {

        onlineManager.markConnected(
                providerName
        );

        /*
         * Se o usuário já tiver escolhido
         * o modo online, ele continuará online.
         */
    }

    /**
     * Informa ao sistema que o provedor
     * online foi desconectado.
     */
    public synchronized void markOnlineDisconnected() {

        onlineManager.markDisconnected();
    }

    /**
     * Alterna entre OFFLINE e ONLINE.
     *
     * OFFLINE → ONLINE
     * ONLINE → OFFLINE
     */
    public synchronized void toggleMode() {

        if (currentMode ==
                AIMode.OFFLINE) {

            enableOnlineMode();

        } else {

            disableOnlineMode();
        }
    }

    /**
     * Restaura o estado seguro padrão:
     * OFFLINE e sem conexão online.
     */
    public synchronized void resetToOffline() {

        onlineManager.shutdown();

        currentMode =
                AIMode.OFFLINE;
    }

    /**
     * Retorna uma descrição do estado atual.
     */
    public synchronized String getStatus() {

        if (currentMode ==
                AIMode.OFFLINE) {

            return "JARVIS em modo offline.";
        }

        if (!onlineManager.isOnlineEnabled()) {

            return "Modo online selecionado, "
                    + "mas desativado.";
        }

        if (!onlineManager.isConnected()) {

            return "Modo online selecionado, "
                    + "aguardando conexão.";
        }

        String provider =
                onlineManager
                        .getProviderName();

        if (provider == null
                || provider.trim().isEmpty()) {

            return "JARVIS online, "
                    + "sem provedor definido.";
        }

        return "JARVIS online — provedor: "
                + provider;
    }

    /**
     * Libera os recursos do gerenciador online.
     */
    public synchronized void shutdown() {

        onlineManager.shutdown();

        currentMode =
                AIMode.OFFLINE;
    }
 }
