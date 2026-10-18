package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Avalia sinais textuais de uma resposta sem afirmar que ela é verdadeira.
 * A pontuação é heurística: não representa probabilidade nem prova de verdade.
 */
public final class JarvisResponseEvaluator {

    public static final int MAX_INPUT_LENGTH = 8000;

    public enum Status {
        ACCEPTABLE,
        NEEDS_REVIEW,
        REJECTED,
        INVALID_INPUT
    }

    public enum Issue {
        EMPTY_RESPONSE,
        RESPONSE_TOO_LONG,
        POSSIBLE_UNCERTAINTY,
        POSSIBLE_ERROR_LANGUAGE,
        MISSING_CONTEXT,
        NO_OBVIOUS_ISSUE
    }

    public static final class Evaluation {

        private final Status status;
        private final List<Issue> issues;
        private final int qualityScore;
        private final String summary;

        private Evaluation(
                Status status,
                List<Issue> issues,
                int qualityScore,
                String summary
        ) {
            this.status = status;
            this.issues = Collections.unmodifiableList(
                    new ArrayList<Issue>(issues)
            );
            this.qualityScore = qualityScore;
            this.summary = summary;
        }

        public Status getStatus() {
            return status;
        }

        public List<Issue> getIssues() {
            return issues;
        }

        public int getQualityScore() {
            return qualityScore;
        }

        public String getSummary() {
            return summary;
        }

        public boolean isAcceptable() {
            return status == Status.ACCEPTABLE;
        }

        public boolean needsReview() {
            return status == Status.NEEDS_REVIEW;
        }
    }

    /**
     * Faz uma avaliação textual básica.
     * Não confirma fatos nem substitui uma verificação de evidências.
     */
    public Evaluation evaluate(String prompt, String response) {

        if (prompt == null || prompt.trim().isEmpty()) {
            return result(
                    Status.INVALID_INPUT,
                    Issue.MISSING_CONTEXT,
                    0,
                    "A pergunta ou contexto não foi informado."
            );
        }

        if (response == null || response.trim().isEmpty()) {
            return result(
                    Status.REJECTED,
                    Issue.EMPTY_RESPONSE,
                    0,
                    "A resposta está vazia."
            );
        }

        String p = prompt.trim();
        String r = response.trim();

        if (p.length() > MAX_INPUT_LENGTH
                || r.length() > MAX_INPUT_LENGTH) {
            return result(
                    Status.REJECTED,
                    Issue.RESPONSE_TOO_LONG,
                    0,
                    "A entrada ou a resposta excede o limite permitido."
            );
        }

        List<Issue> issues = new ArrayList<Issue>();
        int score = 100;

        String lower = r.toLowerCase(java.util.Locale.ROOT);

        String[] uncertaintyPhrases = {
                "não tenho certeza",
                "nao tenho certeza",
                "não sei ao certo",
                "nao sei ao certo",
                "posso estar errado",
                "posso estar enganado",
                "não consigo confirmar",
                "nao consigo confirmar"
        };

        if (containsAny(lower, uncertaintyPhrases)) {
            issues.add(Issue.POSSIBLE_UNCERTAINTY);
            score -= 10;
        }

        String[] errorPhrases = {
                "ocorreu um erro",
                "deu erro",
                "não foi possível processar",
                "nao foi possivel processar",
                "exception",
                "stack trace"
        };

        if (containsAny(lower, errorPhrases)) {
            issues.add(Issue.POSSIBLE_ERROR_LANGUAGE);
            score -= 20;
        }

        if (r.length() < 3) {
            issues.add(Issue.MISSING_CONTEXT);
            score -= 25;
        }

        if (issues.isEmpty()) {
            issues.add(Issue.NO_OBVIOUS_ISSUE);
        }

        score = Math.max(0, Math.min(100, score));

        if (score < 50) {
            return new Evaluation(
                    Status.REJECTED,
                    issues,
                    score,
                    "A resposta precisa ser corrigida antes de ser usada."
            );
        }

        if (score < 90) {
            return new Evaluation(
                    Status.NEEDS_REVIEW,
                    issues,
                    score,
                    "A resposta merece uma revisão adicional."
            );
        }

        return new Evaluation(
                Status.ACCEPTABLE,
                issues,
                score,
                "Nenhum problema textual óbvio foi identificado."
        );
    }

    private static boolean containsAny(
            String text,
            String[] phrases
    ) {
        for (String phrase : phrases) {
            if (text.contains(phrase)) {
                return true;
            }
        }

        return false;
    }

    private static Evaluation result(
            Status status,
            Issue issue,
            int score,
            String summary
    ) {
        List<Issue> issues = new ArrayList<Issue>();
        issues.add(issue);

        return new Evaluation(
                status,
                issues,
                score,
                summary
        );
    }
}
