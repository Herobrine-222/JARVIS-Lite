package com.jarvis.lite;

/**
 * Núcleo neural do JARVIS Lite.
 *
 * Coordena a inferência por meio de um backend substituível.
 * Esta classe não implementa nem treina uma rede neural por si só.
 *
 * Compatível com Java padrão, sem dependências externas.
 */
public final class JarvisNeuralCore {

    public static final int DEFAULT_MAX_INPUT_CHARS = 4000;
    public static final int DEFAULT_MAX_OUTPUT_CHARS = 8000;

    public enum Status {
        READY,
        MODEL_UNAVAILABLE,
        PROCESSING,
        SUCCESS,
        INVALID_INPUT,
        OUTPUT_REJECTED,
        ERROR
    }

    /**
     * Contrato para um runtime/modelo neural real.
     * Uma implementação concreta deverá executar a inferência.
     */
    public interface NeuralBackend {
        boolean isAvailable();

        String generate(String input) throws Exception;

        String getBackendName();
    }

    /**
     * Resultado explícito de uma tentativa de inferência.
     */
    public static final class InferenceResult {
        private final Status status;
        private final String text;
        private final String message;
        private final long durationMillis;

        private InferenceResult(
                Status status,
                String text,
                String message,
                long durationMillis) {
            this.status = status;
            this.text = text;
            this.message = message;
            this.durationMillis = durationMillis;
        }

        public Status getStatus() {
            return status;
        }

        public String getText() {
            return text;
        }

        public String getMessage() {
            return message;
        }

        public long getDurationMillis() {
            return durationMillis;
        }

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }
    }

    private final int maxInputChars;
    private final int maxOutputChars;

    private NeuralBackend backend;
    private Status lastStatus = Status.MODEL_UNAVAILABLE;
    private String lastError = "";
    private long inferenceCount;
    private long failureCount;
    private boolean processing;

    public JarvisNeuralCore() {
        this(DEFAULT_MAX_INPUT_CHARS, DEFAULT_MAX_OUTPUT_CHARS);
    }

    public JarvisNeuralCore(
            int maxInputChars,
            int maxOutputChars) {

        if (maxInputChars < 1 || maxOutputChars < 1) {
            throw new IllegalArgumentException(
                    "Os limites de entrada e saída devem ser positivos.");
        }

        this.maxInputChars = maxInputChars;
        this.maxOutputChars = maxOutputChars;
    }

    /**
     * Registra ou substitui o backend.
     * Passar null desativa a inferência neural.
     */
    public synchronized void setBackend(NeuralBackend backend) {
        if (processing) {
            throw new IllegalStateException(
                    "Não é possível substituir o backend durante a inferência.");
        }

        this.backend = backend;
        this.lastError = "";

        if (backend == null) {
            lastStatus = Status.MODEL_UNAVAILABLE;
        } else {
            lastStatus = safeIsAvailable(backend)
                    ? Status.READY
                    : Status.MODEL_UNAVAILABLE;
        }
    }

    /**
     * Executa uma inferência usando o backend registrado.
     * Não realiza chamadas de rede por conta própria.
     */
    public InferenceResult infer(String input) {
        final NeuralBackend selectedBackend;

        synchronized (this) {
            if (input == null || input.trim().isEmpty()) {
                return result(
                        Status.INVALID_INPUT,
                        null,
                        "A entrada está vazia.",
                        0L);
            }

            if (input.length() > maxInputChars) {
                return result(
                        Status.INVALID_INPUT,
                        null,
                        "A entrada excede o limite permitido.",
                        0L);
            }

            if (processing) {
                return result(
                        Status.ERROR,
                        null,
                        "Já existe uma inferência em andamento.",
                        0L);
            }

            selectedBackend = backend;

            if (selectedBackend == null) {
                return result(
                        Status.MODEL_UNAVAILABLE,
                        null,
                        "Nenhum backend neural foi conectado.",
                        0L);
            }

            if (!safeIsAvailable(selectedBackend)) {
                return result(
                        Status.MODEL_UNAVAILABLE,
                        null,
                        "O backend neural não está disponível.",
                        0L);
            }

            processing = true;
            lastStatus = Status.PROCESSING;
        }

        long start = System.nanoTime();

        try {
            String output = selectedBackend.generate(input);

            long elapsed = elapsedMillis(start);

            if (output == null || output.trim().isEmpty()) {
                return result(
                        Status.ERROR,
                        null,
                        "O backend não produziu uma resposta válida.",
                        elapsed);
            }

            if (output.length() > maxOutputChars) {
                return result(
                        Status.OUTPUT_REJECTED,
                        null,
                        "A saída excedeu o limite permitido.",
                        elapsed);
            }

            return result(
                    Status.SUCCESS,
                    output.trim(),
                    "Inferência concluída pelo backend conectado.",
                    elapsed);

        } catch (Exception exception) {
            long elapsed = elapsedMillis(start);

            synchronized (this) {
                lastError = exception.getClass().getSimpleName()
                        + ": falha durante a inferência.";
            }

            return result(
                    Status.ERROR,
                    null,
                    "Não foi possível concluir a inferência.",
                    elapsed);

        } finally {
            synchronized (this) {
                processing = false;
            }
        }
    }

    private boolean safeIsAvailable(NeuralBackend candidate) {
        try {
            return candidate.isAvailable();
        } catch (RuntimeException exception) {
            lastError = "Falha ao verificar a disponibilidade do backend.";
            return false;
        }
    }

    private synchronized InferenceResult result(
            Status status,
            String text,
            String message,
            long durationMillis) {

        lastStatus = status;

        if (status == Status.SUCCESS) {
            inferenceCount++;
            lastError = "";
        } else if (status == Status.ERROR
                || status == Status.OUTPUT_REJECTED) {
            failureCount++;

            if (lastError.isEmpty()) {
                lastError = message;
            }
        }

        return new InferenceResult(
                status,
                text,
                message,
                durationMillis);
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    public synchronized Status getLastStatus() {
        return lastStatus;
    }

    public synchronized String getLastError() {
        return lastError;
    }

    public synchronized long getInferenceCount() {
        return inferenceCount;
    }

    public synchronized long getFailureCount() {
        return failureCount;
    }

    public synchronized boolean isProcessing() {
        return processing;
    }

    public synchronized boolean isBackendAvailable() {
        return backend != null && safeIsAvailable(backend);
    }

    public synchronized String getBackendName() {
        if (backend == null) {
            return "Nenhum backend conectado";
        }

        try {
            String name = backend.getBackendName();
            return name == null || name.trim().isEmpty()
                    ? "Backend sem nome"
                    : name;
        } catch (RuntimeException exception) {
            return "Backend indisponível";
        }
    }
}
