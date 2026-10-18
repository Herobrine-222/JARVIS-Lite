
package com.jarvis.lite;

/**
 * Avaliacao metacognitiva baseada em regras explicitas.
 *
 * Nao representa consciencia, introspeccao humana
 * ou verificacao automatica da verdade.
 */
public final class JarvisMetacognition {

    public static final int MAX_TEXT_LENGTH = 8000;

    public enum Confidence {
        VERY_LOW,
        LOW,
        MODERATE,
        HIGH
    }

    public enum Recommendation {
        ACCEPT_FOR_REVIEW,
        REQUEST_MORE_INFORMATION,
        RETRY_WITH_DIFFERENT_APPROACH,
        REJECT_EMPTY_RESPONSE,
        MANUAL_REVIEW
    }

    public static final class Evaluation {
        private final Confidence confidence;
        private final Recommendation recommendation;
        private final boolean responsePresent;
        private final boolean uncertaintyDetected;
        private final boolean missingInformationDetected;
        private final int qualityScore;
        private final String explanation;

        private Evaluation(
                Confidence confidence,
                Recommendation recommendation,
                boolean responsePresent,
                boolean uncertaintyDetected,
                boolean missingInformationDetected,
                int qualityScore,
                String explanation) {

            this.confidence = confidence;
            this.recommendation = recommendation;
            this.responsePresent = responsePresent;
            this.uncertaintyDetected = uncertaintyDetected;
            this.missingInformationDetected =
                    missingInformationDetected;
            this.qualityScore = qualityScore;
            this.explanation = explanation;
        }

        public Confidence getConfidence() {
            return confidence;
        }

        public Recommendation getRecommendation() {
            return recommendation;
        }

        public boolean isResponsePresent() {
            return responsePresent;
        }

        public boolean isUncertaintyDetected() {
            return uncertaintyDetected;
        }

        public boolean isMissingInformationDetected() {
            return missingInformationDetected;
        }

        public int getQualityScore() {
            return qualityScore;
        }

        public String getExplanation() {
            return explanation;
        }
    }

    /**
     * Avalia uma resposta por sinais textuais simples.
     *
     * O resultado e heuristico e nao prova que a
     * resposta esteja correta.
     */
    public Evaluation evaluate(
            String question,
            String response) {

        if (question == null
                || question.trim().isEmpty()
                || question.length() > MAX_TEXT_LENGTH) {
            return create(
                    Confidence.VERY_LOW,
                    Recommendation.REQUEST_MORE_INFORMATION,
                    false,
                    false,
                    true,
                    0,
                    "A pergunta esta vazia ou excede o limite."
            );
        }

        if (response == null
                || response.trim().isEmpty()) {
            return create(
                    Confidence.VERY_LOW,
                    Recommendation.REJECT_EMPTY_RESPONSE,
                    false,
                    false,
                    true,
                    0,
                    "Nenhuma resposta foi fornecida."
            );
        }

        if (response.length() > MAX_TEXT_LENGTH) {
            return create(
                    Confidence.LOW,
                    Recommendation.MANUAL_REVIEW,
                    true,
                    false,
                    false,
                    20,
                    "A resposta excede o limite de avaliacao."
            );
        }

        String normalized = response.trim().toLowerCase();

        boolean uncertainty =
                containsAny(
                        normalized,
                        "nao tenho certeza",
                        "não tenho certeza",
                        "nao sei",
                        "não sei",
                        "talvez",
                        "posso estar errado",
                        "posso estar errada",
                        "nao consigo confirmar",
                        "não consigo confirmar"
                );

        boolean missingInformation =
                containsAny(
                        normalized,
                        "preciso de mais informacoes",
                        "preciso de mais informações",
                        "faltam informacoes",
                        "faltam informações",
                        "informe mais detalhes",
                        "pode esclarecer",
                        "qual detalhe",
                        "qual contexto"
                );

        int score = 50;

        if (response.trim().length() >= 20) {
            score += 10;
        }

        if (response.trim().length() >= 80) {
            score += 10;
        }

        if (uncertainty) {
            score -= 5;
        }

        if (missingInformation) {
            score -= 10;
        }

        score = Math.max(0, Math.min(100, score));

        Confidence confidence;

        if (uncertainty || score < 30) {
            confidence = Confidence.LOW;
        } else if (score < 55) {
            confidence = Confidence.MODERATE;
        } else {
            confidence = Confidence.HIGH;
        }

        Recommendation recommendation;

        String explanation;

        if (missingInformation) {
            recommendation =
                    Recommendation.REQUEST_MORE_INFORMATION;
            explanation =
                    "A resposta indica que faltam informacoes.";
        } else if (uncertainty) {
            recommendation =
                    Recommendation.RETRY_WITH_DIFFERENT_APPROACH;
            explanation =
                    "A resposta expressa incerteza; considere verificar.";
        } else {
            recommendation =
                    Recommendation.ACCEPT_FOR_REVIEW;
            explanation =
                    "A resposta passou pelos criterios textuais basicos.";
        }

        return create(
                confidence,
                recommendation,
                true,
                uncertainty,
                missingInformation,
                score,
                explanation
        );
    }

    private boolean containsAny(
            String text,
            String... terms) {

        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }

        return false;
    }

    private Evaluation create(
            Confidence confidence,
            Recommendation recommendation,
            boolean responsePresent,
            boolean uncertaintyDetected,
            boolean missingInformationDetected,
            int score,
            String explanation) {

        return new Evaluation(
                confidence,
                recommendation,
                responsePresent,
                uncertaintyDetected,
                missingInformationDetected,
                score,
                explanation
        );
    }
}
