package com.jarvis.lite;

import android.content.Context;

/**
 * Gerenciador da palavra de ativação do JARVIS.
 *
 * Responsabilidades:
 * - Detectar a palavra "Jarvis".
 * - Detectar a frase de encerramento.
 * - Ativar a sessão.
 * - Encerrar a sessão.
 * - Informar o estado atual.
 *
 * IMPORTANTE:
 * Este arquivo NÃO controla diretamente o microfone.
 * A captura de áudio continuará sendo responsabilidade
 * da camada de voz do Android.
 *
 * Não acessa a Internet.
 */
public class JarvisWakeWordManager {

    private final Context context;
    private final JarvisSessionManager sessionManager;

    private boolean wakeWordEnabled = true;

    /**
     * Palavra padrão de ativação.
     */
    private String wakeWord = "jarvis";

    /**
     * Frase padrão para encerrar a sessão.
     */
    private String endPhrase = "encerrar sessão";

    public JarvisWakeWordManager(Context context) {

        this.context =
                context.getApplicationContext();

        this.sessionManager =
                new JarvisSessionManager(
                        this.context
                );

        sincronizarConfiguracao();
    }

    /**
     * Retorna o gerenciador de sessão.
     */
    public JarvisSessionManager getSessionManager() {

        return sessionManager;
    }

    /**
     * Ativa ou desativa a detecção da palavra de ativação.
     */
    public synchronized void setWakeWordEnabled(
            boolean enabled) {

        wakeWordEnabled = enabled;
    }

    /**
     * Verifica se a palavra de ativação está habilitada.
     */
    public synchronized boolean isWakeWordEnabled() {

        return wakeWordEnabled;
    }

    /**
     * Define uma nova palavra de ativação.
     *
     * Exemplo:
     * "jarvis"
     * "computador"
     *
     * A palavra é normalizada pelo sistema.
     */
    public synchronized void setWakeWord(
            String word) {

        if (word == null) {
            return;
        }

        String normalized =
                JarvisSecurity.normalizeCommand(
                        word
                );

        if (normalized.isEmpty()) {
            return;
        }

        wakeWord = normalized;

        sessionManager.setWakeWord(
                normalized
        );
    }

    /**
     * Retorna a palavra de ativação atual.
     */
    public synchronized String getWakeWord() {

        return wakeWord;
    }

    /**
     * Define a frase utilizada para encerrar
     * a sessão do JARVIS.
     */
    public synchronized void setEndPhrase(
            String phrase) {

        if (phrase == null) {
            return;
        }

        String normalized =
                JarvisSecurity.normalizeCommand(
                        phrase
                );

        if (normalized.isEmpty()) {
            return;
        }

        endPhrase = normalized;

        sessionManager.setEndPhrase(
                normalized
        );
    }

    /**
     * Retorna a frase de encerramento atual.
     */
    public synchronized String getEndPhrase() {

        return endPhrase;
    }

    /**
     * Processa um texto recebido pela camada
     * de reconhecimento de voz.
     *
     * Se JARVIS estiver dormindo e a palavra
     * de ativação aparecer, a sessão será ativada.
     *
     * Retorna true quando uma ativação ocorreu.
     */
    public synchronized boolean processWakeText(
            String recognizedText) {

        if (!wakeWordEnabled) {
            return false;
        }

        if (recognizedText == null
                || recognizedText.trim().isEmpty()) {

            return false;
        }

        if (sessionManager.isActive()) {
            return false;
        }

        boolean detected =
                sessionManager.containsWakeWord(
                        recognizedText
                );

        if (!detected) {
            return false;
        }

        return sessionManager.activate();
    }

    /**
     * Processa um comando recebido enquanto
     * JARVIS está em uma sessão ativa.
     *
     * Retorna true quando o comando continua
     * dentro da sessão.
     *
     * Retorna false quando a sessão foi encerrada.
     */
    public synchronized boolean processSessionText(
            String recognizedText) {

        if (recognizedText == null
                || recognizedText.trim().isEmpty()) {

            return sessionManager.isActive();
        }

        if (!sessionManager.isActive()) {
            return false;
        }

        return sessionManager.processCommand(
                recognizedText
        );
    }

    /**
     * Verifica diretamente se um texto contém
     * a palavra de ativação.
     */
    public synchronized boolean containsWakeWord(
            String text) {

        if (!wakeWordEnabled) {
            return false;
        }

        return sessionManager.containsWakeWord(
                text
        );
    }

    /**
     * Verifica se um texto contém a frase
     * de encerramento.
     */
    public synchronized boolean containsEndPhrase(
            String text) {

        return sessionManager.containsEndPhrase(
                text
        );
    }

    /**
     * Força a ativação da sessão.
     *
     * Útil para a camada oficial de interação
     * por voz do Android.
     */
    public synchronized boolean activateSession() {

        return sessionManager.activate();
    }

    /**
     * Força o encerramento da sessão.
     */
    public synchronized boolean endSession() {

        return sessionManager.sleep();
    }

    /**
     * Retorna true se JARVIS estiver ativo.
     */
    public synchronized boolean isSessionActive() {

        return sessionManager.isActive();
    }

    /**
     * Retorna true se JARVIS estiver dormindo.
     */
    public synchronized boolean isSessionSleeping() {

        return sessionManager.isSleeping();
    }

    /**
     * Retorna a mensagem de ativação.
     */
    public synchronized String getActivationMessage() {

        return sessionManager
                .getActivationMessage();
    }

    /**
     * Retorna o estado atual em texto.
     */
    public synchronized String getStatus() {

        if (sessionManager.isActive()) {

            return "JARVIS ativo — "
                    + "sessão em andamento.";
        }

        return "JARVIS em modo sono.";
    }

    /**
     * Sincroniza a configuração local deste
     * gerenciador com o SessionManager.
     */
    private synchronized void sincronizarConfiguracao() {

        sessionManager.setWakeWord(
                wakeWord
        );

        sessionManager.setEndPhrase(
                endPhrase
        );
    }
}
