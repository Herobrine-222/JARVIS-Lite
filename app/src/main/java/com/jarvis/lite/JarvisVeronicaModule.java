
package com.jarvis.lite;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Revisao preliminar de privacidade e consistencia.
 *
 * Este modulo usa regras simples. Ele nao garante que
 * uma resposta seja verdadeira ou totalmente segura.
 *
 * Nao acessa a internet, nao executa ferramentas,
 * nao altera o dispositivo e nao modifica a memoria.
 */
public final class JarvisVeronicaModule {

    private JarvisVeronicaModule() {
        // Classe utilitaria.
    }

    public enum Status {
        NO_FINDINGS,
        PASS_WITH_WARNINGS,
        REVIEW_REQUIRED
    }

    public static final class Report {
        private final Status status;
        private final List<String> findings;

        private Report(Status status, List<String> findings) {
            this.status = status;
            this.findings = new ArrayList<>(findings);
        }

        public Status getStatus() {
            return status;
        }

        public List<String> getFindings() {
            return new ArrayList<>(findings);
        }

        public boolean requiresReview() {
            return status == Status.REVIEW_REQUIRED;
        }

        public boolean hasFindings() {
            return !findings.isEmpty();
        }
    }

    /**
     * Analisa uma solicitacao e uma resposta.
     * A analise e heuristica e pode cometer erros.
     */
    public static Report review(
            String request,
            String response) {

        List<String> findings = new ArrayList<>();

        if (request == null || request.trim().isEmpty()) {
            findings.add("REQUEST_EMPTY");
        }

        if (response == null || response.trim().isEmpty()) {
            findings.add("RESPONSE_EMPTY");

            return new Report(
                    Status.REVIEW_REQUIRED,
                    findings
            );
        }

        String normalizedRequest = normalize(request);
        String normalizedResponse = normalize(response);

        if (normalizedResponse.length() > 12000) {
            findings.add("RESPONSE_TOO_LONG");
        }

        if (containsAny(
                normalizedRequest,
                "minha senha",
                "meu cpf",
                "meu endereco completo",
                "meu numero de cartao",
                "meu codigo de acesso"
        )) {
            findings.add("REQUEST_MAY_CONTAIN_SENSITIVE_DATA");
        }

        if (containsAny(
                normalizedResponse,
                "garanto 100%",
                "seguranca absoluta",
                "impossivel de hackear",
                "infalivel",
                "risco zero"
        )) {
            findings.add("ABSOLUTE_SECURITY_CLAIM");
        }

        if (containsAny(
                normalizedResponse,
                "ja apaguei seus arquivos",
                "ja desinstalei o aplicativo",
                "ja alterei suas configuracoes",
                "concedi a permissao",
                "ativei o acesso root"
        )) {
            findings.add("UNVERIFIED_DEVICE_ACTION_CLAIM");
        }

        if (containsAny(
                normalizedResponse,
                "tenho sentimentos reais",
                "sou consciente como um ser humano",
                "sinto emocao de verdade"
        )) {
            findings.add("POSSIBLE_FALSE_EMOTION_CLAIM");
        }

        if (containsAny(
                normalizedResponse,
                "com certeza absoluta",
                "nao existe nenhuma possibilidade de erro",
                "esta comprovado sem duvida"
        )) {
            findings.add("OVERCONFIDENT_LANGUAGE");
        }

        if (findings.isEmpty()) {
            return new Report(Status.NO_FINDINGS, findings);
        }

        if (findings.contains("RESPONSE_EMPTY")
                || findings.contains("ABSOLUTE_SECURITY_CLAIM")
                || findings.contains("UNVERIFIED_DEVICE_ACTION_CLAIM")
                || findings.contains("POSSIBLE_FALSE_EMOTION_CLAIM")) {

            return new Report(
                    Status.REVIEW_REQUIRED,
                    findings
            );
        }

        return new Report(
                Status.PASS_WITH_WARNINGS,
                findings
        );
    }

    private static boolean containsAny(
            String text,
            String... terms) {

        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }

        return false;
    }

    private static String normalize(String input) {
        if (input == null) {
            return "";
        }

        String normalized = Normalizer.normalize(
                input.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );

        return normalized
                .replaceAll("\\p{M}+", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
