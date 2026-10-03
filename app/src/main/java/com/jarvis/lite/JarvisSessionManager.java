package com.jarvis.lite;

import android.content.Context;

import java.util.Locale;

/**
 * Gerenciador de sessão do JARVIS.
 *
 * Controla:
 * - modo sono
 * - modo ativo
 * - palavra de ativação
 * - encerramento da sessão
 * - último comando
 * - tempo da sessão
 * - integração com o contexto da conversa
 *
 * Não inicia o microfone e não acessa a Internet.
 */
public class JarvisSessionManager {

    public enum SessionState {
        SLEEP,
        ACTIVE
    }

    private static final String DEFAULT_WAKE_WORD = "jarvis";

    private static final String DEFAULT_END_PHRASE =
            "encerrar sessão";

    private static final String DEFAULT_GREETING =
            "À sua disposição, sistema operacional online, em que posso ajudar?";

    private final Context context;
    private final JarvisContextManager contextManager;

    private SessionState state = SessionState.SLEEP;

    private String wakeWord = DEFAULT_WAKE_WORD;
    private String endPhrase = DEFAULT_END_PHRASE;

    private String lastCommand = "";
    private long sessionStartedAt = 0L;
    private long lastInteractionAt = 0L;

    public JarvisSessionManager(Context context) {

        this.context = context.getApplicationContext();

        this.contextManager =
                new JarvisContextManager(this.context);
    }

    /**
     * Retorna o estado atual.
     */
    public synchronized SessionState getState() {
        return state;
    }

    /**
     * Verifica se o JARVIS está em modo ativo.
     */
    public synchronized boolean isActive() {
        return state == SessionState.ACTIVE;
    }

    /**
     * Verifica se o JARVIS está em modo sono.
     */
    public synchronized boolean isSleeping() {
        return state == SessionState.SLEEP;
    }

    /**
     * Configura a palavra de ativação.
     */
    public synchronized void setWakeWord(String word) {

        String normalized = normalize(word);

        if (!normalized.isEmpty()) {
            wakeWord = normalized;
        }
    }

    /**
     * Retorna a palavra de ativação atual.
     */
    public synchronized String getWakeWord() {
        return wakeWord;
    }

    /**
     * Configura a frase de encerramento.
     */
    public synchronized void setEndPhrase(String phrase) {

        String normalized = normalize(phrase);

        if (!normalized.isEmpty()) {
            endPhrase = normalized;
        }
    }

    /**
     * Retorna a frase de encerramento.
     */
    public synchronized String getEndPhrase() {
        return endPhrase;
    }

    /**
     * Verifica se um texto contém a palavra de ativação.
     */
    public synchronized boolean containsWakeWord(String text) {

        String normalized = normalize(text);

        if (normalized.isEmpty()) {
            return false;
        }

        return containsPhrase(
                normalized,
                wakeWord
        );
    }

    /**
     * Verifica se um texto contém a frase de encerramento.
     */
    public synchronized boolean containsEndPhrase(String text) {

        String normalized = normalize(text);

        if (normalized.isEmpty()) {
            return false;
        }

        return containsPhrase(
                normalized,
                endPhrase
        );
    }

    /**
     * Ativa uma nova sessão.
     *
     * Retorna true somente quando houve mudança
     * de SLEEP para ACTIVE.
     */
    public synchronized boolean activate() {

        if (state == SessionState.ACTIVE) {
            return false;
        }

        state = SessionState.ACTIVE;

        long now = System.currentTimeMillis();

        sessionStartedAt = now;
        lastInteractionAt = now;
        lastCommand = "";

        contextManager.clearConversation();

        return true;
    }

    /**
     * Entra novamente no modo sono.
     *
     * A memória permanente NÃO é apagada.
     *
     * Apenas a conversa temporária da sessão é encerrada.
     */
    public synchronized boolean sleep() {

        if (state == SessionState.SLEEP) {
            return false;
        }

        state = SessionState.SLEEP;

        lastInteractionAt =
                System.currentTimeMillis();

        lastCommand = "";

        contextManager.clearConversation();

        return true;
    }

    /**
     * Processa a ativação a partir de um texto.
     *
     * Exemplo:
     *
     * "Jarvis"
     *
     * Retorna true quando a palavra foi encontrada
     * e a sessão foi ativada.
     */
    public synchronized boolean processWakeWord(
            String text) {

        if (isActive()) {
            return false;
        }

        if (!containsWakeWord(text)) {
            return false;
        }

        return activate();
    }

    /**
     * Processa uma entrada durante a sessão.
     *
     * Retorna false quando o comando encerra
     * a sessão.
     */
    public synchronized boolean processCommand(
            String command) {

        if (!isActive()) {
            return false;
        }

        String normalized =
                normalize(command);

        if (normalized.isEmpty()) {
            return true;
        }

        lastCommand = normalized;

        lastInteractionAt =
                System.currentTimeMillis();

        if (containsEndPhrase(normalized)) {
            sleep();
            return false;
        }

        contextManager.addUserMessage(
                command
        );

        return true;
    }

    /**
     * Registra uma resposta do JARVIS.
     */
    public synchronized void registerJarvisResponse(
            String response) {

        if (!isActive()) {
            return;
        }

        if (response == null
                || response.trim().isEmpty()) {
            return;
        }

        contextManager.addJarvisMessage(
                response
        );

        lastInteractionAt =
                System.currentTimeMillis();
    }

    /**
     * Retorna a mensagem inicial da sessão.
     */
    public synchronized String getActivationMessage() {
        return DEFAULT_GREETING;
    }

    /**
     * Retorna o último comando recebido.
     */
    public synchronized String getLastCommand() {
        return lastCommand;
    }

    /**
     * Retorna o momento em que a sessão começou.
     */
    public synchronized long getSessionStartedAt() {
        return sessionStartedAt;
    }

    /**
     * Retorna o momento da última interação.
     */
    public synchronized long getLastInteractionAt() {
        return lastInteractionAt;
    }

    /**
     * Retorna há quanto tempo a sessão está ativa.
     */
    public synchronized long getSessionDurationMillis() {

        if (!isActive()) {
            return 0L;
        }

        return System.currentTimeMillis()
                - sessionStartedAt;
    }

    /**
     * Retorna o contexto da conversa atual.
     */
    public synchronized String getConversationContext() {

        return contextManager
                .buildConversationContext();
    }

    /**
     * Retorna o contexto completo disponível
     * para o processamento local.
     */
    public synchronized String getFullContext() {

        return contextManager
                .buildFullContext();
    }

    /**
     * Retorna o gerenciador de contexto.
     */
    public synchronized JarvisContextManager
    getContextManager() {

        return contextManager;
    }

    /**
     * Retorna a memória do JARVIS.
     */
    public synchronized JarvisMemory getMemory() {

        return contextManager.getMemory();
    }

    /**
     * Limpa somente a conversa atual.
     *
     * A memória permanente permanece intacta.
     */
    public synchronized void clearCurrentConversation() {

        contextManager.clearConversation();
    }

    /**
     * Retorna um resumo do estado atual.
     */
    public synchronized String getStatus() {

        if (state == SessionState.ACTIVE) {

            return "JARVIS ativo. "
                    + "Sessão em andamento.";
        }

        return "JARVIS em modo sono.";
    }

    /**
     * Retorna o estado como texto simples.
     */
    public synchronized String getStateName() {

        if (state == SessionState.ACTIVE) {
            return "ACTIVE";
        }

        return "SLEEP";
    }

    /**
     * Normaliza texto para comparação de comandos.
     */
    private String normalize(String value) {

        if (value == null) {
            return "";
        }

        String normalized =
                JarvisSecurity.normalizeCommand(
                        value
                );

        return normalized
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    /**
     * Procura uma frase inteira dentro do texto.
     */
    private boolean containsPhrase(
            String text,
            String phrase) {

        if (text == null
                || phrase == null
                || text.isEmpty()
                || phrase.isEmpty()) {

            return false;
        }

        String paddedText =
                " " + text + " ";

        String paddedPhrase =
                " " + phrase + " ";

        return paddedText.contains(
                paddedPhrase
        );
    }
}
