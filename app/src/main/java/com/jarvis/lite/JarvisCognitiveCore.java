
package com.jarvis.lite;

/**
 * Núcleo cognitivo do JARVIS Lite.
 *
 * Responsabilidades:
 * - Coordenar o processador de raciocínio existente.
 * - Disponibilizar contexto opcional ao processador.
 * - Validar respostas antes de aceitá-las.
 * - Permitir inferência neural explicitamente solicitada.
 * - Controlar concorrência e contabilizar resultados.
 *
 * Este componente não substitui automaticamente JarvisBrain,
 * JarvisCore, JarvisVisionCore, JarvisAIManager ou
 * JarvisReasoningEngine.
 *
 * Não realiza chamadas de rede por conta própria.
 * Não altera nem apaga memórias.
 */
public final class JarvisCognitiveCore {

    public static final int MAX_INPUT_CHARS = 4000;
    public static final int MAX_CONTEXT_CHARS = 6000;
    public static final int MAX_OUTPUT_CHARS = 8000;

    public enum Status {
        SUCCESS,
        INVALID_INPUT,
        NO_PROCESSOR,
        PROCESSOR_ERROR,
        NEURAL_UNAVAILABLE
    }

    /**
     * Resultado imutável de uma operação cognitiva.
     */
    public static final class ProcessingResult {

        private final Status status;
        private final String response;
        private final String source;
        private final String message;

        private ProcessingResult(
                Status status,
                String response,
                String source,
                String message) {

            this.status = status == null
                    ? Status.PROCESSOR_ERROR
                    : status;

            this.response = response;
            this.source = source == null ? "NONE" : source;
            this.message = message == null ? "" : message;
        }

        public Status getStatus() {
            return status;
        }

        public String getResponse() {
            return response;
        }

        public String getSource() {
            return source;
        }

        public String getMessage() {
            return message;
        }

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }
    }

    /**
     * Contrato compatível com processadores existentes.
     *
     * Implementações antigas continuam podendo implementar
     * somente process(String).
     */
    public interface CognitiveProcessor {
        ProcessingResult process(String input);
    }

    /**
     * Contrato opcional para processadores que sabem utilizar
     * contexto separado da mensagem original.
     *
     * Evita misturar automaticamente memórias com a entrada
     * do usuário ou modificar o comportamento dos processadores
     * antigos.
     */
    public interface ContextAwareCognitiveProcessor
            extends CognitiveProcessor {

        ProcessingResult process(String input, String context);
    }

    /**
     * Recupera contexto relevante, sem exigir gravação
     * ou alteração da memória.
     */
    public interface ContextProvider {
        String getContext(String input);
    }

    /**
     * Verifica se uma resposta pode ser aceita.
     *
     * A validação é uma barreira adicional, não uma garantia
     * absoluta de que a resposta seja verdadeira.
     */
    public interface ResponseValidator {
        boolean isAcceptable(String input, String response);
    }

    private CognitiveProcessor processor;
    private ContextProvider contextProvider;
    private ResponseValidator responseValidator;
    private JarvisNeuralCore neuralCore;

    private long processedCount;
    private long failureCount;
    private boolean processing;

    public JarvisCognitiveCore() {
    }

    public JarvisCognitiveCore(CognitiveProcessor processor) {
        this.processor = processor;
    }

    public synchronized void setProcessor(
            CognitiveProcessor processor) {
        this.processor = processor;
    }

    public synchronized void setContextProvider(
            ContextProvider contextProvider) {
        this.contextProvider = contextProvider;
    }

    public synchronized void setResponseValidator(
            ResponseValidator responseValidator) {
        this.responseValidator = responseValidator;
    }

    public synchronized void setNeuralCore(
            JarvisNeuralCore neuralCore) {
        this.neuralCore = neuralCore;
    }

    /**
     * Processa uma mensagem usando o processador configurado.
     */
    public ProcessingResult process(String input) {

        final String normalizedInput = normalizeInput(input);

        if (normalizedInput == null) {
            return failure(
                    Status.INVALID_INPUT,
                    "A mensagem está vazia ou excede o limite permitido.");
        }

        final CognitiveProcessor selectedProcessor;
        final ContextProvider selectedContextProvider;
        final ResponseValidator selectedValidator;

        synchronized (this) {

            if (processing) {
                return failure(
                        Status.PROCESSOR_ERROR,
                        "Já existe um processamento cognitivo em andamento.");
            }

            if (processor == null) {
                return failure(
                        Status.NO_PROCESSOR,
                        "Nenhum processador cognitivo foi conectado.");
            }

            selectedProcessor = processor;
            selectedContextProvider = contextProvider;
            selectedValidator = responseValidator;

            processing = true;
        }

        try {
            String context = "";

            /*
             * A falha ao recuperar contexto não impede que
             * o processador principal tente responder.
             */
            if (selectedContextProvider != null) {
                try {
                    context = normalizeContext(
                            selectedContextProvider.getContext(
                                    normalizedInput));
                } catch (RuntimeException exception) {
                    context = "";
                }
            }

            ProcessingResult result;

            /*
             * Processadores compatíveis com contexto recebem
             * a mensagem e o contexto em parâmetros separados.
             *
             * Processadores antigos continuam usando
             * process(String), sem alterações no contrato.
             */
            if (selectedProcessor
                    instanceof ContextAwareCognitiveProcessor) {

                ContextAwareCognitiveProcessor contextProcessor =
                        (ContextAwareCognitiveProcessor)
                                selectedProcessor;

                result = contextProcessor.process(
                        normalizedInput,
                        context);
            } else {
                result = selectedProcessor.process(normalizedInput);
            }

            if (result == null) {
                return failure(
                        Status.PROCESSOR_ERROR,
                        "O processador não retornou um resultado.");
            }

            if (!result.isSuccess()) {
                recordFailure();

                return new ProcessingResult(
                        result.getStatus(),
                        null,
                        result.getSource(),
                        safeMessage(
                                result.getMessage(),
                                500));
            }

            String response = normalizeOutput(
                    result.getResponse());

            if (response == null) {
                return failure(
                        Status.PROCESSOR_ERROR,
                        "O processador retornou uma resposta vazia "
                                + "ou acima do limite permitido.");
            }

            if (selectedValidator != null) {
                boolean acceptable;

                try {
                    acceptable = selectedValidator.isAcceptable(
                            normalizedInput,
                            response);
                } catch (RuntimeException exception) {
                    /*
                     * Falha na validação: comportamento seguro.
                     * Não entregar uma resposta que não pôde
                     * passar pelo validador configurado.
                     */
                    acceptable = false;
                }

                if (!acceptable) {
                    return failure(
                            Status.PROCESSOR_ERROR,
                            "A resposta não passou pela validação.");
                }
            }

            synchronized (this) {
                processedCount++;
            }

            return new ProcessingResult(
                    Status.SUCCESS,
                    response,
                    safeSource(result.getSource()),
                    "Processamento cognitivo concluído.");

        } catch (RuntimeException exception) {

            return failure(
                    Status.PROCESSOR_ERROR,
                    "Ocorreu uma falha no processamento cognitivo.");

        } finally {

            synchronized (this) {
                processing = false;
            }
        }
    }

    /**
     * Solicita inferência neural explicitamente.
     *
     * Esta operação não é acionada automaticamente por process().
     */
    public ProcessingResult requestNeuralInference(String input) {

        final String normalizedInput = normalizeInput(input);

        if (normalizedInput == null) {
            return failure(
                    Status.INVALID_INPUT,
                    "A entrada neural está vazia ou excede o limite.");
        }

        final JarvisNeuralCore selectedNeuralCore;
        final ResponseValidator selectedValidator;

        synchronized (this) {
            selectedNeuralCore = neuralCore;
            selectedValidator = responseValidator;
        }

        if (selectedNeuralCore == null) {
            return failure(
                    Status.NEURAL_UNAVAILABLE,
                    "O núcleo neural ainda não foi conectado.");
        }

        try {
            JarvisNeuralCore.InferenceResult neuralResult =
                    selectedNeuralCore.infer(normalizedInput);

            if (neuralResult == null || !neuralResult.isSuccess()) {
                return failure(
                        Status.NEURAL_UNAVAILABLE,
                        neuralResult == null
                                ? "O núcleo neural não retornou resultado."
                                : safeMessage(
                                        neuralResult.getMessage(),
                                        500));
            }

            String response = normalizeOutput(
                    neuralResult.getText());

            if (response == null) {
                return failure(
                        Status.NEURAL_UNAVAILABLE,
                        "A resposta neural está vazia ou excede "
                                + "o limite permitido.");
            }

            if (selectedValidator != null) {
                boolean acceptable;

                try {
                    acceptable = selectedValidator.isAcceptable(
                            normalizedInput,
                            response);
                } catch (RuntimeException exception) {
                    acceptable = false;
                }

                if (!acceptable) {
                    return failure(
                            Status.NEURAL_UNAVAILABLE,
                            "A resposta neural não passou pela validação.");
                }
            }

            return new ProcessingResult(
                    Status.SUCCESS,
                    response,
                    "NEURAL",
                    "Inferência neural concluída.");

        } catch (RuntimeException exception) {

            return failure(
                    Status.NEURAL_UNAVAILABLE,
                    "Não foi possível concluir a inferência neural.");
        }
    }

    /**
     * Remove espaços externos e verifica o limite da entrada.
     * Retorna null quando a entrada não é válida.
     */
    private String normalizeInput(String input) {

        if (input == null) {
            return null;
        }

        String normalized = input.trim();

        if (normalized.isEmpty()
                || normalized.length() > MAX_INPUT_CHARS) {
            return null;
        }

        return normalized;
    }

    /**
     * Limita o contexto sem modificar a memória original.
     */
    private String normalizeContext(String context) {

        if (context == null) {
            return "";
        }

        String normalized = context.trim();

        if (normalized.length() > MAX_CONTEXT_CHARS) {
            normalized = normalized.substring(
                    0,
                    MAX_CONTEXT_CHARS);
        }

        return normalized;
    }

    /**
     * Valida e normaliza a resposta antes da entrega.
     */
    private String normalizeOutput(String output) {

        if (output == null) {
            return null;
        }

        String normalized = output.trim();

        if (normalized.isEmpty()
                || normalized.length() > MAX_OUTPUT_CHARS) {
            return null;
        }

        return normalized;
    }

    private String safeSource(String source) {

        if (source == null || source.trim().isEmpty()) {
            return "COGNITIVE";
        }

        String normalized = source.trim();

        if (normalized.length() > 100) {
            return normalized.substring(0, 100);
        }

        return normalized;
    }

    private String safeMessage(String message, int maxChars) {

        if (message == null || message.trim().isEmpty()) {
            return "Não foi possível concluir o processamento.";
        }

        String normalized = message.trim();

        if (normalized.length() > maxChars) {
            return normalized.substring(0, maxChars);
        }

        return normalized;
    }

    /**
     * Centraliza a contabilização de falhas para evitar
     * incrementos duplicados no mesmo caminho de execução.
     */
    private synchronized ProcessingResult failure(
            Status status,
            String message) {

        if (status != Status.INVALID_INPUT
                && status != Status.NO_PROCESSOR) {
            failureCount++;
        }

        return new ProcessingResult(
                status,
                null,
                "NONE",
                message);
    }

    private synchronized void recordFailure() {
        failureCount++;
    }

    public synchronized long getProcessedCount() {
        return processedCount;
    }

    public synchronized long getFailureCount() {
        return failureCount;
    }

    public synchronized boolean isProcessing() {
        return processing;
    }

    public synchronized boolean hasProcessor() {
        return processor != null;
    }

    public synchronized boolean hasContextProvider() {
        return contextProvider != null;
    }

    public synchronized boolean hasResponseValidator() {
        return responseValidator != null;
    }

    public synchronized boolean hasNeuralCore() {
        return neuralCore != null;
    }
}
