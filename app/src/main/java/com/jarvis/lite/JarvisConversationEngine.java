package com.jarvis.lite;

import android.content.Context;

/**
 * Motor de conversação do JARVIS.
 *
 * Responsável por:
 * - receber comandos do usuário;
 * - verificar o estado da sessão;
 * - montar o contexto da conversa;
 * - enviar o processamento para o JarvisAIManager;
 * - registrar mensagens na conversa;
 * - utilizar a memória existente;
 * - devolver uma resposta textual.
 *
 * O processamento pode utilizar:
 * - JarvisBrain no modo offline;
 * - provedor online através do JarvisAIManager.
 *
 * Não controla diretamente o microfone.
 */
public class JarvisConversationEngine {

    private final Context context;

    private final JarvisSessionManager sessionManager;
    private final JarvisContextManager contextManager;
    private final JarvisAIManager aiManager;

    public JarvisConversationEngine(
            Context context) {

        if (context == null) {
            throw new IllegalArgumentException(
                    "Context não pode ser nulo."
            );
        }

        this.context =
                context.getApplicationContext();

        this.sessionManager =
                new JarvisSessionManager(
                        this.context
                );

        this.contextManager =
                sessionManager.getContextManager();

        this.aiManager =
                new JarvisAIManager(
                        this.context
                );
    }

    public JarvisConversationEngine(
            Context context,
            JarvisSessionManager sessionManager) {

        if (context == null) {
            throw new IllegalArgumentException(
                    "Context não pode ser nulo."
            );
        }

        if (sessionManager == null) {
            throw new IllegalArgumentException(
                    "JarvisSessionManager não pode ser nulo."
            );
        }

        this.context =
                context.getApplicationContext();

        this.sessionManager =
                sessionManager;

        this.contextManager =
                sessionManager.getContextManager();

        this.aiManager =
                new JarvisAIManager(
                        this.context
                );
    }

    public JarvisSessionManager getSessionManager() {
        return sessionManager;
    }

    public JarvisContextManager getContextManager() {
        return contextManager;
    }

    public JarvisAIManager getAIManager() {
        return aiManager;
    }

    public JarvisBrain getBrain() {
        return aiManager.getOfflineBrain();
    }

    public synchronized String process(
            String userInput) {

        if (userInput == null
                || userInput.trim().isEmpty()) {

            return "";
        }

        if (!sessionManager.isActive()) {

            return "JARVIS está em modo sono. "
                    + "Diga \"Jarvis\" para ativá-lo.";
        }

        if (sessionManager.containsEndPhrase(
                userInput)) {

            sessionManager.sleep();

            return "Sessão encerrada. "
                    + "JARVIS entrando em modo de espera.";
        }

        sessionManager.processCommand(
                userInput
        );

        /*
         * buildFullContext exige a mensagem atual.
         */
        String contextText =
                contextManager
                        .buildFullContext(
                                userInput
                        );

        String response =
                aiManager.process(
                        userInput,
                        contextText
                );

        if (response == null
                || response.trim().isEmpty()) {

            response =
                    "Não consegui gerar uma resposta "
                    + "para esse comando.";
        }

        sessionManager.registerJarvisResponse(
                response
        );

        return response;
    }

    public synchronized ConversationResult
    processCommand(String userInput) {

        if (userInput == null
                || userInput.trim().isEmpty()) {

            return new ConversationResult(
                    "",
                    sessionManager.isActive()
            );
        }

        if (!sessionManager.isActive()) {

            return new ConversationResult(
                    "JARVIS está em modo sono. "
                    + "Diga \"Jarvis\" para ativá-lo.",
                    false
            );
        }

        if (sessionManager.containsEndPhrase(
                userInput)) {

            sessionManager.sleep();

            String response =
                    "Sessão encerrada. "
                    + "JARVIS entrando em modo de espera.";

            return new ConversationResult(
                    response,
                    false
            );
        }

        sessionManager.processCommand(
                userInput
        );

        /*
         * buildFullContext exige a mensagem atual.
         */
        String contextText =
                contextManager
                        .buildFullContext(
                                userInput
                        );

        String response =
                aiManager.process(
                        userInput,
                        contextText
                );

        if (response == null
                || response.trim().isEmpty()) {

            response =
                    "Não consegui gerar uma resposta "
                    + "para esse comando.";
        }

        sessionManager.registerJarvisResponse(
                response
        );

        return new ConversationResult(
                response,
                sessionManager.isActive()
        );
    }

    public synchronized boolean activateSession() {
        return sessionManager.activate();
    }

    public synchronized boolean endSession() {
        return sessionManager.sleep();
    }

    public synchronized boolean isActive() {
        return sessionManager.isActive();
    }

    public synchronized String getFullContext() {

        return contextManager
                .buildFullContext(
                        contextManager
                                .getLastUserMessage()
                );
    }

    public synchronized void clearConversation() {
        contextManager.clearConversation();
    }

    public synchronized JarvisMemory getMemory() {
        return contextManager.getMemory();
    }

    public synchronized void remember(
            String text) {

        if (text == null
                || text.trim().isEmpty()) {

            return;
        }

        contextManager.remember(
                text
        );
    }

    public synchronized void forget(
            String text) {

        if (text == null
                || text.trim().isEmpty()) {

            return;
        }

        contextManager.forget(
                text
        );
    }

    public synchronized void enableOfflineMode() {
        aiManager.enableOfflineMode();
    }

    public synchronized void enableOnlineMode() {
        aiManager.enableOnlineMode();
    }

    public synchronized void disableOnlineMode() {
        aiManager.disableOnlineMode();
    }

    public synchronized boolean isOfflineMode() {
        return aiManager.isOfflineMode();
    }

    public synchronized boolean isOnlineMode() {
        return aiManager.isOnlineMode();
    }

    public synchronized String getAIModeName() {
        return aiManager.getModeName();
    }

    public synchronized String getAIStatus() {
        return aiManager.getStatus();
    }

    public synchronized JarvisAIManager
    getAIManagerInstance() {

        return aiManager;
    }

    public static class ConversationResult {

        private final String response;
        private final boolean sessionActive;

        public ConversationResult(
                String response,
                boolean sessionActive) {

            this.response = response;
            this.sessionActive =
                    sessionActive;
        }

        public String getResponse() {
            return response;
        }

        public boolean isSessionActive() {
            return sessionActive;
        }
    }
}
