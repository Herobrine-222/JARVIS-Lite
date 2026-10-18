
package com.jarvis.lite;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Verificacoes locais para proteger operacoes solicitadas
 * sobre a memoria do JARVIS.
 *
 * Este modulo nao acessa o banco de dados, nao grava,
 * nao apaga e nao altera memorias por conta propria.
 *
 * A decisao e uma verificacao preliminar, nao uma
 * garantia completa de seguranca ou privacidade.
 */
public final class JarvisMemoryGuard {

    private static final int MAX_INPUT_LENGTH = 12000;
    private static final int MAX_MEMORY_LENGTH = 4000;

    private JarvisMemoryGuard() {
        // Classe utilitaria.
    }

    public enum Operation {
        READ,
        SEARCH,
        WRITE,
        IMPORT,
        EXPORT,
        DELETE,
        CLEAR_ALL
    }

    public enum Decision {
        ALLOW,
        REJECT,
        REQUIRE_CONFIRMATION,
        REQUIRE_REVIEW
    }

    public static final class Result {
        private final Operation operation;
        private final Decision decision;
        private final String reasonCode;
        private final String message;

        private Result(
                Operation operation,
                Decision decision,
                String reasonCode,
                String message) {
            this.operation = operation;
            this.decision = decision;
            this.reasonCode = reasonCode;
            this.message = message;
        }

        public Operation getOperation() {
            return operation;
        }

        public Decision getDecision() {
            return decision;
        }

        public String getReasonCode() {
            return reasonCode;
        }

        public String getMessage() {
            return message;
        }

        public boolean isAllowed() {
            return decision == Decision.ALLOW;
        }

        public boolean requiresConfirmation() {
            return decision == Decision.REQUIRE_CONFIRMATION;
        }

        public boolean requiresReview() {
            return decision == Decision.REQUIRE_REVIEW;
        }
    }

    /**
     * Avalia uma operacao antes de encaminha-la ao sistema
     * real de memoria.
     *
     * confirmed deve representar uma confirmacao explicita
     * do usuario, nao apenas uma palavra encontrada no texto.
     */
    public static Result check(
            Operation operation,
            String content,
            boolean confirmed) {

        if (operation == null) {
            return result(
                    null,
                    Decision.REQUIRE_REVIEW,
                    "INVALID_OPERATION",
                    "A operacao de memoria precisa ser identificada."
            );
        }

        if (content == null) {
            return result(
                    operation,
                    Decision.REJECT,
                    "NULL_CONTENT",
                    "O conteudo informado esta ausente."
            );
        }

        if (content.length() > MAX_INPUT_LENGTH) {
            return result(
                    operation,
                    Decision.REJECT,
                    "INPUT_TOO_LONG",
                    "O conteudo excede o limite permitido para esta verificacao."
            );
        }

        if (containsControlCharacters(content)) {
            return result(
                    operation,
                    Decision.REJECT,
                    "CONTROL_CHARACTERS",
                    "O conteudo possui caracteres de controle nao permitidos."
            );
        }

        switch (operation) {
            case READ:
            case SEARCH:
                return result(
                        operation,
                        Decision.ALLOW,
                        "READ_ALLOWED",
                        "A verificacao preliminar permite consultar a memoria."
                );

            case WRITE:
                if (content.trim().isEmpty()) {
                    return result(
                            operation,
                            Decision.REJECT,
                            "EMPTY_MEMORY",
                            "Nao e possivel guardar uma memoria vazia."
                    );
                }

                if (content.length() > MAX_MEMORY_LENGTH) {
                    return result(
                            operation,
                            Decision.REJECT,
                            "MEMORY_TOO_LONG",
                            "O texto excede o limite permitido para uma memoria."
                    );
                }

                return result(
                        operation,
                        Decision.ALLOW,
                        "WRITE_ALLOWED",
                        "A verificacao preliminar permite solicitar a gravacao."
                );

            case IMPORT:
                if (content.trim().isEmpty()) {
                    return result(
                            operation,
                            Decision.REJECT,
                            "EMPTY_IMPORT",
                            "O conteudo de importacao esta vazio."
                    );
                }

                return result(
                        operation,
                        Decision.REQUIRE_CONFIRMATION,
                        "IMPORT_CONFIRMATION_REQUIRED",
                        "Revise a origem e confirme a importacao antes de executa-la."
                );

            case EXPORT:
                return result(
                        operation,
                        Decision.REQUIRE_CONFIRMATION,
                        "EXPORT_CONFIRMATION_REQUIRED",
                        "Confirme a exportacao e verifique onde o arquivo sera salvo."
                );

            case DELETE:
                return result(
                        operation,
                        confirmed
                                ? Decision.REQUIRE_REVIEW
                                : Decision.REQUIRE_CONFIRMATION,
                        confirmed
                                ? "DELETE_REVIEW_REQUIRED"
                                : "DELETE_CONFIRMATION_REQUIRED",
                        "A exclusao exige confirmacao explicita e verificacao do alvo."
                );

            case CLEAR_ALL:
                return result(
                        operation,
                        Decision.REQUIRE_CONFIRMATION,
                        "CLEAR_ALL_CONFIRMATION_REQUIRED",
                        "Limpar toda a memoria e uma operacao destrutiva e exige confirmacao especifica."
                );

            default:
                return result(
                        operation,
                        Decision.REQUIRE_REVIEW,
                        "UNKNOWN_OPERATION",
                        "A operacao precisa ser revisada."
                );
        }
    }

    private static boolean containsControlCharacters(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (Character.isISOControl(c)
                    && c != '\n'
                    && c != '\r'
                    && c != '\t') {
                return true;
            }
        }

        return false;
    }

    private static Result result(
            Operation operation,
            Decision decision,
            String reasonCode,
            String message) {
        return new Result(
                operation,
                decision,
                reasonCode,
                message
        );
    }
}
