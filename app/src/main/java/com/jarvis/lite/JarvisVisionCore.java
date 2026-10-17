
package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Coordenador dos modulos especialistas do VISION.
 *
 * Reutiliza o motor de raciocinio, a memoria persistente
 * e a base de conhecimento ja existentes no JARVIS Lite.
 *
 * A politica de seguranca e executada antes do encaminhamento
 * de mensagens aos modulos especialistas.
 *
 * Este nucleo nao cria uma segunda base de memoria,
 * nao acessa a internet e nao executa comandos do sistema.
 */
public final class JarvisVisionCore {

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
     * Diagnostico da ultima tentativa de processamento.
     *
     * O diagnostico nao armazena o texto privado recebido.
     */
    private String lastProcessedModule = "";
    private String lastFailedModule = "";
    private String lastModuleError = "";
    private int moduleFailureCount = 0;

    /**
     * Construtor principal.
     *
     * Recebe o gerenciador compartilhado pelo aplicativo,
     * permitindo reutilizar a memoria e as ferramentas existentes.
     */
    public JarvisVisionCore(
            JarvisContextManager contextManager
    ) {
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
     * Construtor de compatibilidade.
     *
     * Permite usar o nucleo somente com a memoria existente.
     * Sem ContextManager, os modulos que dependem das ferramentas
     * do aparelho nao sao registrados.
     */
    public JarvisVisionCore(
            JarvisMemory memory
    ) {
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
     * Registra os especialistas ja existentes no projeto.
     *
     * A verificacao de nomes duplicados fica centralizada
     * no metodo registerModule().
     */
    private void registerDefaultModules() {

        registerModule(new JarvisEdithModule());

        registerModule(new JarvisKarenModule());

        registerModule(new JarvisUModule());

        /*
         * O DUM-E depende das ferramentas disponibilizadas
         * pelo ContextManager.
         */
        if (contextManager != null) {
            registerModule(new JarvisDumEModule());
        }

        /*
         * FRIDAY continua disponivel para as funcoes
         * conversacionais e de memoria que ja implementa.
         */
        registerModule(new JarvisFridayModule());
    }

    /**
     * Analisa diretamente uma entrada com o motor existente.
     *
     * Este metodo nao substitui a verificacao de seguranca
     * realizada por processModules().
     */
    public JarvisReasoningEngine.Analysis analyze(
            String input
    ) {
        return reasoningEngine.analyze(input);
    }

    /**
     * Recomenda um especialista conforme o motor existente.
     *
     * Este metodo apenas consulta o classificador.
     * Para processar uma entrada passando pela politica
     * de seguranca, utilize processModules().
     */
    public JarvisReasoningEngine.Specialist recommendSpecialist(
            String input
    ) {
        return reasoningEngine.recommendSpecialist(input);
    }

    /**
     * Consulta a base de conhecimento offline existente.
     *
     * Retorna null quando nao existe resposta cadastrada.
     */
    public String answerFromExistingKnowledgeBase(
            String input
    ) {
        return JarvisKnowledgeBase.answer(input);
    }

    /**
     * Registra um modulo adicional.
     *
     * Nao permite modulos nulos, nomes vazios nem dois
     * modulos com o mesmo nome, ignorando maiusculas
     * e minusculas.
     */
    public synchronized boolean registerModule(
            JarvisVisionModule module
    ) {
        if (module == null) {
            return false;
        }

        final String name;

        try {
            name = module.getName();
        } catch (RuntimeException error) {
            return false;
        }

        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        String normalizedName = name.trim();

        for (JarvisVisionModule registered : modules) {
            String registeredName;

            try {
                registeredName = registered.getName();
            } catch (RuntimeException error) {
                continue;
            }

            if (registeredName != null
                    && registeredName.trim().equalsIgnoreCase(
                            normalizedName
                    )) {
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
     * Processa uma entrada usando a seguinte ordem:
     *
     * 1. Valida se a entrada esta vazia.
     * 2. Avalia a entrada com JarvisSafetyPolicy.
     * 3. Interrompe o encaminhamento se a avaliacao nao permitir.
     * 4. Analisa a entrada com JarvisReasoningEngine.
     * 5. Tenta os modulos registrados em ordem.
     * 6. Isola falhas individuais dos modulos.
     *
     * IMPORTANTE:
     * Esta protecao aplica-se as entradas que passam por este
     * metodo. Ela nao protege automaticamente outros caminhos
     * do aplicativo que nao utilizem JarvisVisionCore.
     */
    public synchronized String processModules(
            String input
    ) {
        resetLastAttemptDiagnostics();

        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        /*
         * CAMADA 1: politica de seguranca.
         *
         * Em caso de excecao ou avaliacao invalida, o nucleo
         * interrompe o processamento em vez de encaminhar
         * a entrada aos especialistas sem verificacao.
         */
        final JarvisSafetyPolicy.Assessment assessment;

        try {
            assessment = JarvisSafetyPolicy.evaluate(input);
        } catch (RuntimeException error) {
            recordFailure(
                    "JarvisSafetyPolicy",
                    error
            );

            lastProcessedModule = "JarvisSafetyPolicy";

            return SAFETY_UNAVAILABLE_MESSAGE;
        }

        if (assessment == null
                || assessment.getDecision() == null) {

            recordFailure(
                    "JarvisSafetyPolicy",
                    "INVALID_ASSESSMENT"
            );

            lastProcessedModule = "JarvisSafetyPolicy";

            return SAFETY_UNAVAILABLE_MESSAGE;
        }

        /*
         * ALLOW permite continuar.
         *
         * REVIEW e REFUSE interrompem o encaminhamento.
         * A politica pode devolver uma resposta segura ao usuario.
         */
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
         * CAMADA 2: motor de raciocinio.
         *
         * A politica permitiu que a entrada prossiga.
         * Ainda assim, uma falha no motor deve ser isolada.
         */
        final JarvisReasoningEngine.Analysis analysis;

        try {
            analysis = reasoningEngine.analyze(input);
        } catch (RuntimeException error) {
            recordFailure(
                    "JarvisReasoningEngine",
                    error
            );

            return null;
        }

        if (analysis == null) {
            recordFailure(
                    "JarvisReasoningEngine",
                    "NULL_ANALYSIS"
            );

            return null;
        }

        /*
         * CAMADA 3: modulos especialistas.
         *
         * A ordem de registro continua sendo respeitada.
         * Um modulo que nao consegue responder pode devolver
         * null ou texto vazio para permitir outras tentativas.
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

                /*
                 * Guarda apenas o nome do modulo e o tipo
                 * da excecao, sem salvar o conteudo da entrada.
                 */
                recordFailure(moduleName, error);

                /*
                 * Uma falha isolada nao impede que outro
                 * modulo registrado tente responder.
                 */
            }
        }

        return null;
    }

    /**
     * Limpa apenas os diagnosticos da tentativa atual.
     *
     * O contador total de falhas permanece acumulado
     * durante a vida desta instancia do nucleo.
     */
    private void resetLastAttemptDiagnostics() {
        lastProcessedModule = "";
        lastFailedModule = "";
        lastModuleError = "";
    }

    /**
     * Registra uma falha sem armazenar a mensagem do usuario.
     */
    private void recordFailure(
            String moduleName,
            RuntimeException error
    ) {
        lastFailedModule = safeDiagnosticName(moduleName);
        lastModuleError = error == null
                ? "UNKNOWN_ERROR"
                : error.getClass().getSimpleName();

        moduleFailureCount++;
    }

    /**
     * Registra uma falha identificada por um codigo interno.
     */
    private void recordFailure(
            String moduleName,
            String errorCode
    ) {
        lastFailedModule = safeDiagnosticName(moduleName);

        if (errorCode == null || errorCode.trim().isEmpty()) {
            lastModuleError = "UNKNOWN_ERROR";
        } else {
            lastModuleError = errorCode.trim();
        }

        moduleFailureCount++;
    }

    private String safeDiagnosticName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "desconhecido";
        }

        return name.trim();
    }

    /**
     * Obtem o nome de um modulo sem permitir que uma falha
     * ao consultar esse nome interrompa o processamento.
     */
    private String getSafeModuleName(
            JarvisVisionModule module
    ) {
        if (module == null) {
            return "desconhecido";
        }

        try {
            String name = module.getName();

            if (name != null && !name.trim().isEmpty()) {
                return name.trim();
            }

        } catch (RuntimeException ignored) {
            // Usa o nome da classe como alternativa.
        }

        return module.getClass().getSimpleName();
    }

    /**
     * Nome do ultimo modulo que produziu uma resposta.
     *
     * Quando a politica de seguranca fornece uma resposta,
     * o valor sera JarvisSafetyPolicy.
     */
    public synchronized String getLastProcessedModule() {
        return lastProcessedModule;
    }

    /**
     * Nome do ultimo modulo que apresentou uma falha.
     */
    public synchronized String getLastFailedModule() {
        return lastFailedModule;
    }

    /**
     * Codigo ou tipo da ultima falha registrada.
     *
     * Uma String vazia significa que nenhuma falha foi
     * registrada na tentativa atual.
     */
    public synchronized String getLastModuleError() {
        return lastModuleError;
    }

    /**
     * Total de falhas registradas nesta instancia.
     *
     * O contador nao e persistente e nao inclui o conteudo
     * das mensagens do usuario.
     */
    public synchronized int getModuleFailureCount() {
        return moduleFailureCount;
    }

    /**
     * Recupera informacoes relevantes da memoria existente.
     */
    public String getRelevantMemory(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }

        try {
            if (contextManager != null) {
                String result =
                        contextManager.getRelevantMemory(query);

                return result == null ? "" : result;
            }

            String result = memory.relevantContext(query, 5);

            return result == null ? "" : result;

        } catch (RuntimeException error) {
            recordMemoryFailure(error);
            return "";
        }
    }

    /**
     * Pesquisa registros na memoria existente.
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
                results = memory.search(query, 5);
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
     * Armazena texto usando a memoria existente.
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
     * Armazena texto e categoria usando a memoria existente.
     */
    public boolean remember(
            String text,
            String category
    ) {
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
     * Remove registros correspondentes a uma consulta,
     * utilizando a implementacao de memoria ja existente.
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

    /**
     * Registra falha originada numa operacao de memoria.
     * Nao armazena o texto pesquisado ou lembrado.
     */
    private synchronized void recordMemoryFailure(
            RuntimeException error
    ) {
        lastFailedModule = "JarvisMemory";
        lastModuleError = error == null
                ? "UNKNOWN_ERROR"
                : error.getClass().getSimpleName();

        moduleFailureCount++;
    }

    /**
     * Retorna a instancia de memoria ja utilizada pelo projeto.
     * Nao cria uma nova base SQLite.
     */
    public JarvisMemory getMemory() {
        return memory;
    }

    /**
     * Retorna o gerenciador compartilhado, se disponivel.
     */
    public JarvisContextManager getContextManager() {
        return contextManager;
    }

    /**
     * Retorna o motor de raciocinio desta instancia.
     */
    public JarvisReasoningEngine getReasoningEngine() {
        return reasoningEngine;
    }
}
