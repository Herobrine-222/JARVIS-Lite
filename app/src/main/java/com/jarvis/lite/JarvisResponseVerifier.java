
package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Revisa respostas em busca de problemas evidentes.
 *
 * Este modulo usa verificacoes heuristicas. Um resultado sem
 * alertas nao prova que a resposta esta correta.
 */
public final class JarvisResponseVerifier {

    public static final class Report {
        private final boolean reviewRequired;
        private final JarvisKevinModule.RiskLevel riskLevel;
        private final List<String> findings;

        private Report(
                boolean reviewRequired,
                JarvisKevinModule.RiskLevel riskLevel,
                List<String> findings) {

            this.reviewRequired = reviewRequired;
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

        public boolean isReviewRequired() {
            return reviewRequired;
        }

        public JarvisKevinModule.RiskLevel getRiskLevel() {
            return riskLevel;
        }

        public List<String> getFindings() {
            return findings;
        }

        public boolean isClearOfDetectedWarnings() {
            return !reviewRequired && findings.isEmpty();
        }
    }

    private JarvisResponseVerifier() {
        // Classe utilitaria, sem estado persistente.
    }

    public static Report verify(
            String userRequest,
            String proposedResponse) {

        return verify(userRequest, proposedResponse, null);
    }

    public static Report verify(
            String userRequest,
            String proposedResponse,
            JarvisKevinModule.ExecutionEvidence evidence) {

        final JarvisKevinModule.ResponseAssessment assessment;

        try {
            assessment = JarvisKevinModule.reviewResponse(
                    userRequest,
                    proposedResponse,
                    evidence
            );
        } catch (RuntimeException error) {
            List<String> findings = new ArrayList<>();

            findings.add(
                    "A revisao falhou; a resposta deve ser tratada como nao verificada."
            );

            findings.add(
                    "Tipo de falha: "
                            + error.getClass().getSimpleName()
            );

            return new Report(
                    true,
                    JarvisKevinModule.RiskLevel.HIGH,
                    findings
            );
        }

        if (assessment == null) {
            List<String> findings = new ArrayList<>();

            findings.add(
                    "O revisor nao retornou um resultado utilizavel."
            );

            return new Report(
                    true,
                    JarvisKevinModule.RiskLevel.HIGH,
                    findings
            );
        }

        return new Report(
                assessment.isReviewRequired(),
                assessment.getRiskLevel(),
                assessment.getFindings()
        );
    }
}
