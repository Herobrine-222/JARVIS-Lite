
package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Nucleo coordenador dos especialistas do JARVIS Lite.
 *
 * Responsabilidades:
 * - reutilizar a memoria e o ContextManager existentes;
 * - avaliar a politica de seguranca antes do encaminhamento;
 * - classificar mensagens com o motor de raciocinio existente;
 * - despachar para especialistas registrados;
 * - isolar falhas e disponibilizar diagnosticos;
 * - evitar duplicacao de memoria e armazenamento de mensagens
 *   privadas nos diagnosticos.
 *
 * Este nucleo nao acessa a internet, nao executa comandos do
 * sistema e nao substitui o fluxo principal do aplicativo sozinho.
 */
public final class JarvisVisionCore {

    private static final int MAX_INPUT_LENGTH = 2000;
    private static final int MAX_MODULES = 32;
    private static final int MEMORY_RESULT_LIMIT = 5;

    private static final String SAFETY_UNAVAILABLE_MESSAGE =
            "Nao foi possivel verificar a seguranca desta "
            + "solicitacao. Por precaucao, nao vou encaminha-la "
            + "aos modulos especialistas neste momento.";

    private static final String INVALID_ANALYSIS_MESSAGE =
            "Nao foi possivel analisar a solicitacao.";

    private final JarvisReasoningEngine reasoningEngine;
    private final JarvisMemory memory;
    private final JarvisContextManager contextManager;

    private final List<JarvisVisionModule> modules =
            new ArrayList<>();

    /*
     * Diagnosticos locais desta instancia.
     * Nenhum campo guarda o texto da mensagem do usuario.
     */
    private String lastProcessedModule = "";
    private String lastFailedModule = "";
    private String lastModuleError = "";
    private int moduleFailureCount = 0;

    /**
     * Construtor principal: reutiliza o ContextManager compartilhado.
     */
    public JarvisVisionCore(JarvisContextManager contextManager) {
        if (contextManager == null) {
            throw new IllegalArgumentException(
                    "O ContextManager nao pode ser nulo."
            );
        }

        JarvisMemory existingMemory = contextManager.getMemory();

        if (existingMemory == null) {
            throw new IllegalArgumentException(
                    "O ContextManager precisa fornecer a memoria existente."
            );
        }

        this.contextManager = contextManager;
        this.memory = existingMemory;
        this.reasoningEngine = new JarvisReasoningEngine();

        registerDefaultModules();
    }

    /**
     * Construtor de compatibilidade para projetos que fornecem
     * somente a memoria ja existente.
     */
    public JarvisVisionCore(JarvisMemory memory) {
        if (memory == null) {
            throw new IllegalArgumentException(
                    "A memoria nao pode ser nula."
            );
        }

        this.memory = memory;
        this.contextManager = null;
        this.reasoningEngine = new JarvisReasoningEngine();

        registerDefaultModules();
    }

    /**
     * Registra os especialistas sem apagar os que ja existem.
     *
     * O VISION e registrado antes do FRIDAY para que possa
     * tratar as intencoes matematicas ou desconhecidas que
     * suportar, sem interceptar as demais intencoes.
     */
    private void registerDefaultModules() {
        registerModule(new JarvisEdithModule());
        registerModule(new JarvisKarenModule());
        registerModule(new JarvisUModule());

        if (contextManager != null) {
            registerModule(new JarvisDumEModule());
        }

        registerModule(new JarvisVisionReasoningModule());
        registerModule(new JarvisFridayModule());
    }

    /**
     * Classifica uma entrada.
     *
     * Este metodo nao aplica a politica de seguranca por si so.
     * Para encaminhamento protegido, use processModules().
     */
    public JarvisReasoningEngine.Analysis analyze(String input) {
        if (input == null || input.trim().isEmpty()
                || input.length() > MAX_INPUT_LENGTH) {
            return null;
        }

        try {
            return reasoningEngine.analyze(input);
        } catch (RuntimeException error) {
            recordFailure("JarvisReasoningEngine", error);
            return null;
        }
    }

    /**
     * Recomenda um especialista sem executar o pedido.
     */
    public JarvisReasoningEngine.Specialist recommendSpecialist(
            String input
    ) {
        if (input == null || input.trim().isEmpty()
                || input.length() > MAX_INPUT_LENGTH) {
            return null;
        }

        try {
            return reasoningEngine.recommendSpecialist(input);
        } catch (RuntimeException error) {
            recordFailure("JarvisReasoningEngine", error);
            return null;
        }
    }

    /**
     * Consulta a base de conhecimento offline existente.
     */
    public String answerFromExistingKnowledgeBase(String input) {
        if (input == null || input.trim().isEmpty()
                || input.length() > MAX_INPUT_LENGTH) {
            return null;
        }

        try {
            return JarvisKnowledgeBase.answer(input);
        } catch (RuntimeException error) {
            recordFailure("JarvisKnowledgeBase", error);
            return null;
        }
    }

    /**
     * Registra um modulo adicional.
     *
     * Recusa nulos, nomes vazios, duplicatas e excesso de modulos.
     * O metodo nao permite sobrescrever um modulo ja registrado.
     */
    public synchronized boolean registerModule(
            JarvisVisionModule module
    ) {
        if (module == null || modules.size() >= MAX_MODULES) {
            return false;
        }

        final String name;

        try {
            name = module.getName();
        } catch (RuntimeException error) {
            recordFailure("ModuleRegistration", error);
            return false;
        }

        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        String normalizedName = name.trim();

        for (JarvisVisionModule registered : modules) {
            String registeredName = getSafeModuleName(registered);

            if (registeredName.equalsIgnoreCase(normalizedName)) {
                return false;
            }
        }

        modules.add(module);
        return true;
    }

    /**
     * Retorna uma copia nao modificavel dos nomes registrados.
     */
    public synchronized List<String> getRegisteredModuleNames() {
        List<String> names = new ArrayList<>();

        for (JarvisVisionModule module : modules) {
            names.add(getSafeModuleName(module));
        }

        return Collections.unmodifiableList(names);
    }

    /**
     * Fluxo central de processamento:
     *
     * 1. valida a entrada;
     * 2. aplica a politica de seguranca;
     * 3. classifica a intencao;
     * 4. tenta os especialistas na ordem registrada;
     * 5. isola falhas individuais;
     * 6. retorna null se nenhum especialista responder.
     *
     * A protecao so vale para chamadas que passam por este metodo.
     */
    public synchronized String processModules(String input) {
        resetLastAttemptDiagnostics();

        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        if (input.length() > MAX_INPUT_LENGTH) {
            lastProcessedModule = "InputValidation";
            return "A mensagem e muito longa para ser processada "
                    + "com seguranca. Resuma o pedido e tente novamente.";
        }

        /*
         * CAMADA 1: politica de seguranca.
         * Em caso de falha, o encaminhamento e interrompido.
         */
        final JarvisSafetyPolicy.Assessment assessment;

        try {
            assessment = JarvisSafetyPolicy.evaluate(input);
        } catch (RuntimeException error) {
            recordFailure("JarvisSafetyPolicy", error);
            lastProcessedModule = "JarvisSafetyPolicy";
            return SAFETY_UNAVAILABLE_MESSAGE;
        }

        if (assessment == null || assessment.getDecision() == null) {
            recordFailure(
                    "JarvisSafetyPolicy",
                    "INVALID_ASSESSMENT"
            );

            lastProcessedModule = "JarvisSafetyPolicy";
            return SAFETY_UNAVAILABLE_MESSAGE;
        }

        if (assessment.getDecision()
                != JarvisSafetyPolicy.Decision.ALLOW) {

            lastProcessedModule = "JarvisSafetyPolicy";

            String policyMessage = assessment.getUserMessage();

            if (policyMessage == null
                    || policyMessage.trim().isEmpty()) {
                return SAFETY_UNAVAILABLE_MESSAGE;
            }

            return policyMessage.trim();
        }

        /*
         * CAMADA 2: classificacao.
         */
        final JarvisReasoningEngine.Analysis analysis;

        try {
            analysis = reasoningEngine.analyze(input);
        } catch (RuntimeException error) {
            recordFailure("JarvisReasoningEngine", error);
            return INVALID_ANALYSIS_MESSAGE;
        }

        if (analysis == null || analysis.getIntent() == null) {
            recordFailure(
                    "JarvisReasoningEngine",
                    "NULL_OR_INVALID_ANALYSIS"
            );

            return INVALID_ANALYSIS_MESSAGE;
        }

        /*
         * CAMADA 3: despacho.
         * Cada modulo pode recusar o pedido devolvendo null ou vazio.
         */
        for (JarvisVisionModule module : modules) {
            String moduleName = getSafeModuleName(module);

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
                    lastProcessedModule = moduleName;
                    return response.trim();
                }

            } catch (RuntimeException error) {
                recordFailure(moduleName, error);

                /*
                 * Uma falha isolada nao impede a tentativa
                 * de outros especialistas registrados.
                 */
            }
        }

        return null;
    }

    /**
     * Limpa os diagnosticos da tentativa atual.
     * O contador acumulado permanece nesta instancia.
     */
    private void resetLastAttemptDiagnostics() {
        lastProcessedModule = "";
        lastFailedModule = "";
        lastModuleError = "";
    }

    private void recordFailure(
            String moduleName,
            RuntimeException error
    ) {
        lastFailedModule = safeDiagnosticName(moduleName);
        lastModuleError = error == null
                ? "UNKNOWN_ERROR"
                : error.getClass().getSimpleName();

        incrementFailureCount();
    }

    private void recordFailure(
            String moduleName,
            String errorCode
    ) {
        lastFailedModule = safeDiagnosticName(moduleName);

        lastModuleError =
                errorCode == null || errorCode.trim().isEmpty()
                        ? "UNKNOWN_ERROR"
                        : errorCode.trim();

        incrementFailureCount();
    }

    private synchronized void incrementFailureCount() {
        if (moduleFailureCount < Integer.MAX_VALUE) {
            moduleFailureCount++;
        }
    }

    private String safeDiagnosticName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "desconhecido";
        }

        return name.trim();
    }

    /**
     * Consulta o nome de um modulo sem permitir que uma falha
     * na propria consulta interrompa o coordenador.
     */
    private String getSafeModuleName(JarvisVisionModule module) {
        if (module == null) {
            return "desconhecido";
        }

        try {
            String name = module.getName();

            if (name != null && !name.trim().isEmpty()) {
                return name.trim();
            }
        } catch (RuntimeException error) {
            // Usa o nome da classe como alternativa.
        }

        return module.getClass().getSimpleName();
    }

    public synchronized String getLastProcessedModule() {
        return lastProcessedModule;
    }

    public synchronized String getLastFailedModule() {
        return lastFailedModule;
    }

    public synchronized String getLastModuleError() {
        return lastModuleError;
    }

    public synchronized int getModuleFailureCount() {
        return moduleFailureCount;
    }

    /**
     * Recupera contexto relevante da memoria ja existente.
     */
    public String getRelevantMemory(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }

        try {
            String result;

            if (contextManager != null) {
                result = contextManager.getRelevantMemory(query);
            } else {
                result = memory.relevantContext(
                        query,
                        MEMORY_RESULT_LIMIT
                );
            }

            return result == null ? "" : result;

        } catch (RuntimeException error) {
            recordMemoryFailure(error);
            return "";
        }
    }

    /**
     * Pesquisa a memoria existente sem criar outro armazenamento.
     */
    public List<String> searchMemory(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            List<String> results;

            if (contextManager != null) {
                results = contextManager.searchMemory(query);
            } else {
                results = memory.search(
                        query,
                        MEMORY_RESULT_LIMIT
                );
            }

            if (results == null) {
                return new ArrayList<>();
            }

            return new ArrayList<>(results);

        } catch (RuntimeException error) {
            recordMemoryFailure(error);
            return new ArrayList<>();
        }
    }

    /**
     * Guarda texto na memoria ja existente.
     */
    public boolean remember(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        try {
            return memory.remember(text);
        } catch (RuntimeException error) {
            recordMemoryFailure(error);
            return false;
        }
    }

    /**
     * Guarda texto e categoria na memoria ja existente.
     */
    public boolean remember(String text, String category) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        try {
            return memory.remember(text, category);
        } catch (RuntimeException error) {
            recordMemoryFailure(error);
            return false;
        }
    }

    /**
     * Remove registros usando a implementacao de memoria existente.
     */
    public int forget(String query) {
        if (query == null || query.trim().isEmpty()) {
            return 0;
        }

        try {
            return memory.forget(query);
        } catch (RuntimeException error) {
            recordMemoryFailure(error);
            return 0;
        }
    }

    private synchronized void recordMemoryFailure(
            RuntimeException error
    ) {
        lastFailedModule = "JarvisMemory";
        lastModuleError = error == null
                ? "UNKNOWN_ERROR"
                : error.getClass().getSimpleName();

        incrementFailureCount();
    }

    /**
     * Retorna a memoria compartilhada desta instancia.
     * Nunca cria uma nova base SQLite.
     */
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
