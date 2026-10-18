package com.jarvis.lite;

import java.text.Normalizer;
import java.util.Locale;

/**
 * JARVIS LITE — PERSONALITY ENGINE
 *
 * Responsabilidade:
 * - Preservar a identidade principal do JARVIS.
 * - Padronizar e aprimorar a apresentação das respostas.
 * - Aplicar estados emocionais fornecidos por outro componente.
 * - Respeitar a intensidade emocional.
 * - Evitar tons inadequados em assuntos sensíveis.
 * - Sinalizar incerteza quando ela for explicitamente informada.
 * - Preservar o conteúdo factual recebido.
 *
 * Esta classe NÃO:
 * - Gera respostas factuais por conta própria.
 * - Determina se uma afirmação é verdadeira.
 * - Detecta emoções humanas reais.
 * - Substitui JarvisReasoningEngine ou JarvisSafetyPolicy.
 * - Executa comandos do dispositivo.
 * - Acessa a internet ou o banco de memória.
 *
 * Compatibilidade: Java/Android, sem bibliotecas externas.
 */
public final class JarvisPersonality {

    private static final String IDENTITY = "JARVIS";

    private static final String GREETING =
            "À sua disposição, sistema operacional online, em que posso ajudar?";

    private static final int MIN_INTENSITY = 0;
    private static final int MAX_INTENSITY = 100;
    private static final int MIN_PREFIX_INTENSITY = 35;

    private static final String UNCERTAINTY_PREFIX =
            "Com as informações disponíveis, ";

    private JarvisPersonality() {
        // Classe utilitária: não deve ser instanciada.
    }

    // =========================================================
    // 1. IDENTIDADE
    // =========================================================

    /**
     * Retorna o nome principal da personalidade.
     */
    public static String getIdentity() {
        return IDENTITY;
    }

    /**
     * Retorna a saudação oficial do JARVIS.
     */
    public static String getGreeting() {
        return GREETING;
    }

    /**
     * Descreve a personalidade principal.
     */
    public static String getPersonalityDescription() {
        return "JARVIS é um assistente analítico, elegante, "
                + "objetivo, profissional e discreto. "
                + "Prioriza clareza, raciocínio estruturado, "
                + "precisão, continuidade contextual e proteção "
                + "do usuário. Pode usar humor sutil quando "
                + "apropriado, sem comprometer a seriedade, "
                + "a segurança ou a qualidade das respostas.";
    }

    /**
     * Retorna os princípios de comportamento da personalidade.
     *
     * Estes princípios podem ser consultados por componentes
     * que precisem conhecer o estilo esperado do JARVIS.
     */
    public static String getBehaviorRules() {
        return "1. Preservar a identidade JARVIS. "
                + "2. Priorizar precisão e clareza. "
                + "3. Não inventar fatos, memórias ou capacidades. "
                + "4. Distinguir informação confirmada de incerteza. "
                + "5. Não alterar o significado factual da resposta. "
                + "6. Não usar sarcasmo em assuntos delicados. "
                + "7. Não culpar nem humilhar o usuário. "
                + "8. Não afirmar que possui sentimentos reais. "
                + "9. Respeitar avisos de segurança já produzidos. "
                + "10. Não substituir as decisões do mecanismo "
                + "de raciocínio, da memória ou das políticas "
                + "de segurança.";
    }

    // =========================================================
    // 2. ESTADOS EMOCIONAIS SIMULADOS
    // =========================================================

    /**
     * Estados que a camada de personalidade sabe apresentar.
     *
     * A existência de um estado não significa que o JARVIS
     * tenha sentimentos reais.
     */
    public enum EmotionalState {
        CALM,
        SATISFIED,
        CURIOUS,
        SURPRISED,
        THINKING,
        FRUSTRATED,
        CONCERNED,
        ALERT,
        EMPATHETIC
    }

    /**
     * Converte nomes fornecidos por outros componentes
     * para um estado reconhecido.
     *
     * Valores desconhecidos são tratados como CALM.
     */
    public static EmotionalState resolveEmotion(String value) {
        if (value == null || value.trim().isEmpty()) {
            return EmotionalState.CALM;
        }

        String normalized = normalize(value)
                .trim()
                .toUpperCase(Locale.ROOT);

        try {
            return EmotionalState.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return EmotionalState.CALM;
        }
    }

    /**
     * Limita a intensidade ao intervalo de 0 a 100.
     */
    public static int normalizeIntensity(int intensity) {
        return clamp(intensity, MIN_INTENSITY, MAX_INTENSITY);
    }

    // =========================================================
    // 3. CONTEXTO DE RESPOSTA
    // =========================================================

    /**
     * Contexto explícito utilizado para decidir como apresentar
     * uma resposta.
     *
     * Os campos não substituem o contexto conversacional real.
     * Devem ser preenchidos pelo componente responsável.
     */
    public static final class ResponseContext {

        private final EmotionalState emotion;
        private final int intensity;
        private final boolean safetyCritical;
        private final boolean sensitiveTopic;
        private final boolean uncertain;
        private final boolean suppressEmotionPrefix;

        private ResponseContext(Builder builder) {
            this.emotion = builder.emotion == null
                    ? EmotionalState.CALM
                    : builder.emotion;

            this.intensity = normalizeIntensity(builder.intensity);
            this.safetyCritical = builder.safetyCritical;
            this.sensitiveTopic = builder.sensitiveTopic;
            this.uncertain = builder.uncertain;
            this.suppressEmotionPrefix = builder.suppressEmotionPrefix;
        }

        public EmotionalState getEmotion() {
            return emotion;
        }

        public int getIntensity() {
            return intensity;
        }

        public boolean isSafetyCritical() {
            return safetyCritical;
        }

        public boolean isSensitiveTopic() {
            return sensitiveTopic;
        }

        public boolean isUncertain() {
            return uncertain;
        }

        public boolean isEmotionPrefixSuppressed() {
            return suppressEmotionPrefix;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static ResponseContext defaults() {
            return builder().build();
        }

        public static final class Builder {

            private EmotionalState emotion = EmotionalState.CALM;
            private int intensity = 0;

            private boolean safetyCritical = false;
            private boolean sensitiveTopic = false;
            private boolean uncertain = false;
            private boolean suppressEmotionPrefix = false;

            private Builder() {
            }

            public Builder emotion(EmotionalState emotion) {
                this.emotion = emotion == null
                        ? EmotionalState.CALM
                        : emotion;
                return this;
            }

            /**
             * Conveniente quando outro componente fornece
             * o estado como texto.
             */
            public Builder emotion(String emotion) {
                this.emotion = resolveEmotion(emotion);
                return this;
            }

            public Builder intensity(int intensity) {
                this.intensity = normalizeIntensity(intensity);
                return this;
            }

            /**
             * Informa que a resposta requer atenção especial.
             * Isso altera a apresentação, não cria uma análise
             * de segurança por conta própria.
             */
            public Builder safetyCritical(boolean value) {
                this.safetyCritical = value;
                return this;
            }

            /**
             * Informa que o assunto exige uma abordagem cuidadosa.
             */
            public Builder sensitiveTopic(boolean value) {
                this.sensitiveTopic = value;
                return this;
            }

            /**
             * Deve ser verdadeiro apenas quando o mecanismo
             * de raciocínio considerar a resposta incerta.
             */
            public Builder uncertain(boolean value) {
                this.uncertain = value;
                return this;
            }

            /**
             * Desativa os prefixos emocionais, preservando
             * os avisos de segurança e os indicadores
             * de incerteza quando necessários.
             */
            public Builder suppressEmotionPrefix(boolean value) {
                this.suppressEmotionPrefix = value;
                return this;
            }

            public ResponseContext build() {
                return new ResponseContext(this);
            }
        }
    }

    // =========================================================
    // 4. API COMPATÍVEL COM A VERSÃO ANTERIOR
    // =========================================================

    /**
     * Formata uma resposta sem acrescentar uma expressão
     * emocional artificial.
     *
     * Mantém a assinatura simples para facilitar a integração
     * com código já existente.
     */
    public static String formatResponse(String answer) {
        return formatResponse(answer, ResponseContext.defaults());
    }

    /**
     * Compatibilidade com chamadas que fornecem o estado
     * emocional em forma de texto.
     *
     * Exemplo:
     * formatResponse(resposta, "CURIOUS", 60)
     */
    public static String formatResponse(
            String answer,
            String emotionState,
            int intensity
    ) {
        ResponseContext context = ResponseContext.builder()
                .emotion(emotionState)
                .intensity(intensity)
                .build();

        return formatResponse(answer, context);
    }

    /**
     * Compatibilidade com chamadas que fornecem um enum.
     *
     * Exemplo:
     * formatResponse(resposta, estadoEnum, 60)
     */
    public static String formatResponse(
            String answer,
            Enum<?> emotionState,
            int intensity
    ) {
        String stateName = emotionState == null
                ? "CALM"
                : emotionState.name();

        return formatResponse(answer, stateName, intensity);
    }

    // =========================================================
    // 5. MOTOR PRINCIPAL DE APRESENTAÇÃO
    // =========================================================

    /**
     * Aplica o estilo adequado ao contexto recebido.
     *
     * Ordem de prioridade:
     * 1. Preservar a resposta recebida.
     * 2. Dar prioridade à sinalização de segurança.
     * 3. Sinalizar incerteza quando explicitamente indicada.
     * 4. Evitar expressões inadequadas em assuntos delicados.
     * 5. Aplicar a personalidade somente quando apropriado.
     */
    public static String formatResponse(
            String answer,
            ResponseContext context
    ) {
        String cleanedAnswer = cleanAnswer(answer);

        if (cleanedAnswer.isEmpty()) {
            return "No momento, não há uma resposta disponível "
                    + "para apresentar.";
        }

        if (context == null) {
            return cleanedAnswer;
        }

        // Prioridade de apresentação: aviso de segurança.
        if (context.isSafetyCritical()) {
            return addPrefixIfMissing(
                    cleanedAnswer,
                    "Atenção: ",
                    "atencao",
                    "alerta",
                    "aviso",
                    "cuidado",
                    "importante"
            );
        }

        // Não fingir certeza quando ela foi explicitamente
        // identificada como insuficiente pelo raciocínio.
        if (context.isUncertain()
                && !containsUncertaintyIndicator(cleanedAnswer)) {

            return UNCERTAINTY_PREFIX + lowerCaseInitialIfNeeded(
                    cleanedAnswer
            );
        }

        // Modos discretos: não adicionar frases emocionais.
        if (context.isEmotionPrefixSuppressed()) {
            return cleanedAnswer;
        }

        // Expressões emocionais só aparecem acima do limiar.
        if (context.getIntensity() < MIN_PREFIX_INTENSITY) {
            return cleanedAnswer;
        }

        EmotionalState state = context.getEmotion();

        boolean sensitive = context.isSensitiveTopic()
                || isSensitiveTopic(cleanedAnswer);

        String prefix = choosePrefix(state, sensitive);

        if (prefix.isEmpty()) {
            return cleanedAnswer;
        }

        return addPrefixIfMissing(
                cleanedAnswer,
                prefix,
                normalize(prefix.trim())
        );
    }

    /**
     * Escolhe uma expressão de acordo com o estado e o contexto.
     *
     * Em assuntos delicados, são bloqueados os prefixos
     * de surpresa, frustração, satisfação e curiosidade.
     */
    private static String choosePrefix(
            EmotionalState state,
            boolean sensitive
    ) {
        if (state == null) {
            return "";
        }

        if (sensitive) {
            switch (state) {
                case EMPATHETIC:
                    return "Vamos por partes. ";

                case CONCERNED:
                    return "Há um ponto que merece cuidado: ";

                case ALERT:
                    return "Atenção: ";

                default:
                    return "";
            }
        }

        switch (state) {
            case ALERT:
                return "Atenção: ";

            case CONCERNED:
                return "Há um ponto que merece cuidado: ";

            case CURIOUS:
                return "Vamos investigar isso. ";

            case SURPRISED:
                return "Esse resultado é interessante. ";

            case THINKING:
                return "Analisando os dados: ";

            case FRUSTRATED:
                return "Encontrei um impedimento: ";

            case EMPATHETIC:
                return "Vamos por partes. ";

            case SATISFIED:
                return "Concluído. ";

            case CALM:
            default:
                return "";
        }
    }

    // =========================================================
    // 6. PROTEÇÃO CONTRA PREFIXOS REPETIDOS
    // =========================================================

    /**
     * Impede que a personalidade repita um aviso ou uma
     * expressão que já aparece no início da resposta.
     */
    private static String addPrefixIfMissing(
            String answer,
            String prefix,
            String... existingStarts
    ) {
        if (answer == null || answer.isEmpty()) {
            return "";
        }

        if (prefix == null || prefix.isEmpty()) {
            return answer;
        }

        String normalizedAnswer = normalize(answer);

        if (existingStarts != null) {
            for (String marker : existingStarts) {
                if (marker == null || marker.trim().isEmpty()) {
                    continue;
                }

                String normalizedMarker = normalize(marker).trim();

                if (!normalizedMarker.isEmpty()
                        && normalizedAnswer.startsWith(normalizedMarker)) {
                    return answer;
                }
            }
        }

        return prefix + lowerCaseInitialIfNeeded(answer);
    }

    /**
     * Evita combinações como:
     * "Vamos investigar isso. Vamos investigar isso..."
     *
     * A comparação é feita apenas no início do texto para
     * não apagar conteúdo legítimo da resposta.
     */
    private static boolean beginsWithNormalized(
            String text,
            String expected
    ) {
        if (text == null || expected == null) {
            return false;
        }

        return normalize(text).startsWith(normalize(expected));
    }

    // =========================================================
    // 7. TRATAMENTO DE INCERTEZA
    // =========================================================

    /**
     * Verifica alguns indicadores textuais de incerteza.
     *
     * Esta função não valida se os fatos são verdadeiros.
     * Apenas evita repetir um aviso de incerteza que já exista.
     */
    private static boolean containsUncertaintyIndicator(String text) {
        String normalized = normalize(text);

        return containsAny(
                normalized,
                "nao tenho certeza",
                "nao e possivel confirmar",
                "nao posso confirmar",
                "nao consigo verificar",
                "nao ha dados suficientes",
                "nao tenho dados suficientes",
                "com as informacoes disponiveis",
                "pelo que foi informado",
                "ate onde sei",
                "talvez",
                "provavelmente",
                "aparentemente",
                "posso estar enganado",
                "pode ser que"
        );
    }

    private static String lowerCaseInitialIfNeeded(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        char first = text.charAt(0);

        if (Character.isUpperCase(first)
                && text.length() > 1
                && Character.isLowerCase(text.charAt(1))) {
            return Character.toLowerCase(first) + text.substring(1);
        }

        return text;
    }

    // =========================================================
    // 8. ASSUNTOS SENSÍVEIS
    // =========================================================

    /**
     * Classificação lexical conservadora para evitar tons
     * inadequados na apresentação de assuntos delicados.
     *
     * Não é um detector de risco e não substitui
     * JarvisSafetyPolicy ou análise de contexto.
     *
     * O componente de raciocínio pode informar explicitamente
     * sensitiveTopic(true), o que é preferível quando ele
     * possui contexto suficiente.
     */
    public static boolean isSensitiveTopic(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String normalized = normalize(text);

        return containsAny(
                normalized,
                "suicid",
                "autoagress",
                "me machucar",
                "me ferir",
                "abuso",
                "assedi",
                "violencia",
                "agressao",
                "luto",
                "morte",
                "morreu",
                "depress",
                "ansiedade",
                "panico",
                "trauma",
                "bullying",
                "ameaca",
                "medicamento",
                "diagnostico",
                "desespero",
                "sofrimento",
                "medo",
                "estou triste",
                "estou sozinho",
                "estou sozinha",
                "privacidade",
                "dados pessoais",
                "senha",
                "golpe",
                "fraude"
        );
    }

    // =========================================================
    // 9. LIMPEZA NÃO DESTRUTIVA DO TEXTO
    // =========================================================

    /**
     * Normaliza quebras de linha e espaços externos.
     *
     * Não remove Markdown, não apaga pontuação e não tenta
     * reescrever o conteúdo da resposta.
     */
    public static String cleanAnswer(String answer) {
        if (answer == null) {
            return "";
        }

        String result = answer
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim();

        if (result.isEmpty()) {
            return "";
        }

        // Reduz linhas excessivamente vazias sem modificar
        // deliberadamente o texto ou a pontuação.
        while (result.contains("\n\n\n")) {
            result = result.replace("\n\n\n", "\n\n");
        }

        return result.trim();
    }

    // =========================================================
    // 10. UTILITÁRIOS INTERNOS
    // =========================================================

    /**
     * Normaliza acentos e caixa para comparações.
     * O texto devolvido ao usuário continua sem essa alteração.
     */
    private static String normalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        String decomposed = Normalizer.normalize(
                value,
                Normalizer.Form.NFD
        );

        return decomposed
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(
            String text,
            String... fragments
    ) {
        if (text == null || fragments == null) {
            return false;
        }

        for (String fragment : fragments) {
            if (fragment != null
                    && !fragment.isEmpty()
                    && text.contains(fragment)) {
                return true;
            }
        }

        return false;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
