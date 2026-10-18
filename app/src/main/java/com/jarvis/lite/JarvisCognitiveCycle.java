
package com.jarvis.lite;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coordena o ciclo de processamento cognitivo do JARVIS.
 *
 * Este modulo nao cria uma rede neural e nao executa
 * comandos no dispositivo. O processamento real e
 * fornecido por uma interface externa.
 */
public final class JarvisCognitiveCycle {

    public static final int MAX_INPUT_LENGTH = 4000;
    public static final int MAX_OUTPUT_LENGTH = 8000;

    public enum Stage {
        IDLE,
        RECEIVED,
        PROCESSING,
        VALIDATING,
        COMPLETED,
        REJECTED,
        FAILED
    }

    public enum Status {
        SUCCESS,
        INVALID_INPUT,
        BUSY,
        NO_PROCESSOR,
        EMPTY_OUTPUT,
        OUTPUT_TOO_LONG,
        PROCESSING_ERROR,
        VALIDATION_REJECTED
    }

    public interface Processor {
        String process(String input) throws Exception;
    }

    public interface OutputValidator {
        boolean isValid(String input, String output);
    }

    public static final class CycleResult {
        private final Status status;
        private final Stage finalStage;
        private final String output;
        private final String message;
        private final long durationMillis;

        private CycleResult(
                Status status,
                Stage finalStage,
                String output,
                String message,
                long durationMillis) {

            this.status = status;
            this.finalStage = finalStage;
            this.output = output;
            this.message = message;
            this.durationMillis = durationMillis;
        }

        public Status getStatus() {
            return status;
        }

        public Stage getFinalStage() {
            return finalStage;
        }

        public String getOutput() {
            return output;
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

    private volatile Processor processor;
    private volatile OutputValidator validator;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile Stage currentStage = Stage.IDLE;
    private volatile Status lastStatus;
    private volatile String lastError = "";

    private long cycleCount;
    private long failureCount;

    public JarvisCognitiveCycle() {
    }

    public JarvisCognitiveCycle(Processor processor) {
        this.processor = processor;
    }

    public void setProcessor(Processor processor) {
        if (running.get()) {
            throw new IllegalStateException(
                    "Nao e possivel trocar o processador durante um ciclo.");
        }

        this.processor = processor;
    }

    public void setOutputValidator(OutputValidator validator) {
        if (running.get()) {
            throw new IllegalStateException(
                    "Nao e possivel trocar o validador durante um ciclo.");
        }

        this.validator = validator;
    }

    public CycleResult run(String input) {
        long start = System.nanoTime();

        if (input == null || input.trim().isEmpty()
                || input.length() > MAX_INPUT_LENGTH) {
            return finish(
                    Status.INVALID_INPUT,
                    Stage.REJECTED,
                    null,
                    "Entrada vazia ou acima do limite.",
                    start
            );
        }

        if (!running.compareAndSet(false, true)) {
            return new CycleResult(
                    Status.BUSY,
                    currentStage,
                    null,
                    "Ja existe um ciclo em andamento.",
                    elapsedMillis(start)
            );
        }

        try {
            currentStage = Stage.RECEIVED;

            Processor activeProcessor = processor;

            if (activeProcessor == null) {
                return finish(
                        Status.NO_PROCESSOR,
                        Stage.FAILED,
                        null,
                        "Nenhum processador foi configurado.",
                        start
                );
            }

            currentStage = Stage.PROCESSING;

            String output = activeProcessor.process(input.trim());

            if (output == null || output.trim().isEmpty()) {
                return finish(
                        Status.EMPTY_OUTPUT,
                        Stage.REJECTED,
                        null,
                        "O processador nao produziu uma resposta.",
                        start
                );
            }

            if (output.length() > MAX_OUTPUT_LENGTH) {
                return finish(
                        Status.OUTPUT_TOO_LONG,
                        Stage.REJECTED,
                        null,
                        "A resposta ultrapassou o limite permitido.",
                        start
                );
            }

            currentStage = Stage.VALIDATING;

            OutputValidator activeValidator = validator;

            if (activeValidator != null
                    && !activeValidator.isValid(input, output)) {
                return finish(
                        Status.VALIDATION_REJECTED,
                        Stage.REJECTED,
                        null,
                        "A resposta foi rejeitada pelo validador.",
                        start
                );
            }

            synchronized (this) {
                cycleCount++;
            }

            return finish(
                    Status.SUCCESS,
                    Stage.COMPLETED,
                    output.trim(),
                    "Ciclo concluido.",
                    start
            );

        } catch (Exception exception) {
            return finish(
                    Status.PROCESSING_ERROR,
                    Stage.FAILED,
                    null,
                    "Falha durante o processamento: "
                            + exception.getClass().getSimpleName(),
                    start
            );

        } finally {
            running.set(false);
        }
    }

    private CycleResult finish(
            Status status,
            Stage stage,
            String output,
            String message,
            long start) {

        currentStage = stage;
        lastStatus = status;

        if (status != Status.SUCCESS) {
            lastError = message;

            synchronized (this) {
                failureCount++;
            }
        } else {
            lastError = "";
        }

        return new CycleResult(
                status,
                stage,
                output,
                message,
                elapsedMillis(start)
        );
    }

    private long elapsedMillis(long start) {
        return (System.nanoTime() - start) / 1_000_000L;
    }

    public Stage getCurrentStage() {
        return currentStage;
    }

    public Status getLastStatus() {
        return lastStatus;
    }

    public String getLastError() {
        return lastError;
    }

    public boolean isRunning() {
        return running.get();
    }

    public synchronized long getCycleCount() {
        return cycleCount;
    }

    public synchronized long getFailureCount() {
        return failureCount;
    }

    public boolean hasProcessor() {
        return processor != null;
    }

    public boolean hasValidator() {
        return validator != null;
    }
}
