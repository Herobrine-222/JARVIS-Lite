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

    /**
     * Construtor padrão.
     *
     * Mantido para compatibilidade com outras partes
     * do projeto.
     *
     * Este construtor cria uma nova sessão própria.
     */
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

    /**
     * Construtor para utilizar uma sessão
     * compartilhada com outros componentes
     * do JARVIS.
     *
     * Esse construtor é importante para que o
     * JarvisWakeWordManager e o
     * JarvisConversationEngine enxerguem
     * exatamente o mesmo estado:
     *
     * JARVIS dormindo
     *       ↓
     * palavra "Jarvis"
     *       ↓
     * JARVIS ativo
     *       ↓
     * comandos
     *       ↓
     * "encerrar sessão"
     *       ↓
     * JARVIS dormindo
     */
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

    /**
     * Retorna o gerenciador de sessão.
     */
    public JarvisSessionManager getSessionManager() {

        return sessionManager;
    }

    /**
     * Retorna o contexto da conversa.
     */
    public JarvisContextManager getContextManager() {

        return contextManager;
    }

    /**
     * Retorna o gerenciador de IA.
     */
    public JarvisAIManager getAIManager() {

        return aiManager;
    }

    /**
     * Retorna o cérebro offline.
     */
    public JarvisBrain getBrain() {

        return aiManager.getOfflineBrain();
    }

    /**
     * Processa uma mensagem recebida.
     *
     * O processamento acontece somente se
     * houver uma sessão ativa.
     */
    public synchronized String process(
            String userInput) {

        if (userInput == null
                || userInput.trim().isEmpty()) {

            return "";
        }

        /*
         * O usuário precisa estar em uma sessão
         * ativa para enviar comandos.
         */
        if (!sessionManager.isActive()) {

            return "JARVIS está em modo sono. "
                    + "Diga \"Jarvis\" para ativá-lo.";
        }

        /*
         * Verifica se o usuário pediu para
         * encerrar a sessão.
         */
        if (sessionManager.containsEndPhrase(
                userInput)) {

            sessionManager.sleep();

            return "Sessão encerrada. "
                    + "JARVIS entrando em modo de espera.";
        }

        /*
         * Registra o comando na sessão.
         */
        sessionManager.processCommand(
                userInput
        );

        /*
         * Monta o contexto completo.
         *
         * Esse contexto pode conter:
         * - conversa atual;
         * - memória;
         * - informações do aparelho.
         */
        String contextText =
                contextManager
                        .buildFullContext();

        /*
         * Envia o comando para o gerenciador
         * central de IA.
         *
         * O JarvisAIManager decide se deve
         * utilizar OFFLINE ou ONLINE.
         */
        String response =
                aiManager.process(
                        userInput,
                        contextText
                );

        /*
         * Garante que sempre exista uma resposta.
         */
        if (response == null
                || response.trim().isEmpty()) {

            response =
                    "Não consegui gerar uma resposta "
                    + "para esse comando.";
        }

        /*
         * Registra a resposta do JARVIS.
         */
        sessionManager.registerJarvisResponse(
                response
        );

        return response;
    }

    /**
     * Processa uma mensagem e permite que
     * o chamador saiba se a sessão continua ativa.
     */
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

        /*
         * Encerramento da sessão.
         */
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

        /*
         * Registra o comando.
         */
        sessionManager.processCommand(
                userInput
        );

        /*
         * Obtém o contexto completo.
         */
        String contextText =
                contextManager
                        .buildFullContext();

        /*
         * Processa através do gerenciador
         * central de IA.
         */
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

        /*
         * Registra a resposta.
         */
        sessionManager.registerJarvisResponse(
                response
        );

        return new ConversationResult(
                response,
                sessionManager.isActive()
        );
    }

    /**
     * Ativa uma nova sessão.
     */
    public synchronized boolean activateSession() {

        return sessionManager.activate();
    }

    /**
     * Encerra a sessão atual.
     */
    public synchronized boolean endSession() {

        return sessionManager.sleep();
    }

    /**
     * Verifica se a sessão está ativa.
     */
    public synchronized boolean isActive() {

        return sessionManager.isActive();
    }

    /**
     * Retorna todo o contexto disponível.
     */
    public synchronized String getFullContext() {

        return contextManager
                .buildFullContext();
    }

    /**
     * Limpa somente a conversa temporária
     * da sessão atual.
     *
     * A memória permanente não é apagada.
     */
    public synchronized void clearConversation() {

        contextManager.clearConversation();
    }

    /**
     * Retorna a memória permanente.
     */
    public synchronized JarvisMemory getMemory() {

        return contextManager.getMemory();
    }

    /**
     * Guarda uma informação na memória.
     */
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

    /**
     * Remove uma informação da memória.
     */
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

    /**
     * Ativa o modo offline.
     */
    public synchronized void enableOfflineMode() {

        aiManager.enableOfflineMode();
    }

    /**
     * Ativa o modo online.
     *
     * A conexão real depende de um provedor
     * configurado no JarvisOnlineManager.
     */
    public synchronized void enableOnlineMode() {

        aiManager.enableOnlineMode();
    }

    /**
     * Desativa o modo online e retorna
     * ao modo offline.
     */
    public synchronized void disableOnlineMode() {

        aiManager.disableOnlineMode();
    }

    /**
     * Verifica se está no modo offline.
     */
    public synchronized boolean isOfflineMode() {

        return aiManager.isOfflineMode();
    }

    /**
     * Verifica se está no modo online.
     */
    public synchronized boolean isOnlineMode() {

        return aiManager.isOnlineMode();
    }

    /**
     * Retorna o nome do modo atual.
     */
    public synchronized String getAIModeName() {

        return aiManager.getModeName();
    }

    /**
     * Retorna o status da IA.
     */
    public synchronized String getAIStatus() {

        return aiManager.getStatus();
    }

    /**
     * Retorna o gerenciador de IA.
     */
    public synchronized JarvisAIManager
    getAIManagerInstance() {

        return aiManager;
    }

    /**
     * Resultado de uma conversa.
     *
     * Contém:
     * - resposta gerada;
     * - informação sobre a continuidade
     *   da sessão.
     */
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
