package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Coordenador dos módulos especialistas do VISION.
 *
 * Reutiliza o motor de raciocínio, a memória persistente
 * e a base de conhecimento já existentes no JARVIS Lite.
 *
 * Não cria uma segunda base de memória.
 * Não acessa a internet.
 * Não executa comandos do sistema operacional.
 */
public final class JarvisVisionCore {

    private final JarvisReasoningEngine reasoningEngine;
    private final JarvisMemory memory;
    private final JarvisContextManager contextManager;

    private final List<JarvisVisionModule> modules =
            new ArrayList<>();

    /*
     * Diagnóstico da última tentativa de processamento.
     *
     * Os erros são registrados pelo tipo da exceção,
     * sem armazenar o conteúdo privado das mensagens.
     */
    private String lastProcessedModule = "";
    private String lastFailedModule = "";
    private String lastModuleError = "";
    private int moduleFailureCount = 0;

    /**
     * Construtor principal.
     *
     * Recebe o gerenciador compartilhado do aplicativo.
     * Dessa forma, os especialistas podem reutilizar
     * a memória e as ferramentas existentes.
     */
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

        registerDefaultModules();
    }

    /**
     * Construtor de compatibilidade.
     *
     * Permite utilizar o núcleo somente com a memória.
     * Sem ContextManager, os módulos que dependem
     * das ferramentas do aparelho não são registrados.
     */
    public JarvisVisionCore(
            JarvisMemory memory
    ) {
        if (memory == null) {
            throw new IllegalArgumentException(
                    "A memória não pode ser nula."
            );
        }

        this.memory = memory;
        this.contextManager = null;
        this.reasoningEngine = new JarvisReasoningEngine();

        registerDefaultModules();
    }

    /**
     * Registra os especialistas já existentes no projeto.
     *
     * O registro automático evita que o núcleo seja
     * criado com uma lista vazia de módulos.
     */
    private void registerDefaultModules() {

        registerModule(new JarvisEdithModule());

        registerModule(new JarvisKarenModule());

        registerModule(new JarvisUModule());

        /*
         * O DUM-E utiliza as ferramentas mantidas
         * pelo ContextManager para consultar o aparelho.
         */
        if (contextManager != null) {
            registerModule(new JarvisDumEModule());
        }

        /*
         * FRIDAY permanece disponível para as funções
         * conversacionais e de consulta à memória
         * que seu módulo já implementa.
         */
        registerModule(new JarvisFridayModule());
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
     * Consulta a base de conhecimento offline existente.
     *
     * Retorna null quando não existe resposta cadastrada.
     */
    public String answerFromExistingKnowledgeBase(
            String input
    ) {
        return JarvisKnowledgeBase.answer(input);
    }

    /**
     * Registra um módulo adicional.
     *
     * Não permite registrar dois módulos com o mesmo nome.
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

        for (JarvisVisionModule registered : modules) {
            String registeredName;

            try {
                registeredName = registered.getName();
            } catch (RuntimeException error) {
                continue;
            }

            if (registeredName != null
                    && registeredName.equalsIgnoreCase(
                            name.trim()
                    )) {
                return false;
            }
        }

        modules.add(module);
        return true;
    }

    /**
     * Retorna uma cópia não modificável dos nomes registrados.
     */
    public synchronized List<String> getRegisteredModuleNames() {

        List<String> names = new ArrayList<>();

        for (JarvisVisionModule module : modules) {
            names.add(getSafeModuleName(module));
        }

        return Collections.unmodifiableList(names);
    }

    /**
     * Encaminha a mensagem aos módulos registrados.
     *
     * Um módulo pode devolver null quando não consegue
     * tratar a solicitação. Nesse caso, o próximo módulo
     * compatível terá a oportunidade de responder.
     *
     * Uma falha isolada não deve interromper os demais.
     */
    public synchronized String processModules(
            String input
    ) {
        lastProcessedModule = "";
        lastFailedModule = "";
        lastModuleError = "";

        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        JarvisReasoningEngine.Analysis analysis =
                reasoningEngine.analyze(input);

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
                 * Guarda apenas dados técnicos básicos.
                 * Não registra a mensagem privada do usuário.
                 */
                lastFailedModule = moduleName;
                lastModuleError =
                        error.getClass().getSimpleName();

                moduleFailureCount++;

                /*
                 * Continua o processamento para que
                 * outro módulo possa tentar responder.
                 */
            }
        }

        return null;
    }

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
     * Nome do último módulo que produziu uma resposta.
     */
    public synchronized String getLastProcessedModule() {
        return lastProcessedModule;
    }

    /**
     * Nome do último módulo que apresentou uma falha.
     */
    public synchronized String getLastFailedModule() {
        return lastFailedModule;
    }

    /**
     * Tipo da última exceção ocorrida.
     *
     * String vazia significa que nenhuma falha
     * foi registrada na última tentativa.
     */
    public synchronized String getLastModuleError() {
        return lastModuleError;
    }

    /**
     * Total de falhas de módulos nesta instância.
     *
     * O contador não é persistente: ele vale somente
     * durante a vida desta instância do núcleo.
     */
    public synchronized int getModuleFailureCount() {
        return moduleFailureCount;
    }

    /**
     * Recupera informações relevantes da memória existente.
     */
    public String getRelevantMemory(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }

        if (contextManager != null) {
            return contextManager.getRelevantMemory(query);
        }

        return memory.relevantContext(query, 5);
    }

    /**
     * Pesquisa registros da memória existente.
     */
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
