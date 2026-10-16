
package com.jarvis.lite;

import java.util.Locale;

/**
 * Motor local de raciocínio do JARVIS Lite.
 *
 * Responsabilidades:
 * - identificar a intenção provável de uma mensagem;
 * - indicar o especialista mais adequado;
 * - estimar a confiança da classificação;
 * - manter o processamento leve e offline;
 * - não executar comandos nem alterar o aparelho.
 *
 * Esta classe classifica mensagens. Ela não é, sozinha,
 * um modelo de linguagem generativo.
 */
public final class JarvisReasoningEngine {

    public enum Intent {
        GREETING,
        MATHEMATICS,
        BATTERY,
        STORAGE,
        MEMORY,
        PROJECT,
        SCHOOL_HELP,
        DEVICE_CHECK,
        END_SESSION,
        GENERAL_CONVERSATION,
        UNKNOWN
    }

    public enum Specialist {
        CORE,
        FRIDAY,
        EDITH,
        DUM_E,
        U,
        MEMORY
    }

    public static final class Analysis {

        private final Intent intent;
        private final Specialist specialist;
        private final float confidence;
        private final String normalizedText;
        private final String reason;

        private Analysis(
                Intent intent,
                Specialist specialist,
                float confidence,
                String normalizedText,
                String reason
        ) {
            this.intent = intent;
            this.specialist = specialist;
            this.confidence = confidence;
            this.normalizedText = normalizedText;
            this.reason = reason;
        }

        public Intent getIntent() {
            return intent;
        }

        public Specialist getSpecialist() {
            return specialist;
        }

        public float getConfidence() {
            return confidence;
        }

        public String getNormalizedText() {
            return normalizedText;
        }

        public String getReason() {
            return reason;
        }

        public boolean isConfident(float minimum) {
            return confidence >= clamp(minimum, 0.0f, 1.0f);
        }
    }

    /**
     * Analisa uma mensagem sem internet e sem executar ações.
     */
    public Analysis analyze(String message) {

        String text = normalize(message);

        if (text.isEmpty()) {
            return result(
                    Intent.UNKNOWN,
                    Specialist.CORE,
                    0.0f,
                    text,
                    "Mensagem vazia."
            );
        }

        if (containsAny(text,
                "encerrar sessao",
                "encerrar a sessao",
                "jarvis dormir",
                "jarvis pode dormir",
                "voltar ao modo de espera")) {
            return result(
                    Intent.END_SESSION,
                    Specialist.CORE,
                    0.98f,
                    text,
                    "Pedido explícito para encerrar a sessão."
            );
        }

        if (isGreeting(text)) {
            return result(
                    Intent.GREETING,
                    Specialist.FRIDAY,
                    0.96f,
                    text,
                    "Saudação reconhecida."
            );
        }

        if (containsAny(text,
                "bateria",
                "por cento de bateria",
                "temperatura da bateria",
                "temperatura do celular")) {
            return result(
                    Intent.BATTERY,
                    Specialist.DUM_E,
                    0.94f,
                    text,
                    "Possível consulta local sobre bateria."
            );
        }

        if (containsAny(text,
                "armazenamento",
                "espaco livre",
                "espaço livre",
                "memoria interna",
                "memória interna",
                "quanto de espaco",
                "quanto de espaço")) {
            return result(
                    Intent.STORAGE,
                    Specialist.DUM_E,
                    0.92f,
                    text,
                    "Possível consulta local sobre armazenamento."
            );
        }

        if (containsAny(text,
                "verificacao do aparelho",
                "verificação do aparelho",
                "verificar o aparelho",
                "verificar seguranca",
                "verificar segurança",
                "verificacao basica",
                "verificação básica")) {
            return result(
                    Intent.DEVICE_CHECK,
                    Specialist.DUM_E,
                    0.90f,
                    text,
                    "Pedido de verificação informativa do aparelho."
            );
        }

        if (containsAny(text,
                "lembra",
                "voce lembra",
                "você lembra",
                "minha memoria",
                "minha memória",
                "o que voce sabe sobre mim",
                "o que você sabe sobre mim",
                "lembranca",
                "lembrança",
                "historico da conversa",
                "histórico da conversa")) {
            return result(
                    Intent.MEMORY,
                    Specialist.MEMORY,
                    0.88f,
                    text,
                    "Pedido relacionado a lembranças ou histórico."
            );
        }

        if (containsAny(text,
                "jarvis lite",
                "nosso projeto",
                "nosso aplicativo",
                "nosso app",
                "codigo do jarvis",
                "código do jarvis",
                "github",
                "programacao do jarvis",
                "programação do jarvis")) {
            return result(
                    Intent.PROJECT,
                    Specialist.EDITH,
                    0.88f,
                    text,
                    "Mensagem relacionada ao projeto JARVIS Lite."
            );
        }

        if (containsAny(text,
                "quanto e",
                "quanto é",
                "calcule",
                "calcular",
                "raiz quadrada",
                "porcentagem de",
                "resultado de")) {
            return result(
                    Intent.MATHEMATICS,
                    Specialist.CORE,
                    0.82f,
                    text,
                    "Possível pedido de cálculo."
            );
        }

        if (containsAny(text,
                "atividade da escola",
                "dever de casa",
                "me ajuda na escola",
                "me ajude na escola",
                "questao da escola",
                "questão da escola",
                "explica essa materia",
                "explica essa matéria",
                "me ensina")) {
            return result(
                    Intent.SCHOOL_HELP,
                    Specialist.FRIDAY,
                    0.82f,
                    text,
                    "Pedido de explicação ou ajuda escolar."
            );
        }

        return result(
                Intent.GENERAL_CONVERSATION,
                Specialist.FRIDAY,
                0.55f,
                text,
                "Nenhuma intenção específica foi identificada."
        );
    }

    /**
     * Alias para facilitar a integração com outros componentes.
     */
    public Analysis analisar(String mensagem) {
        return analyze(mensagem);
    }

    /**
     * Retorna o nome do especialista recomendado.
     * A recomendação não concede permissões ao módulo.
     */
    public Specialist recommendSpecialist(String message) {
        return analyze(message).getSpecialist();
    }

    public String describeIntent(String message) {
        Analysis analysis = analyze(message);

        return "Intenção: " + analysis.getIntent().name()
                + "; especialista: "
                + analysis.getSpecialist().name()
                + "; confiança: "
                + analysis.getConfidence()
                + "; motivo: "
                + analysis.getReason();
    }

    private static Analysis result(
            Intent intent,
            Specialist specialist,
            float confidence,
            String normalizedText,
            String reason
    ) {
        return new Analysis(
                intent,
                specialist,
                clamp(confidence, 0.0f, 1.0f),
                normalizedText,
                reason
        );
    }

    private static boolean isGreeting(String text) {
        return text.equals("oi")
                || text.equals("ola")
                || text.equals("olá")
                || text.equals("e ai")
                || text.equals("e aí")
                || text.equals("bom dia")
                || text.equals("boa tarde")
                || text.equals("boa noite")
                || text.equals("opa")
                || text.equals("tudo bem");
    }

    private static boolean containsAny(
            String text,
            String... terms
    ) {
        for (String term : terms) {
            String normalizedTerm = normalize(term);

            if (!normalizedTerm.isEmpty()
                    && text.contains(normalizedTerm)) {
                return true;
            }
        }

        return false;
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }

        String normalized = text
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace('á', 'a')
                .replace('à', 'a')
                .replace('ã', 'a')
                .replace('â', 'a')
                .replace('é', 'e')
                .replace('ê', 'e')
                .replace('í', 'i')
                .replace('ó', 'o')
                .replace('ô', 'o')
                .replace('õ', 'o')
                .replace('ú', 'u')
                .replace('ç', 'c');

        return normalized.replaceAll("\\s+", " ");
    }

    private static float clamp(
            float value,
            float minimum,
            float maximum
    ) {
        return Math.max(
                minimum,
                Math.min(maximum, value)
        );
    }
}
