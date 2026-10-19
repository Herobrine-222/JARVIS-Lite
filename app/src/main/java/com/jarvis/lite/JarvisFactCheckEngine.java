
package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Avalia afirmações usando evidências fornecidas por outros módulos.
 *
 * Não acessa a internet, não inventa fontes e não garante
 * que uma afirmação seja verdadeira apenas por estar bem escrita.
 */
public final class JarvisFactCheckEngine {

    public static final int MAX_CLAIM_LENGTH = 2000;
    public static final int MAX_EVIDENCE_LENGTH = 4000;
    public static final int MAX_EVIDENCE_ITEMS = 100;

    public enum Verdict {
        SUPPORTED,
        CONTRADICTED,
        INSUFFICIENT_EVIDENCE,
        MIXED,
        INVALID_INPUT
    }

    public enum EvidenceType {
        TRUSTED_SOURCE,
        TOOL_RESULT,
        LOCAL_KNOWLEDGE,
        USER_PROVIDED,
        INFERENCE,
        UNKNOWN
    }

    public static final class Evidence {

        private final String id;
        private final String statement;
        private final EvidenceType type;
        private final boolean supportsClaim;

        public Evidence(
                String id,
                String statement,
                EvidenceType type,
                boolean supportsClaim
        ) {
            this.id = safeText(id, 100);
            this.statement = safeText(
                    statement,
                    MAX_EVIDENCE_LENGTH
            );
            this.type = type == null
                    ? EvidenceType.UNKNOWN
                    : type;
            this.supportsClaim = supportsClaim;
        }

        public String getId() {
            return id;
        }

        public String getStatement() {
            return statement;
        }

        public EvidenceType getType() {
            return type;
        }

        public boolean supportsClaim() {
            return supportsClaim;
        }
    }

    public static final class Assessment {

        private final Verdict verdict;
        private final String claim;
        private final double confidence;
        private final List<Evidence> supportingEvidence;
        private final List<Evidence> contradictingEvidence;
        private final List<Evidence> otherEvidence;
        private final String explanation;

        private Assessment(
                Verdict verdict,
                String claim,
                double confidence,
                List<Evidence> supportingEvidence,
                List<Evidence> contradictingEvidence,
                List<Evidence> otherEvidence,
                String explanation
        ) {
            this.verdict = verdict;
            this.claim = claim;
            this.confidence = clamp(confidence);

            this.supportingEvidence = immutableCopy(
                    supportingEvidence
            );

            this.contradictingEvidence = immutableCopy(
                    contradictingEvidence
            );

            this.otherEvidence = immutableCopy(otherEvidence);
            this.explanation = explanation;
        }

        public Verdict getVerdict() {
            return verdict;
        }

        public String getClaim() {
            return claim;
        }

        /**
         * Indica a força da avaliação heurística,
         * não uma probabilidade matemática de verdade.
         */
        public double getConfidence() {
            return confidence;
        }

        public List<Evidence> getSupportingEvidence() {
            return supportingEvidence;
        }

        public List<Evidence> getContradictingEvidence() {
            return contradictingEvidence;
        }

        public List<Evidence> getOtherEvidence() {
            return otherEvidence;
        }

        public String getExplanation() {
            return explanation;
        }

        public boolean hasConflict() {
            return !supportingEvidence.isEmpty()
                    && !contradictingEvidence.isEmpty();
        }
    }

    /**
     * Avalia uma afirmação a partir das evidências fornecidas.
     *
     * A classificação de cada evidência é informada por quem
     * chama este método; não é verificada automaticamente.
     */
    public Assessment assess(
            String claim,
            List<Evidence> evidenceList
    ) {
        String safeClaim = safeText(claim, MAX_CLAIM_LENGTH);

        if (safeClaim.isEmpty()
                || claim == null
                || claim.trim().length() > MAX_CLAIM_LENGTH) {
            return invalidAssessment(
                    safeClaim,
                    "A afirmação está vazia ou excede o limite."
            );
        }

        if (evidenceList == null || evidenceList.isEmpty()) {
            return new Assessment(
                    Verdict.INSUFFICIENT_EVIDENCE,
                    safeClaim,
                    0.0,
                    Collections.<Evidence>emptyList(),
                    Collections.<Evidence>emptyList(),
                    Collections.<Evidence>emptyList(),
                    "Não foram fornecidas evidências para avaliar a afirmação."
            );
        }

        List<Evidence> supporting = new ArrayList<Evidence>();
        List<Evidence> contradicting = new ArrayList<Evidence>();
        List<Evidence> other = new ArrayList<Evidence>();

        int processed = 0;

        for (Evidence evidence : evidenceList) {
            if (evidence == null) {
                continue;
            }

            if (processed >= MAX_EVIDENCE_ITEMS) {
                break;
            }

            processed++;

            if (evidence.getStatement().isEmpty()) {
                other.add(evidence);
                continue;
            }

            if (evidence.getType() == EvidenceType.INFERENCE
                    || evidence.getType() == EvidenceType.UNKNOWN
                    || evidence.getType() == EvidenceType.USER_PROVIDED) {
                other.add(evidence);
                continue;
            }

            if (evidence.supportsClaim()) {
                supporting.add(evidence);
            } else {
                contradicting.add(evidence);
            }
        }

        Verdict verdict;
        double confidence;
        String explanation;

        if (!supporting.isEmpty() && !contradicting.isEmpty()) {
            verdict = Verdict.MIXED;
            confidence = 0.45;
            explanation =
                    "Existem evidências classificadas em direções opostas. "
                    + "É necessária uma análise adicional.";
        } else if (!supporting.isEmpty()) {
            verdict = Verdict.SUPPORTED;
            confidence = calculateConfidence(supporting);
            explanation =
                    "As evidências fornecidas foram classificadas como "
                    + "favoráveis à afirmação. A fonte e o contexto ainda "
                    + "precisam ser considerados.";
        } else if (!contradicting.isEmpty()) {
            verdict = Verdict.CONTRADICTED;
            confidence = calculateConfidence(contradicting);
            explanation =
                    "As evidências fornecidas foram classificadas como "
                    + "contrárias à afirmação. Confira a fonte e o contexto.";
        } else {
            verdict = Verdict.INSUFFICIENT_EVIDENCE;
            confidence = 0.0;
            explanation =
                    "As evidências disponíveis não permitem concluir "
                    + "se a afirmação está correta.";
        }

        return new Assessment(
                verdict,
                safeClaim,
                confidence,
                supporting,
                contradicting,
                other,
                explanation
        );
    }

    /**
     * Compara duas afirmações textualmente.
     *
     * Esta função identifica igualdade textual normalizada,
     * não determina se as afirmações são factualmente opostas.
     */
    public boolean areTextuallyEquivalent(
            String first,
            String second
    ) {
        if (first == null || second == null) {
            return false;
        }

        return normalizeForComparison(first).equals(
                normalizeForComparison(second)
        );
    }

    public boolean hasConflictingEvidence(Assessment assessment) {
        return assessment != null && assessment.hasConflict();
    }

    private double calculateConfidence(List<Evidence> evidence) {
        if (evidence.isEmpty()) {
            return 0.0;
        }

        double totalWeight = 0.0;

        for (Evidence item : evidence) {
            totalWeight += weightFor(item.getType());
        }

        /*
         * Este valor é apenas um indicador heurístico.
         * Não representa uma probabilidade real de verdade.
         */
        double score = totalWeight / (totalWeight + 2.0);

        return Math.min(0.75, score);
    }

    private double weightFor(EvidenceType type) {
        if (type == null) {
            return 0.0;
        }

        switch (type) {
            case TRUSTED_SOURCE:
                return 2.0;

            case TOOL_RESULT:
                return 1.5;

            case LOCAL_KNOWLEDGE:
                return 1.0;

            case USER_PROVIDED:
            case INFERENCE:
            case UNKNOWN:
            default:
                return 0.0;
        }
    }

    private Assessment invalidAssessment(
            String claim,
            String explanation
    ) {
        return new Assessment(
                Verdict.INVALID_INPUT,
                claim,
                0.0,
                Collections.<Evidence>emptyList(),
                Collections.<Evidence>emptyList(),
                Collections.<Evidence>emptyList(),
                explanation
        );
    }

    private static String normalizeForComparison(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private static String safeText(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        String result = value.trim();

        if (result.length() > maxLength) {
            result = result.substring(0, maxLength);
        }

        return result;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static List<Evidence> immutableCopy(
            List<Evidence> source
    ) {
        return Collections.unmodifiableList(
                new ArrayList<Evidence>(source)
        );
    }
}
