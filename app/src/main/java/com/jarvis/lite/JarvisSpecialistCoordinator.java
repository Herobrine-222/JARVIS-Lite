
package com.jarvis.lite;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Recomenda um especialista para uma solicitacao.
 *
 * Este coordenador nao executa ferramentas, nao altera
 * o dispositivo, nao acessa a rede e nao gera respostas.
 *
 * A escolha e heuristica e deve ser validada pelo fluxo
 * principal antes de encaminhar uma solicitacao.
 */
public final class JarvisSpecialistCoordinator {

    private JarvisSpecialistCoordinator() {
        // Classe utilitaria.
    }

    public enum Specialist {
        CORE,
        FRIDAY,
        EDITH,
        DUM_E,
        U,
        KAREN,
        MEMORY,
        UNKNOWN
    }

    public enum Confidence {
        LOW,
        MEDIUM,
        HIGH
    }

    public static final class Recommendation {
        private final Specialist specialist;
        private final Confidence confidence;
        private final String reasonCode;
        private final boolean needsClarification;

        private Recommendation(
                Specialist specialist,
                Confidence confidence,
                String reasonCode,
                boolean needsClarification) {
            this.specialist = specialist;
            this.confidence = confidence;
            this.reasonCode = reasonCode;
            this.needsClarification = needsClarification;
        }

        public Specialist getSpecialist() {
            return specialist;
        }

        public Confidence getConfidence() {
            return confidence;
        }

        public String getReasonCode() {
            return reasonCode;
        }

        public boolean needsClarification() {
            return needsClarification;
        }
    }

    /**
     * Analisa palavras-chave e sugere um destino.
     * Nao comprova a intencao real do usuario.
     */
    public static Recommendation recommend(String input) {
        if (input == null || input.trim().isEmpty()) {
            return recommendation(
                    Specialist.UNKNOWN,
                    Confidence.LOW,
                    "EMPTY_INPUT",
                    true
            );
        }

        if (input.length() > 6000) {
            return recommendation(
                    Specialist.UNKNOWN,
                    Confidence.LOW,
                    "INPUT_TOO_LONG",
                    true
            );
        }

        String text = normalize(input);

        if (containsAny(text,
                "minha memoria", "lembra", "lembrar",
                "esqueci", "o que eu disse", "historico")) {
            return recommendation(
                    Specialist.MEMORY,
                    Confidence.HIGH,
                    "MEMORY_KEYWORDS",
                    false
            );
        }

        if (containsAny(text,
                "bateria", "armazenamento", "espaco livre",
                "memoria ram", "temperatura do aparelho",
                "informacoes do dispositivo")) {
            return recommendation(
                    Specialist.EDITH,
                    Confidence.HIGH,
                    "DEVICE_INFO_KEYWORDS",
                    false
            );
        }

        if (containsAny(text,
                "erro no codigo", "depurar", "debug",
                "compilar", "build", "java", "android",
                "github", "programacao")) {
            return recommendation(
                    Specialist.KAREN,
                    Confidence.MEDIUM,
                    "DEVELOPMENT_KEYWORDS",
                    false
            );
        }

        if (containsAny(text,
                "calcule", "matematica", "equacao",
                "quanto e", "resolver a conta", "raiz quadrada")) {
            return recommendation(
                    Specialist.U,
                    Confidence.MEDIUM,
                    "MATH_KEYWORDS",
                    false
            );
        }

        if (containsAny(text,
                "imagem", "foto", "camera",
                "descreva o que aparece", "visao")) {
            return recommendation(
                    Specialist.FRIDAY,
                    Confidence.MEDIUM,
                    "VISION_KEYWORDS",
                    false
            );
        }

        if (containsAny(text,
                "organize", "planeje", "passo a passo",
                "lista de tarefas", "prioridades")) {
            return recommendation(
                    Specialist.DUM_E,
                    Confidence.MEDIUM,
                    "PLANNING_KEYWORDS",
                    false
            );
        }

        if (containsAny(text,
                "seguranca", "permissao", "risco",
                "suspeito", "verificacao de seguranca")) {
            return recommendation(
                    Specialist.EDITH,
                    Confidence.MEDIUM,
                    "SAFETY_REVIEW_KEYWORDS",
                    false
            );
        }

        if (containsAny(text,
                "explique", "pesquise", "compare",
                "resuma", "o que e", "como funciona")) {
            return recommendation(
                    Specialist.CORE,
                    Confidence.MEDIUM,
                    "GENERAL_KNOWLEDGE_KEYWORDS",
                    false
            );
        }

        return recommendation(
                Specialist.CORE,
                Confidence.LOW,
                "DEFAULT_CORE_ROUTE",
                true
        );
    }

    private static Recommendation recommendation(
            Specialist specialist,
            Confidence confidence,
            String reasonCode,
            boolean needsClarification) {
        return new Recommendation(
                specialist,
                confidence,
                reasonCode,
                needsClarification
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
