package com.jarvis.lite;

import android.content.Context;

/**
 * Motor de conversação do JARVIS.
 *
 * Responsável por:
 * - receber comandos do usuário;
 * - verificar o estado da sessão;
 * - enviar o contexto para o JarvisBrain;
 * - registrar mensagens na conversa;
 * - utilizar a memória existente;
 * - devolver uma resposta textual.
 *
 * Não acessa a Internet.
 * Não controla diretamente o microfone.
 */
public class JarvisConversationEngine {

    private final Context context;

    private final JarvisSessionManager sessionManager;
    private final JarvisContextManager contextManager;
    private final JarvisBrain brain;

    public JarvisConversationEngine(
            Context context) {

        this.context =
                context.getApplicationContext();

        this.sessionManager =
                new JarvisSessionManager(
                        this.context
                );

        this.contextManager =
                sessionManager.getContextManager();

        this.brain =
                new JarvisBrain(
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
     * Retorna o cérebro local.
     */
    public JarvisBrain getBrain() {

        return brain;
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
         * Monta o contexto atual.
         */
        String memoryContext =
                contextManager
                        .buildFullContext();

        /*
         * Envia o comando para o cérebro local.
         */
        String response =
                brain.process(
                        userInput,
                        memoryContext
                );

        /*
         * Garante que sempre exista uma resposta.
         */
        if (response == null
                || response.trim().isEmpty()) {

            response =
                    "Não consegui gerar uma resposta "
                    + "local para esse comando.";
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
         * Obtém o contexto atual.
         */
        String contextText =
                contextManager
                        .buildFullContext();

        /*
         * Processa no cérebro local.
         */
        String response =
                brain.process(
                        userInput,
                        contextText
                );

        if (response == null
                || response.trim().isEmpty()) {

            response =
                    "Não consegui gerar uma resposta "
                    + "local para esse comando.";
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
