
package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Camada central de confiabilidade do JARVIS Lite.
 *
 * Responsabilidades:
 * - Consultar o gateway de segurança antes de liberar uma resposta.
 * - Revisar a resposta com o verificador existente.
 * - Avaliar evidências fornecidas por outros módulos.
 * - Diferenciar ausência de alertas de comprovação factual.
 * - Falhar de forma conservadora quando uma verificação falha.
 *
 * Este módulo não executa ações, não acessa a internet,
 * não concede permissões e não garante que uma resposta seja verdadeira.
 */
public final class JarvisReliabilityCore {

    public static final int MAX_REQUEST_LENGTH = 6000;
    public static final int MAX_RESPONSE_LENGTH = 12000;

    private JarvisReliabilityCore() {
        // Classe utilitária sem estado global mutável.
    }

    public enum Status {
        APPROVED_WITH_LIMITS,
        NEEDS_REVIEW,
        INSUFFICIENT_EVIDENCE,
        BLOCKED,
        INVALID_INPUT,
        VERIFICATION_FAILURE
    }

    public enum EvidenceStatus {
        SUPPORTED,
        CONTRADICTED,
        MIXED,
        INSUFFICIENT_EVIDENCE,
        INVALID_INPUT,
        VERIFICATION_FAILURE
    }

    public static final class ReliabilityReport {
        private final Status status;
        private final JarvisKevinModule.RiskLevel riskLevel;
        private final String safeResponse;
        private final boolean originalResponseReleased;
        private final boolean factualAccuracyEstablished;
        private final List<String> findings;

        private ReliabilityReport(
                Status status,
                JarvisKevinModule.RiskLevel riskLevel,
                String safeResponse,
                boolean originalResponseReleased,
                boolean factualAccuracyEstablished,
                List<String> findings) {

            this.status = status == null
                    ? Status.VERIFICATION_FAILURE
                    : status;

            this.riskLevel = riskLevel == null
                    ? JarvisKevinModule.RiskLevel.HIGH
                    : riskLevel;

            this.safeResponse = clean(
                    safeResponse,
                    MAX_RESPONSE_LENGTH
            );

            this.originalResponseReleased =
                    originalResponseReleased;

            this.factualAccuracyEstablished =
                    factualAccuracyEstablished;

            this.findings = immutableCopy(findings);
        }

        public Status getStatus() {
            return status;
        }

        public JarvisKevinModule.RiskLevel getRiskLevel() {
            return riskLevel;
        }

        public String getSafeResponse() {
            return safeResponse;
        }

        public boolean isOriginalResponseReleased() {
            return originalResponseReleased;
        }

        /**
         * Só será true se houver uma avaliação factual explícita
         * e adequada. A simples ausência de alertas nunca basta.
         */
        public boolean isFactualAccuracyEstablished() {
            return factualAccuracyEstablished;
        }

        public List<String> getFindings() {
            return findings;
        }

        public boolean requiresReview() {
            return status == Status.NEEDS_REVIEW
                    || status == Status.VERIFICATION_FAILURE;
        }

        public boolean isApprovedWithLimits() {
            return status == Status.APPROVED_WITH_LIMITS;
        }
    }

    public static final class EvidenceReport {
        private final EvidenceStatus status;
        private final double heuristicConfidence;
        private final String explanation;
        private final List<String> findings;

        private EvidenceReport(
                EvidenceStatus status,
                double heuristicConfidence,
                String explanation,
                List<String> findings) {

            this.status = status == null
                    ? EvidenceStatus.VERIFICATION_FAILURE
                    : status;

            this.heuristicConfidence = clamp(
                    heuristicConfidence
            );

            this.explanation = clean(explanation, 1000);
            this.findings = immutableCopy(findings);
        }

        public EvidenceStatus getStatus() {
            return status;
        }

        /**
         * Indicador heurístico do avaliador local.
         * Não representa probabilidade matemática de verdade.
         */
        public double getHeuristicConfidence() {
            return heuristicConfidence;
        }

        public String getExplanation() {
            return explanation;
        }

        public List<String> getFindings() {
            return findings;
        }

        public boolean hasSufficientEvidence() {
            return status == EvidenceStatus.SUPPORTED;
        }
    }

    /**
     * Avalia uma resposta proposta.
     *
     * A resposta original só é liberada quando:
     * 1. A entrada é válida.
     * 2. O gateway permite processamento normal.
     * 3. O verificador não encontra alertas nem exige revisão.
     *
     * Mesmo nesse caso, a aprovação é limitada: ela não prova
     * que todo o conteúdo factual está correto.
     */
    public static ReliabilityReport evaluateResponse(
            String userRequest,
            String proposedResponse) {

        List<String> findings = new ArrayList<String>();

        if (userRequest == null
                || userRequest.trim().isEmpty()) {

            findings.add(
                    "O pedido está vazio; é necessário esclarecer a solicitação."
            );

            return report(
                    Status.INVALID_INPUT,
                    JarvisKevinModule.RiskLevel.LOW,
                    "Descreva o que você gostaria que o JARVIS fizesse.",
                    false,
                    false,
                    findings
            );
        }

        if (userRequest.length() > MAX_REQUEST_LENGTH) {
            findings.add(
                    "O pedido excede o limite de análise."
            );

            return report(
                    Status.INVALID_INPUT,
                    JarvisKevinModule.RiskLevel.MEDIUM,
                    "A solicitação é muito longa. Reduza o texto e tente novamente.",
                    false,
                    false,
                    findings
            );
        }

        if (proposedResponse == null
                || proposedResponse.trim().isEmpty()) {

            findings.add(
                    "Não foi fornecida uma resposta para revisar."
            );

            return report(
                    Status.INVALID_INPUT,
                    JarvisKevinModule.RiskLevel.LOW,
                    "Ainda não tenho uma resposta utilizável para apresentar.",
                    false,
                    false,
                    findings
            );
        }

        if (proposedResponse.length() > MAX_RESPONSE_LENGTH) {
            findings.add(
                    "A resposta excede o limite de revisão."
            );

            return report(
                    Status.INVALID_INPUT,
                    JarvisKevinModule.RiskLevel.MEDIUM,
                    "A resposta ficou muito longa para ser revisada com segurança.",
                    false,
                    false,
                    findings
            );
        }

        final JarvisSafetyGateway.Result gatewayResult;

        try {
            gatewayResult = JarvisSafetyGateway.evaluate(
                    userRequest
            );
        } catch (RuntimeException error) {
            findings.add(
                    "O gateway de segurança falhou; a resposta não foi liberada."
            );
            findings.add(
                    "Tipo de falha: "
                            + error.getClass().getSimpleName()
            );

            return report(
                    Status.VERIFICATION_FAILURE,
                    JarvisKevinModule.RiskLevel.HIGH,
                    "Não consegui concluir a verificação de segurança. "
                            + "A resposta precisa ser revisada antes de ser apresentada.",
                    false,
                    false,
                    findings
            );
        }

        if (gatewayResult == null) {
            findings.add(
                    "O gateway não retornou um resultado utilizável."
            );

            return report(
                    Status.VERIFICATION_FAILURE,
                    JarvisKevinModule.RiskLevel.HIGH,
                    "Não foi possível verificar a solicitação.",
                    false,
                    false,
                    findings
            );
        }

        findings.addAll(gatewayResult.getFindings());

        switch (gatewayResult.getDecision()) {
            case REFUSE:
                findings.add(
                        "A solicitação foi recusada pela camada de segurança."
                );

                return report(
                        Status.BLOCKED,
                        gatewayResult.getRiskLevel(),
                        nonEmpty(
                                gatewayResult.getMessage(),
                                "Não posso atender a essa solicitação."
                        ),
                        false,
                        false,
                        findings
                );

            case REJECT_INPUT:
                return report(
                        Status.INVALID_INPUT,
                        gatewayResult.getRiskLevel(),
                        nonEmpty(
                                gatewayResult.getMessage(),
                                "Revise a solicitação e tente novamente."
                        ),
                        false,
                        false,
                        findings
                );

            case ASK_FOR_CLARIFICATION:
                return report(
                        Status.NEEDS_REVIEW,
                        gatewayResult.getRiskLevel(),
                        nonEmpty(
                                gatewayResult.getMessage(),
                                "Preciso de mais informações para continuar."
                        ),
                        false,
                        false,
                        findings
                );

            case REQUIRE_AUTHORIZATION:
                findings.add(
                        "Uma autorização específica é necessária antes de qualquer ação."
                );

                return report(
                        Status.NEEDS_REVIEW,
                        gatewayResult.getRiskLevel(),
                        "A solicitação exige autorização antes de prosseguir. "
                                + "Nenhuma ação foi executada por esta camada.",
                        false,
                        false,
                        findings
                );

            case REVIEW_REQUIRED:
                findings.add(
                        "O gateway exige revisão adicional."
                );

                return report(
                        Status.NEEDS_REVIEW,
                        gatewayResult.getRiskLevel(),
                        "A solicitação precisa de uma revisão adicional antes de prosseguir.",
                        false,
                        false,
                        findings
                );

            case CONTINUE_TO_NORMAL_PROCESSING:
                // Prossegue apenas para revisar a resposta.
                // Isso não autoriza ações no dispositivo.
                break;

            default:
                findings.add(
                        "O gateway retornou uma decisão não reconhecida."
                );

                return report(
                        Status.VERIFICATION_FAILURE,
                        JarvisKevinModule.RiskLevel.HIGH,
                        "Não foi possível interpretar a decisão de segurança.",
                        false,
                        false,
                        findings
                );
        }

        final JarvisResponseVerifier.Report verification;

        try {
            verification = JarvisResponseVerifier.verify(
                    userRequest,
                    proposedResponse
            );
        } catch (RuntimeException error) {
            findings.add(
                    "O verificador de respostas falhou."
            );
            findings.add(
                    "Tipo de falha: "
                            + error.getClass().getSimpleName()
            );

            return report(
                    Status.VERIFICATION_FAILURE,
                    JarvisKevinModule.RiskLevel.HIGH,
                    "Não consegui verificar a resposta. "
                            + "Ela não será liberada por esta camada.",
                    false,
                    false,
                    findings
            );
        }

        if (verification == null) {
            findings.add(
                    "O verificador não retornou um relatório."
            );

            return report(
                    Status.VERIFICATION_FAILURE,
                    JarvisKevinModule.RiskLevel.HIGH,
                    "A revisão não produziu um resultado utilizável.",
                    false,
                    false,
                    findings
            );
        }

        findings.addAll(verification.getFindings());

        JarvisKevinModule.RiskLevel combinedRisk = maxRisk(
                gatewayResult.getRiskLevel(),
                verification.getRiskLevel()
        );

        if (verification.isReviewRequired()
                || !verification.getFindings().isEmpty()) {

            findings.add(
                    "A resposta contém alertas detectados ou exige revisão."
            );

            return report(
                    Status.NEEDS_REVIEW,
                    combinedRisk,
                    "Encontrei pontos que precisam ser revisados. "
                            + "Não vou apresentar a resposta como verificada.",
                    false,
                    false,
                    findings
            );
        }

        findings.add(
                "Nenhum alerta foi identificado pelos verificadores locais."
        );
        findings.add(
                "A ausência de alertas não comprova a exatidão factual."
        );

        return report(
                Status.APPROVED_WITH_LIMITS,
                combinedRisk,
                proposedResponse.trim(),
                true,
                false,
                findings
        );
    }

    /**
     * Avalia uma afirmação usando o motor factual já existente.
     *
     * As evidências precisam ser fornecidas pelo chamador.
     * Este método não inventa fontes nem verifica automaticamente
     * se as evidências fornecidas são autênticas.
     */
    public static EvidenceReport evaluateEvidence(
            String claim,
            List<JarvisFactCheckEngine.Evidence> evidence) {

        List<String> findings = new ArrayList<String>();

        if (claim == null || claim.trim().isEmpty()) {
            findings.add(
                    "A afirmação está vazia."
            );

            return evidenceReport(
                    EvidenceStatus.INVALID_INPUT,
                    0.0,
                    "Forneça uma afirmação para avaliar.",
                    findings
            );
        }

        if (claim.length()
                > JarvisFactCheckEngine.MAX_CLAIM_LENGTH) {

            findings.add(
                    "A afirmação excede o limite do motor factual."
            );

            return evidenceReport(
                    EvidenceStatus.INVALID_INPUT,
                    0.0,
                    "A afirmação é muito longa para ser avaliada.",
                    findings
            );
        }

        final JarvisFactCheckEngine.Assessment assessment;

        try {
            assessment = new JarvisFactCheckEngine().assess(
                    claim,
                    evidence
            );
        } catch (RuntimeException error) {
            findings.add(
                    "O motor de verificação factual falhou."
            );
            findings.add(
                    "Tipo de falha: "
                            + error.getClass().getSimpleName()
            );

            return evidenceReport(
                    EvidenceStatus.VERIFICATION_FAILURE,
                    0.0,
                    "Não foi possível concluir a avaliação das evidências.",
                    findings
            );
        }

        if (assessment == null
                || assessment.getVerdict() == null) {

            findings.add(
                    "O motor factual não retornou uma avaliação utilizável."
            );

            return evidenceReport(
                    EvidenceStatus.VERIFICATION_FAILURE,
                    0.0,
                    "A avaliação factual não produziu um resultado válido.",
                    findings
            );
        }

        findings.add(assessment.getExplanation());

        EvidenceStatus status;

        switch (assessment.getVerdict()) {
            case SUPPORTED:
                status = EvidenceStatus.SUPPORTED;
                findings.add(
                        "A classificação indica suporte nas evidências fornecidas, "
                                + "mas não garante a verdade da afirmação."
                );
                break;

            case CONTRADICTED:
                status = EvidenceStatus.CONTRADICTED;
                findings.add(
                        "Há evidências classificadas como contraditórias à afirmação."
                );
                break;

            case MIXED:
                status = EvidenceStatus.MIXED;
                findings.add(
                        "Existem evidências em direções opostas."
                );
                break;

            case INSUFFICIENT_EVIDENCE:
                status = EvidenceStatus.INSUFFICIENT_EVIDENCE;
                findings.add(
                        "Não há suporte suficiente para tratar a afirmação como confirmada."
                );
                break;

            case INVALID_INPUT:
            default:
                status = EvidenceStatus.INVALID_INPUT;
                break;
        }

        return evidenceReport(
                status,
                assessment.getConfidence(),
                assessment.getExplanation(),
                findings
        );
    }

    private static ReliabilityReport report(
            Status status,
            JarvisKevinModule.RiskLevel risk,
            String response,
            boolean originalReleased,
            boolean factualAccuracyEstablished,
            List<String> findings) {

        return new ReliabilityReport(
                status,
                risk,
                response,
                originalReleased,
                factualAccuracyEstablished,
                findings
        );
    }

    private static EvidenceReport evidenceReport(
            EvidenceStatus status,
            double confidence,
            String explanation,
            List<String> findings) {

        return new EvidenceReport(
                status,
                confidence,
                explanation,
                findings
        );
    }

    private static List<String> immutableCopy(
            List<String> source) {

        List<String> copy = new ArrayList<String>();

        if (source != null) {
            for (String item : source) {
                if (item != null && !item.trim().isEmpty()) {
                    copy.add(clean(item, 1000));
                }
            }
        }

        return Collections.unmodifiableList(copy);
    }

    private static String clean(
            String value,
            int maxLength) {

        if (value == null) {
            return "";
        }

        String result = value.trim();

        if (result.length() > maxLength) {
            result = result.substring(0, maxLength);
        }

        return result;
    }

    private static String nonEmpty(
            String value,
            String fallback) {

        return value == null || value.trim().isEmpty()
                ? fallback
                : clean(value, MAX_RESPONSE_LENGTH);
    }

    private static double clamp(double value) {
        if (Double.isNaN(value)
                || Double.isInfinite(value)) {
            return 0.0;
        }

        return Math.max(0.0, Math.min(1.0, value));
    }

    private static JarvisKevinModule.RiskLevel maxRisk(
            JarvisKevinModule.RiskLevel first,
            JarvisKevinModule.RiskLevel second) {

        JarvisKevinModule.RiskLevel a = first == null
                ? JarvisKevinModule.RiskLevel.HIGH
                : first;

        JarvisKevinModule.RiskLevel b = second == null
                ? JarvisKevinModule.RiskLevel.HIGH
                : second;

        return riskRank(a) >= riskRank(b) ? a : b;
    }

    private static int riskRank(
            JarvisKevinModule.RiskLevel risk) {

        switch (risk) {
            case LOW:
                return 0;
            case MEDIUM:
                return 1;
            case HIGH:
                return 2;
            case CRITICAL:
                return 3;
            default:
                return 3;
        }
    }
}
