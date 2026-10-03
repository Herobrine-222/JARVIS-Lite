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

    public synchronized SessionState getState() {
        return state;
    }

    public synchronized boolean isActive() {
        return state == SessionState.ACTIVE;
    }

    public synchronized boolean isSleeping() {
        return state == SessionState.SLEEP;
    }

    public synchronized void setWakeWord(String word) {

        String normalized = normalize(word);

        if (!normalized.isEmpty()) {
            wakeWord = normalized;
        }
    }

    public synchronized String getWakeWord() {
        return wakeWord;
    }

    public synchronized void setEndPhrase(String phrase) {

        String normalized = normalize(phrase);

        if (!normalized.isEmpty()) {
            endPhrase = normalized;
        }
    }

    public synchronized String getEndPhrase() {
        return endPhrase;
    }

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

    public synchronized String getActivationMessage() {
        return DEFAULT_GREETING;
    }

    public synchronized String getLastCommand() {
        return lastCommand;
    }

    public synchronized long getSessionStartedAt() {
        return sessionStartedAt;
    }

    public synchronized long getLastInteractionAt() {
        return lastInteractionAt;
    }

    public synchronized long getSessionDurationMillis() {

        if (!isActive()) {
            return 0L;
        }

        return System.currentTimeMillis()
                - sessionStartedAt;
    }

    public synchronized String getConversationContext() {

        return contextManager
                .buildConversationContext();
    }

    public synchronized String getFullContext() {

        return contextManager
                .buildFullContext(
                        contextManager
                                .getLastUserMessage()
                );
    }

    public synchronized JarvisContextManager
    getContextManager() {

        return contextManager;
    }

    public synchronized JarvisMemory getMemory() {

        return contextManager.getMemory();
    }

    public synchronized void clearCurrentConversation() {

        contextManager.clearConversation();
    }

    public synchronized String getStatus() {

        if (state == SessionState.ACTIVE) {

            return "JARVIS ativo. "
                    + "Sessão em andamento.";
        }

        return "JARVIS em modo sono.";
    }

    public synchronized String getStateName() {

        if (state == SessionState.ACTIVE) {
            return "ACTIVE";
        }

        return "SLEEP";
    }

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
