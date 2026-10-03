package com.jarvis.lite;

import android.content.Context;

/**
 * Gerenciador principal dos modos de IA do JARVIS.
 *
 * Responsável por:
 * - controlar o modo offline;
 * - controlar o modo online;
 * - utilizar o JarvisBrain no modo offline;
 * - manter a estrutura preparada para uma IA online;
 * - não acessar a Internet automaticamente.
 */
public class JarvisAIManager {

    public enum AIMode {
        OFFLINE,
        ONLINE
    }

    private final Context context;
    private final JarvisBrain offlineBrain;

    private AIMode currentMode = AIMode.OFFLINE;

    private boolean onlineEnabled = false;

    public JarvisAIManager(Context context) {

        this.context =
                context.getApplicationContext();

        this.offlineBrain =
                new JarvisBrain(this.context);
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
     * O JARVIS passa a utilizar exclusivamente
     * o cérebro local.
     */
    public synchronized void enableOfflineMode() {

        currentMode = AIMode.OFFLINE;
    }

    /**
     * Solicita a ativação do modo online.
     *
     * Esta função NÃO abre conexão de Internet.
     *
     * Ela apenas habilita o modo para que uma
     * futura camada online possa ser conectada.
     */
    public synchronized void enableOnlineMode() {

        onlineEnabled = true;
        currentMode = AIMode.ONLINE;
    }

    /**
     * Desativa completamente o modo online
     * e retorna ao modo offline.
     */
    public synchronized void disableOnlineMode() {

        onlineEnabled = false;
        currentMode = AIMode.OFFLINE;
    }

    /**
     * Verifica se o modo online está habilitado.
     */
    public synchronized boolean isOnlineEnabled() {

        return onlineEnabled;
    }

    /**
     * Verifica se o JARVIS está funcionando
     * no modo offline.
     */
    public synchronized boolean isOfflineMode() {

        return currentMode == AIMode.OFFLINE;
    }

    /**
     * Verifica se o JARVIS está configurado
     * para o modo online.
     */
    public synchronized boolean isOnlineMode() {

        return currentMode == AIMode.ONLINE;
    }

    /**
     * Processa uma mensagem usando a IA
     * disponível atualmente.
     *
     * No estado atual do projeto:
     * - OFFLINE usa JarvisBrain;
     * - ONLINE ainda não possui um provedor
     *   externo conectado.
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
        if (currentMode == AIMode.OFFLINE) {

            return offlineBrain.process(
                    input,
                    context
            );
        }

        /*
         * MODO ONLINE
         *
         * A infraestrutura online será conectada
         * posteriormente.
         *
         * Não fazemos uma conexão automática aqui.
         */
        return "O modo online está ativado, "
                + "mas o provedor de IA online "
                + "ainda não foi conectado.";
    }

    /**
     * Processa uma mensagem usando
     * explicitamente o modo offline.
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
     * Retorna o cérebro offline.
     */
    public JarvisBrain getOfflineBrain() {

        return offlineBrain;
    }

    /**
     * Retorna o estado atual do sistema.
     */
    public synchronized String getStatus() {

        if (currentMode == AIMode.ONLINE) {

            if (onlineEnabled) {

                return "JARVIS em modo online.";
            }

            return "Modo online indisponível.";
        }

        return "JARVIS em modo offline.";
    }

    /**
     * Alterna entre offline e online.
     *
     * Se estiver offline, passa para online.
     * Se estiver online, retorna para offline.
     */
    public synchronized void toggleMode() {

        if (currentMode == AIMode.OFFLINE) {

            enableOnlineMode();

        } else {

            disableOnlineMode();
        }
    }

    /**
     * Reseta o sistema para o modo offline.
     *
     * Útil como comportamento seguro padrão.
     */
    public synchronized void resetToOffline() {

        onlineEnabled = false;
        currentMode = AIMode.OFFLINE;
    }
}
