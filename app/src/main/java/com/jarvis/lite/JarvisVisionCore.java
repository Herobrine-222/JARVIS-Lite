
package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Coordenador inicial do VISION.
 *
 * Reutiliza os motores, a memória e a base de conhecimento
 * que já existem no JARVIS Lite.
 */
public final class JarvisVisionCore {

    private final JarvisReasoningEngine reasoningEngine;
    private final JarvisMemory memory;
    private final JarvisContextManager contextManager;

    private final List<JarvisVisionModule> modules =
            new ArrayList<>();

    public JarvisVisionCore(
            JarvisContextManager contextManager
    ) {
        if (contextManager == null) {
            throw new IllegalArgumentException(
                    "O ContextManager não pode ser nulo."
            );
        }

        this.contextManager = contextManager;
        this.memory = contextManager.getMemory();
        this.reasoningEngine = new JarvisReasoningEngine();
    }

    public JarvisVisionCore(JarvisMemory memory) {
        if (memory == null) {
            throw new IllegalArgumentException(
                    "A memória não pode ser nula."
            );
        }

        this.memory = memory;
        this.contextManager = null;
        this.reasoningEngine = new JarvisReasoningEngine();
    }

    public JarvisReasoningEngine.Analysis analyze(
            String input
    ) {
        return reasoningEngine.analyze(input);
    }

    public JarvisReasoningEngine.Specialist recommendSpecialist(
            String input
    ) {
        return reasoningEngine.recommendSpecialist(input);
    }

    /**
     * Consulta a base de conhecimento existente.
     */
    public String answerFromExistingKnowledgeBase(
            String input
    ) {
        return JarvisKnowledgeBase.answer(input);
    }

    public synchronized boolean registerModule(
            JarvisVisionModule module
    ) {
        if (module == null) {
            return false;
        }

        String name = module.getName();

        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        for (JarvisVisionModule registered : modules) {
            if (registered.getName() != null
                    && registered.getName().equalsIgnoreCase(
                            name.trim()
                    )) {
                return false;
            }
        }

        modules.add(module);
        return true;
    }

    public synchronized List<String> getRegisteredModuleNames() {
        List<String> names = new ArrayList<>();

        for (JarvisVisionModule module : modules) {
            names.add(module.getName());
        }

        return Collections.unmodifiableList(names);
    }

    /**
     * Encaminha a mensagem aos módulos registrados.
     * Retorna null quando nenhum deles consegue responder.
     */
    public synchronized String processModules(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        JarvisReasoningEngine.Analysis analysis =
                reasoningEngine.analyze(input);

        for (JarvisVisionModule module : modules) {
            try {
                if (!module.supports(analysis)) {
                    continue;
                }

                String response = module.process(
                        input,
                        analysis,
                        this
                );

                if (response != null
                        && !response.trim().isEmpty()) {
                    return response.trim();
                }
            } catch (RuntimeException ignored) {
                // Um módulo com falha não deve derrubar o JARVIS.
            }
        }

        return null;
    }

    public String getRelevantMemory(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }

        if (contextManager != null) {
            return contextManager.getRelevantMemory(query);
        }

        return memory.relevantContext(query, 5);
    }

    public List<String> searchMemory(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        if (contextManager != null) {
            return contextManager.searchMemory(query);
        }

        return memory.search(query, 5);
    }

    public boolean remember(String text) {
        return memory.remember(text);
    }

    public boolean remember(
            String text,
            String category
    ) {
        return memory.remember(text, category);
    }

    public int forget(String query) {
        return memory.forget(query);
    }

    public JarvisMemory getMemory() {
        return memory;
    }

    public JarvisContextManager getContextManager() {
        return contextManager;
    }

    public JarvisReasoningEngine getReasoningEngine() {
        return reasoningEngine;
    }
}
