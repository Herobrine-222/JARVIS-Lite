
package com.jarvis.lite;

import android.content.Context;

/**
 * Coordenador de alto nivel do JARVIS Lite.
 *
 * Reutiliza os especialistas existentes.
 * Nao substitui MainActivity nem duplica o VisionCore.
 * Nao executa comandos do sistema nem acessa a Internet.
 */
public final class JarvisCore {

    private static final int MAX_INPUT_LENGTH = 2000;

    private final JarvisContextManager contextManager;
    private final JarvisMemoryCore memoryCore;
    private final JarvisVisionCore visionCore;

    /**
     * Inicializacao independente, para testes e usos novos.
     */
    public JarvisCore(Context context) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "O contexto Android nao pode ser nulo."
            );
        }

        contextManager = new JarvisContextManager(
                context.getApplicationContext()
        );

        memoryCore = new JarvisMemoryCore(contextManager);
        visionCore = new JarvisVisionCore(contextManager);
    }

    /**
     * Permite reutilizar os componentes ja criados pelo aplicativo.
     */
    public JarvisCore(
            JarvisContextManager contextManager,
            JarvisMemoryCore memoryCore,
            JarvisVisionCore visionCore
    ) {
        if (contextManager == null
                || memoryCore == null
                || visionCore == null) {
            throw new IllegalArgumentException(
                    "Os tres componentes sao obrigatorios."
            );
        }

        this.contextManager = contextManager;
        this.memoryCore = memoryCore;
        this.visionCore = visionCore;
    }

    /**
     * Encaminha a solicitacao aos especialistas existentes.
     * Nao grava automaticamente a conversa.
     */
    public synchronized String process(String input) {
        if (!isValidInput(input)) {
            return null;
        }

        try {
            return visionCore.processModules(input.trim());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /**
     * Variante que registra a conversa recente no ContextManager.
     * Isso nao equivale a criar uma memoria pessoal permanente.
     */
    public synchronized String processAndRecord(String input) {
        if (!isValidInput(input)) {
            return null;
        }

        String message = input.trim();

        contextManager.addUserMessage(message);

        String response = process(message);

        if (response != null && !response.trim().isEmpty()) {
            contextManager.addJarvisMessage(response.trim());
        }

        return response;
    }

    /**
     * Classifica a mensagem sem encaminha-la para execucao.
     */
    public JarvisReasoningEngine.Analysis analyze(String input) {
        if (!isValidInput(input)) {
            return null;
        }

        return visionCore.analyze(input);
    }

    /**
     * Recupera memorias relevantes localmente.
     */
    public String getContextFor(String query) {
        return memoryCore.buildContext(query);
    }

    public JarvisContextManager getContextManager() {
        return contextManager;
    }

    public JarvisMemoryCore getMemoryCore() {
        return memoryCore;
    }

    public JarvisVisionCore getVisionCore() {
        return visionCore;
    }

    public String getStatusSummary() {
        return "JarvisCore inicializado; processamento local; "
                + "especialistas registrados: "
                + visionCore.getRegisteredModuleNames().size()
                + ".";
    }

    private boolean isValidInput(String input) {
        return input != null
                && !input.trim().isEmpty()
                && input.length() <= MAX_INPUT_LENGTH;
    }
}
