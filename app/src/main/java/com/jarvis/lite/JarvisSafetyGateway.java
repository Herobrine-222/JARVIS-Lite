
package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JARVIS Lite — gateway local de verificacao de seguranca.
 *
 * Combina:
 * 1. JarvisSafetyPolicy: primeira avaliacao deterministica.
 * 2. JarvisKevinModule: segunda avaliacao heuristica.
 * 3. Resultado consolidado para o chamador.
 *
 * Este gateway nao executa ferramentas, nao acessa a rede,
 * nao altera o dispositivo e nao concede permissoes.
 *
 * CONTINUE_TO_NORMAL_PROCESSING nao e uma autorizacao para
 * executar uma acao externa. O chamador ainda deve validar
 * capacidades, permissoes, consentimento e o alvo da operacao.
 *
 * As regras sao heuristicas e nao garantem deteccao de todos
 * os pedidos perigosos ou de todas as tentativas de abuso.
 */
public final class JarvisSafetyGateway {

    private static final int MAX_INPUT_LENGTH = 6000;
    private static final int MAX_REASON_LENGTH = 100;
    private static final int MAX_MESSAGE_LENGTH = 600;
    private static final int MAX_FINDINGS = 30;
    private static final int MAX_FINDING_LENGTH = 400;

    private JarvisSafetyGateway() {
        // Classe utilitaria.
    }

    public enum Decision {
        CONTINUE_TO_NORMAL_PROCESSING,
        ASK_FOR_CLARIFICATION,
        REQUIRE_AUTHORIZATION,
        REVIEW_REQUIRED,
        REFUSE,
        REJECT_INPUT
    }

    /**
     * Resultado imutavel da avaliacao consolidada.
     */
    public static final class Result {

        private final Decision decision;
        private final String reasonCode;
        private final String message;
        private final JarvisSafetyPolicy.Category safetyCategory;
        private final JarvisKevinModule.RiskLevel riskLevel;
        private final List<String> findings;

        private Result(
                Decision decision,
                String reasonCode,
                String message,
                JarvisSafetyPolicy.Category safetyCategory,
                JarvisKevinModule.RiskLevel riskLevel,
                List<String> findings) {

            this.decision = decision == null
                    ? Decision.REVIEW_REQUIRED
                    : decision;

            this.reasonCode = safeText(
                    reasonCode,
                    MAX_REASON_LENGTH,
                    "UNSPECIFIED"
            );

            this.message = safeText(
                    message,
                    MAX_MESSAGE_LENGTH,
                    "A avaliacao de seguranca nao forneceu detalhes."
            );

            this.safetyCategory = safetyCategory == null
                    ? JarvisSafetyPolicy.Category.NONE
                    : safetyCategory;

            this.riskLevel = riskLevel == null
                    ? JarvisKevinModule.RiskLevel.HIGH
                    : riskLevel;

            this.findings = immutableFindings(findings);
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

        public JarvisSafetyPolicy.Category getSafetyCategory() {
            return safetyCategory;
        }

        public JarvisKevinModule.RiskLevel getRiskLevel() {
            return riskLevel;
        }

        public List<String> getFindings() {
            return findings;
        }

        /**
         * Significa apenas que as verificacoes locais nao
         * identificaram um bloqueio conhecido.
         *
         * Nao autoriza ferramentas nem operacoes externas.
         */
        public boolean mayContinueToNormalProcessing() {
            return decision
                    == Decision.CONTINUE_TO_NORMAL_PROCESSING;
        }

        public boolean requiresHumanOrCallerReview() {
            return decision == Decision.REVIEW_REQUIRED
                    || decision == Decision.REQUIRE_AUTHORIZATION;
        }

        public boolean requiresAuthorization() {
            return decision == Decision.REQUIRE_AUTHORIZATION;
        }

        public boolean requiresClarification() {
            return decision == Decision.ASK_FOR_CLARIFICATION;
        }

        public boolean shouldRefuse() {
            return decision == Decision.REFUSE;
        }

        public boolean isInputRejected() {
            return decision == Decision.REJECT_INPUT;
        }

        @Override
        public String toString() {
            return "Result{"
                    + "decision=" + decision
                    + ", reasonCode='" + reasonCode + '\''
                    + ", safetyCategory=" + safetyCategory
                    + ", riskLevel=" + riskLevel
                    + ", findingsCount=" + findings.size()
                    + '}';
        }
    }

    /**
     * Avalia uma entrada sem executar o conteudo.
     *
     * Em caso de falha inesperada, a decisao e REVIEW_REQUIRED.
     */
    public static Result evaluate(String input) {

        if (input == null || input.trim().isEmpty()) {
            return result(
                    Decision.ASK_FOR_CLARIFICATION,
                    "EMPTY_INPUT",
                    "Descreva o que voce gostaria que o JARVIS fizesse.",
                    JarvisSafetyPolicy.Category.NONE,
                    JarvisKevinModule.RiskLevel.LOW,
                    singleFinding(
                            "A solicitacao esta vazia ou contem apenas espacos."
                    )
            );
        }

        /*
         * Aplicar o limite antes de normalizar ou analisar
         * uma entrada potencialmente muito grande.
         */
        if (input.length() > MAX_INPUT_LENGTH) {
            return result(
                    Decision.REJECT_INPUT,
                    "INPUT_TOO_LONG",
                    "A solicitacao excede o limite de tamanho permitido. Reduza o texto e tente novamente.",
                    JarvisSafetyPolicy.Category.NONE,
                    JarvisKevinModule.RiskLevel.MEDIUM,
                    singleFinding(
                            "Entrada recusada antes da avaliacao das regras."
                    )
            );
        }

        if (containsDisallowedControlCharacters(input)) {
            return result(
                    Decision.REJECT_INPUT,
                    "DISALLOWED_CONTROL_CHARACTERS",
                    "A solicitacao contem caracteres inesperados. Revise o texto e tente novamente.",
                    JarvisSafetyPolicy.Category.NONE,
                    JarvisKevinModule.RiskLevel.MEDIUM,
                    singleFinding(
                            "A entrada contem caracteres de controle nao permitidos."
                    )
            );
        }

        final JarvisSafetyPolicy.Assessment policy;

        try {
            policy = JarvisSafetyPolicy.evaluate(input);
        } catch (RuntimeException error) {
            return failClosed(
                    "SAFETY_POLICY_FAILURE",
                    "Nao foi possivel concluir a primeira verificacao de seguranca.",
                    JarvisSafetyPolicy.Category.NONE,
                    "A politica local falhou; a solicitacao nao foi liberada.",
                    error
            );
        }

        if (policy == null) {
            return result(
                    Decision.REVIEW_REQUIRED,
                    "SAFETY_POLICY_NO_RESULT",
                    "Nao foi possivel avaliar a solicitacao com seguranca.",
                    JarvisSafetyPolicy.Category.NONE,
                    JarvisKevinModule.RiskLevel.HIGH,
                    singleFinding(
                            "A politica local nao retornou uma avaliacao utilizavel."
                    )
            );
        }

        if (policy.getDecision() == null) {
            return result(
                    Decision.REVIEW_REQUIRED,
                    "SAFETY_POLICY_INVALID_DECISION",
                    "A avaliacao de seguranca retornou uma decisao invalida.",
                    policy.getCategory(),
                    JarvisKevinModule.RiskLevel.HIGH,
                    singleFinding(
                            "A decisao da politica local estava ausente."
                    )
            );
        }

        /*
         * Recusa da politica tem prioridade.
         * Nao encaminhar a entrada ao segundo avaliador.
         */
        if (policy.getDecision()
                == JarvisSafetyPolicy.Decision.REFUSE) {

            return result(
                    Decision.REFUSE,
                    safeText(
                            policy.getReasonCode(),
                            MAX_REASON_LENGTH,
                            "POLICY_REFUSAL"
                    ),
                    safeText(
                            policy.getUserMessage(),
                            MAX_MESSAGE_LENGTH,
                            "Nao posso ajudar com essa solicitacao."
                    ),
                    policy.getCategory(),
                    JarvisKevinModule.RiskLevel.HIGH,
                    singleFinding(
                            "A politica local identificou uma solicitacao que deve ser recusada."
                    )
            );
        }

        /*
         * Revisao exigida pela politica nunca e convertida
         * em liberacao pelo avaliador seguinte.
         */
        if (policy.getDecision()
                == JarvisSafetyPolicy.Decision.REVIEW) {

            return result(
                    Decision.REVIEW_REQUIRED,
                    safeText(
                            policy.getReasonCode(),
                            MAX_REASON_LENGTH,
                            "POLICY_REVIEW_REQUIRED"
                    ),
                    safeText(
                            policy.getUserMessage(),
                            MAX_MESSAGE_LENGTH,
                            "A solicitacao precisa ser revisada antes de prosseguir."
                    ),
                    policy.getCategory(),
                    JarvisKevinModule.RiskLevel.HIGH,
                    singleFinding(
                            "A politica local solicitou revisao antes do encaminhamento."
                    )
            );
        }

        if (policy.getDecision()
                != JarvisSafetyPolicy.Decision.ALLOW) {

            return result(
                    Decision.REVIEW_REQUIRED,
                    "UNSUPPORTED_POLICY_DECISION",
                    "A politica retornou uma decisao que o gateway nao reconhece.",
                    policy.getCategory(),
                    JarvisKevinModule.RiskLevel.HIGH,
                    singleFinding(
                            "Decisao da politica nao reconhecida; liberacao bloqueada."
                    )
            );
        }

        final JarvisKevinModule.RequestAssessment kevin;

        try {
            kevin = JarvisKevinModule.assessRequest(input);
        } catch (RuntimeException error) {
            return failClosed(
                    "KEVIN_ASSESSMENT_FAILURE",
                    "Nao foi possivel concluir a segunda verificacao de seguranca.",
                    policy.getCategory(),
                    "A avaliacao complementar falhou; a solicitacao nao foi liberada.",
                    error
            );
        }

        if (kevin == null || kevin.getDecision() == null) {
            return result(
                    Decision.REVIEW_REQUIRED,
                    "KEVIN_NO_VALID_RESULT",
                    "A avaliacao complementar nao retornou um resultado valido.",
                    policy.getCategory(),
                    JarvisKevinModule.RiskLevel.HIGH,
                    singleFinding(
                            "A avaliacao complementar estava ausente ou incompleta."
                    )
            );
        }

        List<String> findings = new ArrayList<>();
        addFinding(
                findings,
                "A primeira politica local nao identificou uma regra de recusa."
        );
        addFindings(findings, kevin.getFindings());

        Decision decision;
        String message;
        JarvisKevinModule.RiskLevel risk = kevin.getRiskLevel();

        if (risk == null) {
            risk = JarvisKevinModule.RiskLevel.HIGH;
            addFinding(
                    findings,
                    "O nivel de risco estava ausente; foi adotada uma classificacao conservadora."
            );
        }

        switch (kevin.getDecision()) {

            case READY_FOR_REVIEW:
                decision = Decision.CONTINUE_TO_NORMAL_PROCESSING;
                message =
                        "As regras locais nao encontraram um bloqueio conhecido. "
                        + "Isso nao autoriza acoes externas.";
                break;

            case ASK_FOR_CLARIFICATION:
                decision = Decision.ASK_FOR_CLARIFICATION;
                message = safeText(
                        kevin.getSummary(),
                        MAX_MESSAGE_LENGTH,
                        "Peca esclarecimento antes de encaminhar a solicitacao."
                );
                break;

            case ACTION_REQUIRES_AUTHORIZATION:
                decision = Decision.REQUIRE_AUTHORIZATION;
                message = safeText(
                        kevin.getSummary(),
                        MAX_MESSAGE_LENGTH,
                        "A solicitacao exige autorizacao especifica antes de qualquer acao."
                );
                break;

            case INPUT_REJECTED:
                decision = Decision.REJECT_INPUT;
                message = safeText(
                        kevin.getSummary(),
                        MAX_MESSAGE_LENGTH,
                        "A entrada foi recusada pela verificacao complementar."
                );
                break;

            case POLICY_REVIEW_REQUIRED:
            default:
                decision = Decision.REVIEW_REQUIRED;
                message = safeText(
                        kevin.getSummary(),
                        MAX_MESSAGE_LENGTH,
                        "A solicitacao precisa ser revisada antes de prosseguir."
                );
                break;
        }

        /*
         * Evita liberar uma solicitacao classificada como
         * critica mesmo se o avaliador devolver READY.
         */
        if (risk == JarvisKevinModule.RiskLevel.CRITICAL
                && decision == Decision.CONTINUE_TO_NORMAL_PROCESSING) {

            decision = Decision.REVIEW_REQUIRED;
            message =
                    "A solicitacao foi encaminhada para revisao devido ao nivel de risco critico.";

            addFinding(
                    findings,
                    "O nivel de risco critico impede a continuacao automatica."
            );
        }

        return result(
                decision,
                "LOCAL_RULES_" + kevin.getDecision().name(),
                message,
                policy.getCategory(),
                risk,
                findings
        );
    }

    private static Result failClosed(
            String reasonCode,
            String message,
            JarvisSafetyPolicy.Category category,
            String finding,
            RuntimeException error) {

        List<String> findings = new ArrayList<>();
        addFinding(findings, finding);

        /*
         * Nao incluir mensagens de excecao, que podem conter
         * dados internos ou texto da entrada.
         */
        if (error != null) {
            addFinding(
                    findings,
                    "Tipo de falha: "
                            + error.getClass().getSimpleName()
                            + "."
            );
        }

        return result(
                Decision.REVIEW_REQUIRED,
                reasonCode,
                message,
                category,
                JarvisKevinModule.RiskLevel.HIGH,
                findings
        );
    }

    private static Result result(
            Decision decision,
            String reasonCode,
            String message,
            JarvisSafetyPolicy.Category category,
            JarvisKevinModule.RiskLevel risk,
            List<String> findings) {

        return new Result(
                decision,
                reasonCode,
                message,
                category,
                risk,
                findings
        );
    }

    private static List<String> singleFinding(String finding) {
        List<String> findings = new ArrayList<>();
        addFinding(findings, finding);
        return findings;
    }

    private static void addFindings(
            List<String> destination,
            List<String> source) {

        if (destination == null || source == null) {
            return;
        }

        for (String finding : source) {
            if (destination.size() >= MAX_FINDINGS) {
                return;
            }

            addFinding(destination, finding);
        }
    }

    private static void addFinding(
            List<String> findings,
            String finding) {

        if (findings == null || findings.size() >= MAX_FINDINGS) {
            return;
        }

        String cleaned = safeText(
                finding,
                MAX_FINDING_LENGTH,
                ""
        );

        if (!cleaned.isEmpty()) {
            findings.add(cleaned);
        }
    }

    private static List<String> immutableFindings(
            List<String> source) {

        List<String> copy = new ArrayList<>();

        if (source != null) {
            for (String finding : source) {
                if (copy.size() >= MAX_FINDINGS) {
                    break;
                }

                String cleaned = safeText(
                        finding,
                        MAX_FINDING_LENGTH,
                        ""
                );

                if (!cleaned.isEmpty()) {
                    copy.add(cleaned);
                }
            }
        }

        return Collections.unmodifiableList(copy);
    }

    private static boolean containsDisallowedControlCharacters(
            String input) {

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (Character.isISOControl(c)
                    && c != '\n'
                    && c != '\r'
                    && c != '\t') {
                return true;
            }
        }

        return false;
    }

    private static String safeText(
            String value,
            int maxLength,
            String fallback) {

        if (value == null) {
            return fallback;
        }

        String cleaned = value.trim();

        if (cleaned.isEmpty()) {
            return fallback;
        }

        if (cleaned.length() > maxLength) {
            return cleaned.substring(0, maxLength);
        }

        return cleaned;
    }
}
