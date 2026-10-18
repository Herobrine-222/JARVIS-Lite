package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Combina as verificacoes locais de seguranca.
 *
 * Este gateway nao executa ferramentas, nao concede permissoes
 * Android e nao garante que uma solicitacao seja segura.
 *
 * CONTINUE_TO_NORMAL_PROCESSING significa apenas que as regras
 * locais nao encontraram um bloqueio conhecido. Acoes reais ainda
 * exigem autorizacao e verificacoes especificas.
 */
public final class JarvisSafetyGateway {

    public enum Decision {
        CONTINUE_TO_NORMAL_PROCESSING,
        ASK_FOR_CLARIFICATION,
        REQUIRE_AUTHORIZATION,
        REVIEW_REQUIRED,
        REFUSE,
        REJECT_INPUT
    }

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
                    ? Decision.REVIEW_REQUIRED : decision;

            this.reasonCode = safe(reasonCode, 100);
            this.message = safe(message, 600);

            this.safetyCategory = safetyCategory == null
                    ? JarvisSafetyPolicy.Category.NONE
                    : safetyCategory;

            this.riskLevel = riskLevel == null
                    ? JarvisKevinModule.RiskLevel.MEDIUM
                    : riskLevel;

            this.findings = Collections.unmodifiableList(
                    new ArrayList<>(
                            findings == null
                                    ? Collections.<String>emptyList()
                                    : findings
                    )
            );
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

        public boolean mayContinueToNormalProcessing() {
            return decision
                    == Decision.CONTINUE_TO_NORMAL_PROCESSING;
        }

        public boolean requiresHumanOrCallerReview() {
            return decision == Decision.REVIEW_REQUIRED
                    || decision == Decision.REQUIRE_AUTHORIZATION;
        }
    }

    private JarvisSafetyGateway() {
        // Classe utilitaria, sem estado persistente.
    }

    public static Result evaluate(String input) {
        if (input == null || input.trim().isEmpty()) {
            List<String> findings = new ArrayList<>();
            findings.add(
                    "A solicitacao esta vazia; peca esclarecimento."
            );

            return result(
                    Decision.ASK_FOR_CLARIFICATION,
                    "EMPTY_INPUT",
                    "Descreva o que voce gostaria que o JARVIS fizesse.",
                    JarvisSafetyPolicy.Category.NONE,
                    JarvisKevinModule.RiskLevel.LOW,
                    findings
            );
        }

        final JarvisSafetyPolicy.Assessment policy;

        try {
            policy = JarvisSafetyPolicy.evaluate(input);
        } catch (RuntimeException error) {
            List<String> findings = new ArrayList<>();
            findings.add(
                    "A politica local falhou; o pedido nao foi encaminhado."
            );
            findings.add(
                    "Tipo de falha: "
                            + error.getClass().getSimpleName()
            );

            return result(
                    Decision.REVIEW_REQUIRED,
                    "SAFETY_POLICY_FAILURE",
                    "Nao foi possivel concluir a verificacao local.",
                    JarvisSafetyPolicy.Category.NONE,
                    JarvisKevinModule.RiskLevel.HIGH,
                    findings
            );
        }

        if (policy == null) {
            List<String> findings = new ArrayList<>();
            findings.add(
                    "A politica local nao retornou uma avaliacao."
            );

            return result(
                    Decision.REVIEW_REQUIRED,
                    "SAFETY_POLICY_NO_RESULT",
                    "Nao foi possivel avaliar a solicitacao.",
                    JarvisSafetyPolicy.Category.NONE,
                    JarvisKevinModule.RiskLevel.HIGH,
                    findings
            );
        }

        if (policy.getDecision()
                == JarvisSafetyPolicy.Decision.REFUSE) {

            List<String> findings = new ArrayList<>();
            findings.add(
                    "A politica local identificou um pedido que deve ser recusado."
            );

            return result(
                    Decision.REFUSE,
                    policy.getReasonCode(),
                    policy.getUserMessage(),
                    policy.getCategory(),
                    JarvisKevinModule.RiskLevel.HIGH,
                    findings
            );
        }

        if (policy.getDecision()
                == JarvisSafetyPolicy.Decision.REVIEW) {

            List<String> findings = new ArrayList<>();
            findings.add(
                    "A politica local solicitou revisao antes do encaminhamento."
            );

            return result(
                    Decision.REVIEW_REQUIRED,
                    policy.getReasonCode(),
                    policy.getUserMessage(),
                    policy.getCategory(),
                    JarvisKevinModule.RiskLevel.HIGH,
                    findings
            );
        }

        final JarvisKevinModule.RequestAssessment kevin;

        try {
            kevin = JarvisKevinModule.assessRequest(input);
        } catch (RuntimeException error) {
            List<String> findings = new ArrayList<>();
            findings.add(
                    "A avaliacao de coordenacao falhou; o pedido nao foi encaminhado."
            );
            findings.add(
                    "Tipo de falha: "
                            + error.getClass().getSimpleName()
            );

            return result(
                    Decision.REVIEW_REQUIRED,
                    "KEVIN_ASSESSMENT_FAILURE",
                    "Nao foi possivel revisar a solicitacao.",
                    policy.getCategory(),
                    JarvisKevinModule.RiskLevel.HIGH,
                    findings
            );
        }

        if (kevin == null) {
            List<String> findings = new ArrayList<>();
            findings.add(
                    "Kevin nao retornou uma avaliacao utilizavel."
            );

            return result(
                    Decision.REVIEW_REQUIRED,
                    "KEVIN_NO_RESULT",
                    "Nao foi possivel revisar a solicitacao.",
                    policy.getCategory(),
                    JarvisKevinModule.RiskLevel.HIGH,
                    findings
            );
        }

        List<String> findings =
                new ArrayList<>(kevin.getFindings());

        Decision decision;
        String message = kevin.getSummary();
        JarvisKevinModule.RiskLevel risk = kevin.getRiskLevel();

        switch (kevin.getDecision()) {
            case READY_FOR_REVIEW:
                decision = Decision.CONTINUE_TO_NORMAL_PROCESSING;
                message =
                        "As regras locais nao encontraram bloqueio conhecido. "
                        + "Isso nao autoriza acoes externas.";
                break;

            case ASK_FOR_CLARIFICATION:
                decision = Decision.ASK_FOR_CLARIFICATION;
                break;

            case ACTION_REQUIRES_AUTHORIZATION:
                decision = Decision.REQUIRE_AUTHORIZATION;
                break;

            case INPUT_REJECTED:
                decision = Decision.REJECT_INPUT;
                break;

            case POLICY_REVIEW_REQUIRED:
            default:
                decision = Decision.REVIEW_REQUIRED;
                break;
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

    private static String safe(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        String trimmed = value.trim();

        return trimmed.length() <= maxLength
                ? trimmed
                : trimmed.substring(0, maxLength);
    }
}
