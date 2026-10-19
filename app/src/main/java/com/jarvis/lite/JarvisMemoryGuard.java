
package com.jarvis.lite;

/**
 * JARVIS Lite — verificacao preliminar de operacoes de memoria.
 *
 * Este modulo:
 * - valida operacoes e limites de entrada;
 * - distingue operacoes de leitura e alteracao;
 * - solicita confirmacao para operacoes sensiveis;
 * - exige revisao para operacoes destrutivas ambiguas;
 * - nao acessa nem modifica o banco SQLite.
 *
 * IMPORTANTE:
 * Uma decisao ALLOW nao garante que a operacao seja segura.
 * O componente responsavel ainda precisa validar o alvo,
 * as permissoes, o estado do banco e a autorizacao real.
 */
public final class JarvisMemoryGuard {

    private static final int MAX_INPUT_LENGTH = 12000;
    private static final int MAX_MEMORY_LENGTH = 4000;
    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MAX_REASON_CODE_LENGTH = 100;

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

    /**
     * Resultado imutavel da verificacao.
     */
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

            this.decision = decision == null
                    ? Decision.REQUIRE_REVIEW
                    : decision;

            this.reasonCode = safeText(
                    reasonCode,
                    MAX_REASON_CODE_LENGTH,
                    "UNSPECIFIED"
            );

            this.message = safeText(
                    message,
                    MAX_MESSAGE_LENGTH,
                    "A verificacao nao forneceu detalhes."
            );
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

        public boolean isRejected() {
            return decision == Decision.REJECT;
        }

        @Override
        public String toString() {
            return "Result{"
                    + "operation=" + operation
                    + ", decision=" + decision
                    + ", reasonCode='" + reasonCode + '\''
                    + '}';
        }
    }

    /**
     * API original mantida para compatibilidade.
     *
     * confirmed representa uma confirmacao informada pelo
     * chamador. Este metodo nao consegue provar que a
     * confirmacao foi obtida de uma interface confiavel.
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

        /*
         * Verifica o tamanho antes de executar outras analises
         * sobre o conteudo.
         */
        if (content.length() > MAX_INPUT_LENGTH) {
            return result(
                    operation,
                    Decision.REJECT,
                    "INPUT_TOO_LONG",
                    "O conteudo excede o limite desta verificacao."
            );
        }

        if (containsDisallowedControlCharacters(content)) {
            return result(
                    operation,
                    Decision.REJECT,
                    "CONTROL_CHARACTERS",
                    "O conteudo possui caracteres de controle nao permitidos."
            );
        }

        switch (operation) {

            case READ:
                return result(
                        operation,
                        Decision.ALLOW,
                        "READ_ALLOWED",
                        "A verificacao preliminar permite solicitar a leitura."
                );

            case SEARCH:
                if (content.trim().isEmpty()) {
                    return result(
                            operation,
                            Decision.REJECT,
                            "EMPTY_SEARCH",
                            "Informe um termo para pesquisar na memoria."
                    );
                }

                return result(
                        operation,
                        Decision.ALLOW,
                        "SEARCH_ALLOWED",
                        "A verificacao preliminar permite solicitar a pesquisa."
                );

            case WRITE:
                return checkWrite(operation, content);

            case IMPORT:
                return checkImport(operation, content, confirmed);

            case EXPORT:
                return checkExport(operation, confirmed);

            case DELETE:
                return checkDelete(operation, content, confirmed);

            case CLEAR_ALL:
                return checkClearAll(operation, confirmed);

            default:
                return result(
                        operation,
                        Decision.REQUIRE_REVIEW,
                        "UNKNOWN_OPERATION",
                        "A operacao precisa ser revisada antes de prosseguir."
                );
        }
    }

    private static Result checkWrite(
            Operation operation,
            String content) {

        if (content.trim().isEmpty()) {
            return result(
                    operation,
                    Decision.REJECT,
                    "EMPTY_MEMORY",
                    "Nao e permitido solicitar a gravacao de uma memoria vazia."
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
                "A verificacao preliminar permite solicitar a gravacao; valide duplicatas, privacidade e estado do banco."
        );
    }

    private static Result checkImport(
            Operation operation,
            String content,
            boolean confirmed) {

        if (content.trim().isEmpty()) {
            return result(
                    operation,
                    Decision.REJECT,
                    "EMPTY_IMPORT",
                    "O conteudo de importacao esta vazio."
            );
        }

        if (!confirmed) {
            return result(
                    operation,
                    Decision.REQUIRE_CONFIRMATION,
                    "IMPORT_CONFIRMATION_REQUIRED",
                    "Revise a origem, o conteudo e o destino antes de confirmar a importacao."
            );
        }

        return result(
                operation,
                Decision.REQUIRE_REVIEW,
                "IMPORT_VALIDATION_REQUIRED",
                "A confirmacao foi informada; valide o formato, os limites, a origem e a integridade antes de importar."
        );
    }

    private static Result checkExport(
            Operation operation,
            boolean confirmed) {

        if (!confirmed) {
            return result(
                    operation,
                    Decision.REQUIRE_CONFIRMATION,
                    "EXPORT_CONFIRMATION_REQUIRED",
                    "Confirme a exportacao e verifique onde os dados serao salvos."
            );
        }

        return result(
                operation,
                Decision.REQUIRE_REVIEW,
                "EXPORT_DESTINATION_REVIEW_REQUIRED",
                "A confirmacao foi informada; valide o destino e a privacidade dos dados antes de exportar."
        );
    }

    private static Result checkDelete(
            Operation operation,
            String content,
            boolean confirmed) {

        if (content.trim().isEmpty()) {
            return result(
                    operation,
                    Decision.REJECT,
                    "DELETE_TARGET_MISSING",
                    "Identifique a memoria exata que deseja excluir."
            );
        }

        if (!confirmed) {
            return result(
                    operation,
                    Decision.REQUIRE_CONFIRMATION,
                    "DELETE_CONFIRMATION_REQUIRED",
                    "A exclusao exige confirmacao explicita do alvo especifico."
            );
        }

        /*
         * A confirmacao isolada nao identifica com seguranca
         * o registro real no banco. O modulo responsavel deve
         * resolver e validar o alvo antes de qualquer exclusao.
         */
        return result(
                operation,
                Decision.REQUIRE_REVIEW,
                "DELETE_TARGET_REVIEW_REQUIRED",
                "Confirme a identidade do registro e valide o alvo no banco antes de excluir."
        );
    }

    private static Result checkClearAll(
            Operation operation,
            boolean confirmed) {

        if (!confirmed) {
            return result(
                    operation,
                    Decision.REQUIRE_CONFIRMATION,
                    "CLEAR_ALL_CONFIRMATION_REQUIRED",
                    "Limpar toda a memoria exige uma confirmacao explicita e especifica."
            );
        }

        /*
         * Nao liberamos uma operacao destrutiva global apenas
         * com um booleano. O fluxo real precisa verificar
         * o escopo e a autorizacao especifica da acao.
         */
        return result(
                operation,
                Decision.REQUIRE_REVIEW,
                "CLEAR_ALL_REVIEW_REQUIRED",
                "A confirmacao foi informada; a operacao global continua bloqueada ate uma revisao especifica do escopo."
        );
    }

    /**
     * Rejeita caracteres de controle inesperados.
     * Tabulacao e quebras de linha sao aceitas para permitir
     * texto normal e dados estruturados.
     */
    private static boolean containsDisallowedControlCharacters(
            String value) {

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

    /**
     * Verifica se um resultado pode prosseguir segundo
     * este guard local. Nao executa a operacao.
     */
    public static boolean isAllowed(Result result) {
        return result != null && result.isAllowed();
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

    private static String safeText(
            String value,
            int maxLength,
            String fallback) {

        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        String cleaned = value.trim();

        if (cleaned.length() > maxLength) {
            return cleaned.substring(0, maxLength);
        }

        return cleaned;
    }
}
