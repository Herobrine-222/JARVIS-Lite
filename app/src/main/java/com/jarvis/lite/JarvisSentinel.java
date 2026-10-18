
package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Supervisor leve de integridade do JARVIS Lite.
 *
 * Avalia resultados de módulos e recomenda uma resposta segura.
 * Não executa comandos, não acessa a rede, não altera permissões
 * e não desativa módulos automaticamente.
 */
public final class JarvisSentinel {

    public enum Severity {
        INFO, WARNING, ERROR, CRITICAL
    }

    public enum Recommendation {
        ACCEPT,
        RETRY_ONCE,
        USE_SAFE_FALLBACK,
        ASK_USER,
        STOP_PIPELINE
    }

    public static final class Check {
        public final boolean accepted;
        public final Severity severity;
        public final Recommendation recommendation;
        public final String code;
        public final String message;

        private Check(
                boolean accepted,
                Severity severity,
                Recommendation recommendation,
                String code,
                String message) {
            this.accepted = accepted;
            this.severity = severity;
            this.recommendation = recommendation;
            this.code = code;
            this.message = message;
        }
    }

    private JarvisSentinel() {
        // Evita criar instâncias desnecessárias.
    }

    /**
     * Analisa o resultado de um módulo.
     * Não armazena a entrada nem o conteúdo da resposta.
     */
    public static Check inspectResult(
            String moduleName,
            String result,
            Throwable error,
            boolean requiresNonEmptyResult) {

        String module = safeLabel(moduleName, "module");

        if (error != null) {
            return new Check(
                    false,
                    Severity.ERROR,
                    Recommendation.USE_SAFE_FALLBACK,
                    "MODULE_EXCEPTION",
                    module + " apresentou uma exceção; use uma alternativa segura."
            );
        }

        if (result == null) {
            return new Check(
                    false,
                    Severity.WARNING,
                    Recommendation.USE_SAFE_FALLBACK,
                    "NULL_RESULT",
                    module + " não retornou um resultado."
            );
        }

        if (requiresNonEmptyResult && result.trim().isEmpty()) {
            return new Check(
                    false,
                    Severity.WARNING,
                    Recommendation.RETRY_ONCE,
                    "EMPTY_RESULT",
                    module + " retornou uma resposta vazia."
            );
        }

        return new Check(
                true,
                Severity.INFO,
                Recommendation.ACCEPT,
                "RESULT_OK",
                module + " retornou um resultado."
        );
    }

    /**
     * Compara a duração informada com um limite definido pelo chamador.
     * Não cria temporizadores nem interrompe tarefas.
     */
    public static Check inspectDuration(
            String moduleName,
            long durationMillis,
            long allowedDurationMillis) {

        String module = safeLabel(moduleName, "module");

        if (durationMillis < 0L || allowedDurationMillis < 0L) {
            return new Check(
                    false,
                    Severity.WARNING,
                    Recommendation.USE_SAFE_FALLBACK,
                    "INVALID_DURATION",
                    "Medição de duração inválida para " + module + "."
            );
        }

        if (durationMillis > allowedDurationMillis) {
            return new Check(
                    false,
                    Severity.WARNING,
                    Recommendation.USE_SAFE_FALLBACK,
                    "SLOW_MODULE",
                    module + " ultrapassou o limite de tempo definido."
            );
        }

        return new Check(
                true,
                Severity.INFO,
                Recommendation.ACCEPT,
                "DURATION_OK",
                module + " concluiu dentro do limite definido."
        );
    }

    /**
     * Segunda verificação de segurança.
     * A permissão Android real deve ser conferida pelo código
     * responsável pela operação, não apenas por este módulo.
     */
    public static Check inspectPermissionDecision(
            boolean allowed,
            boolean permissionRequired,
            boolean androidPermissionGranted) {

        if (permissionRequired && !androidPermissionGranted) {
            return new Check(
                    false,
                    Severity.CRITICAL,
                    Recommendation.STOP_PIPELINE,
                    "ANDROID_PERMISSION_MISSING",
                    "A permissão Android necessária não foi confirmada."
            );
        }

        if (!allowed) {
            return new Check(
                    false,
                    Severity.WARNING,
                    Recommendation.ASK_USER,
                    "CAPABILITY_NOT_ALLOWED",
                    "O controle de capacidades não autorizou a operação."
            );
        }

        return new Check(
                true,
                Severity.INFO,
                Recommendation.ACCEPT,
                "CAPABILITY_ALLOWED",
                "A verificação preliminar permitiu a operação."
        );
    }

    /**
     * Retorna uma cópia imutável dos resultados recebidos.
     * Não salva registros permanentemente.
     */
    public static List<Check> snapshot(List<Check> checks) {
        if (checks == null || checks.isEmpty()) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(
                new ArrayList<Check>(checks)
        );
    }

    private static String safeLabel(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        String cleaned = value.trim()
                .replaceAll("[\\r\\n\\t]", " ");

        return cleaned.length() > 48
                ? cleaned.substring(0, 48)
                : cleaned;
    }
}
