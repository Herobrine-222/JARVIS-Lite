package com.jarvis.lite;

/**
 * Núcleo cognitivo do JARVIS Lite.
 *
 * Coordena interpretação, contexto, raciocínio e uso opcional
 * do núcleo neural por meio de interfaces injetáveis.
 *
 * Não substitui automaticamente JarvisCore, JarvisVisionCore,
 * JarvisAIManager ou JarvisReasoningEngine.
 */
public final class JarvisCognitiveCore {

    public static final int MAX_INPUT_CHARS = 4000;

    public enum Status {
        SUCCESS,
        INVALID_INPUT,
        NO_PROCESSOR,
        PROCESSOR_ERROR,
        NEURAL_UNAVAILABLE
    }

    /**
     * Resultado de uma etapa de processamento cognitivo.
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
            this.status = status;
            this.response = response;
            this.source = source;
            this.message = message;
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
     * Contrato para o raciocínio local já existente ou futuro.
     *
     * A implementação pode integrar o JarvisReasoningEngine,
     * JarvisBrain ou outro coordenador, após a auditoria das APIs.
     */
    public interface CognitiveProcessor {
        ProcessingResult process(String input);
    }

    /**
     * Contrato opcional para recuperar contexto relevante.
     * Não implica gravar nem apagar memórias.
     */
    public interface ContextProvider {
        String getContext(String input);
    }

    /**
     * Contrato para validar uma resposta antes de entregá-la.
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
     * Processa uma mensagem pelo processador cognitivo local.
     *
     * O contexto é recuperado quando existe um provedor,
     * mas não é anexado automaticamente à mensagem até que
     * a estratégia de contexto seja definida pela arquitetura.
     */
    public ProcessingResult process(String input) {
        if (input == null || input.trim().isEmpty()) {
            return failure(
                    Status.INVALID_INPUT,
                    "A mensagem está vazia.");
        }

        if (input.length() > MAX_INPUT_CHARS) {
            return failure(
                    Status.INVALID_INPUT,
                    "A mensagem excede o limite permitido.");
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

            selectedProcessor = processor;
            selectedContextProvider = contextProvider;
            selectedValidator = responseValidator;

            if (selectedProcessor == null) {
                return failure(
                        Status.NO_PROCESSOR,
                        "Nenhum processador cognitivo foi conectado.");
            }

            processing = true;
        }

        try {
            /*
             * Etapa de contexto:
             * a recuperação pode ajudar o processador,
             * mas ainda não altera nem persiste a memória.
             */
            String context = null;

            if (selectedContextProvider != null) {
                try {
                    context = selectedContextProvider.getContext(input);
                } catch (RuntimeException ignored) {
                    // A falha de contexto não deve derrubar o núcleo.
                    context = null;
                }
            }

            /*
             * A interface atual mantém o processador desacoplado.
             * A integração de contexto será feita quando o contrato
             * comum dos módulos de raciocínio estiver definido.
             */
            ProcessingResult result = selectedProcessor.process(input);

            if (result == null || !result.isSuccess()) {
                synchronized (this) {
                    failureCount++;
                }

                if (result == null) {
                    return failure(
                            Status.PROCESSOR_ERROR,
                            "O processador não retornou um resultado.");
                }

                return result;
            }

            String response = result.getResponse();

            if (response == null || response.trim().isEmpty()) {
                return failure(
                        Status.PROCESSOR_ERROR,
                        "O processador retornou uma resposta vazia.");
            }

            if (selectedValidator != null) {
                boolean acceptable;

                try {
                    acceptable = selectedValidator.isAcceptable(
                            input, response);
                } catch (RuntimeException exception) {
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
                    result.getSource(),
                    "Processamento cognitivo concluído.");

        } catch (RuntimeException exception) {
            synchronized (this) {
                failureCount++;
            }

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
     * Não é acionada automaticamente nesta primeira versão.
     */
    public ProcessingResult requestNeuralInference(String input) {
        if (input == null || input.trim().isEmpty()) {
            return failure(
                    Status.INVALID_INPUT,
                    "A entrada neural está vazia.");
        }

        if (input.length() > MAX_INPUT_CHARS) {
            return failure(
                    Status.INVALID_INPUT,
                    "A entrada neural excede o limite permitido.");
        }

        JarvisNeuralCore selectedNeuralCore;

        synchronized (this) {
            selectedNeuralCore = neuralCore;
        }

        if (selectedNeuralCore == null) {
            return failure(
                    Status.NEURAL_UNAVAILABLE,
                    "O núcleo neural ainda não foi conectado.");
        }

        JarvisNeuralCore.InferenceResult result =
                selectedNeuralCore.infer(input);

        if (result == null || !result.isSuccess()) {
            return failure(
                    Status.NEURAL_UNAVAILABLE,
                    result == null
                            ? "Não houve resultado neural."
                            : result.getMessage());
        }

        return new ProcessingResult(
                Status.SUCCESS,
                result.getText(),
                "NEURAL",
                "Inferência neural concluída.");
    }

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

    public synchronized boolean hasNeuralCore() {
        return neuralCore != null;
    }
}
