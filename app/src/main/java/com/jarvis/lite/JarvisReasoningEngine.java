package com.jarvis.lite;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Motor local de classificação e roteamento do JARVIS Lite.
 *
 * Responsabilidades:
 * - identificar a intenção provável de uma mensagem;
 * - recomendar o especialista mais adequado;
 * - atribuir uma estimativa simples de confiança;
 * - normalizar texto em português, incluindo letras acentuadas;
 * - funcionar localmente, sem internet;
 * - não executar comandos, acessar sensores ou alterar o aparelho.
 *
 * IMPORTANTE: esta classe classifica mensagens. Ela não é, sozinha,
 * um modelo de linguagem generativo e não produz as respostas finais.
 */
public final class JarvisReasoningEngine {

    /** Tipos de intenção reconhecidos pelo mecanismo. */
    public enum Intent {
        GREETING,
        MATHEMATICS,
        BATTERY,
        STORAGE,
        MEMORY,
        PROJECT,
        SCHOOL_HELP,
        GUIDANCE,
        DEVICE_CHECK,
        END_SESSION,
        GENERAL_CONVERSATION,
        UNKNOWN
    }

    /** Especialistas disponíveis para receber cada tipo de solicitação. */
    public enum Specialist {
        CORE,
        FRIDAY,
        KAREN,
        EDITH,
        DUM_E,
        U,
        MEMORY
    }

    /** Resultado imutável da análise de uma mensagem. */
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
            this.intent = intent == null ? Intent.UNKNOWN : intent;
            this.specialist = specialist == null
                    ? Specialist.CORE
                    : specialist;
            this.confidence = clamp(confidence, 0.0f, 1.0f);
            this.normalizedText = normalizedText == null
                    ? ""
                    : normalizedText;
            this.reason = reason == null
                    ? "Análise concluída."
                    : reason;
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

        /**
         * Texto em minúsculas, sem acentos e com espaços normalizados.
         */
        public String getNormalizedText() {
            return normalizedText;
        }

        public String getReason() {
            return reason;
        }

        /**
         * Informa se a estimativa de confiança alcança o mínimo informado.
         * Valores de mínimo abaixo de 0 ou acima de 1 são limitados
         * ao intervalo permitido.
         */
        public boolean isConfident(float minimum) {
            return confidence >= clamp(minimum, 0.0f, 1.0f);
        }

        @Override
        public String toString() {
            return "Analysis{"
                    + "intent=" + intent
                    + ", specialist=" + specialist
                    + ", confidence=" + confidence
                    + ", normalizedText='" + normalizedText + '\''
                    + ", reason='" + reason + '\''
                    + '}';
        }
    }

    /**
     * Analisa uma mensagem sem internet e sem executar ações.
     *
     * A ordem das regras é intencional: pedidos explícitos de encerramento,
     * consultas de sistema e memória são reconhecidos antes do fallback geral.
     */
    public Analysis analyze(String message) {
        final String text = normalize(message);

        if (text.isEmpty()) {
            return result(
                    Intent.UNKNOWN,
                    Specialist.CORE,
                    0.0f,
                    text,
                    "Mensagem vazia ou sem conteúdo útil."
            );
        }

        // 1. Comando de encerramento.
        if (containsAny(text,
                "encerrar sessao",
                "encerrar a sessao",
                "finalizar sessao",
                "terminar sessao",
                "jarvis dormir",
                "jarvis pode dormir",
                "voltar ao modo de espera",
                "entrar em modo de espera")) {
            return result(
                    Intent.END_SESSION,
                    Specialist.CORE,
                    0.98f,
                    text,
                    "Pedido explícito para encerrar ou suspender a sessão."
            );
        }

        // 2. Saudação exata, evitando classificar mensagens inteiras
        // como saudações quando contêm outras solicitações.
        if (isGreeting(text)) {
            return result(
                    Intent.GREETING,
                    Specialist.FRIDAY,
                    0.96f,
                    text,
                    "Saudação reconhecida."
            );
        }

        // 3. Verificação informativa do aparelho.
        if (containsAny(text,
                "verificacao do aparelho",
                "verificar o aparelho",
                "verificar meu aparelho",
                "verificar seguranca",
                "verificacao de seguranca",
                "verificacao basica",
                "fazer uma verificacao",
                "analisar seguranca do celular",
                "ver se tem virus",
                "verificar se tem virus",
                "checar malware",
                "verificacao de malware")) {
            return result(
                    Intent.DEVICE_CHECK,
                    Specialist.DUM_E,
                    0.90f,
                    text,
                    "Pedido de verificação informativa do aparelho."
            );
        }

        // 4. Dados locais de bateria.
        if (containsAny(text,
                "bateria",
                "por cento de bateria",
                "porcentagem da bateria",
                "nivel da bateria",
                "temperatura da bateria",
                "temperatura do celular",
                "celular esta carregando")) {
            return result(
                    Intent.BATTERY,
                    Specialist.DUM_E,
                    0.94f,
                    text,
                    "Possível consulta local sobre a bateria."
            );
        }

        // 5. Dados locais de armazenamento.
        if (containsAny(text,
                "armazenamento",
                "espaco livre",
                "memoria interna",
                "quanto de espaco",
                "espaco no celular",
                "armazenamento do celular",
                "quanto armazenamento",
                "espaco disponivel")) {
            return result(
                    Intent.STORAGE,
                    Specialist.DUM_E,
                    0.92f,
                    text,
                    "Possível consulta local sobre armazenamento."
            );
        }

        // 6. Memória persistente e histórico.
        // Evitamos usar apenas a palavra "lembra", que pode aparecer
        // incidentalmente em perguntas sem relação com a memória.
        if (containsAny(text,
                "o que voce lembra de mim",
                "o que voce lembra sobre mim",
                "voce lembra de mim",
                "lembra de mim",
                "o que voce sabe sobre mim",
                "o que sabe sobre mim",
                "minhas lembrancas",
                "minha memoria",
                "memoria do jarvis",
                "memoria do sistema",
                "historico da conversa",
                "historico das conversas",
                "nossas conversas anteriores",
                "o que conversamos antes",
                "o que eu te contei",
                "o que eu falei anteriormente",
                "consultar minhas lembrancas")) {
            return result(
                    Intent.MEMORY,
                    Specialist.MEMORY,
                    0.88f,
                    text,
                    "Pedido relacionado à memória ou ao histórico de conversa."
            );
        }

        // 7. Solicitações sobre o código e o projeto vão para EDITH.
        if (containsAny(text,
                "jarvis lite",
                "nosso projeto",
                "nosso aplicativo",
                "nosso app",
                "codigo do jarvis",
                "programacao do jarvis",
                "programacao do aplicativo",
                "repositorio do projeto",
                "branch feature vision core",
                "github",
                "erro de compilacao",
                "erro no codigo",
                "corrigir esse codigo",
                "corrigir o codigo",
                "implementar um modulo",
                "criar um modulo java")) {
            return result(
                    Intent.PROJECT,
                    Specialist.EDITH,
                    0.88f,
                    text,
                    "Mensagem relacionada ao desenvolvimento do JARVIS Lite ou a código."
            );
        }

        // 8. Matemática e cálculos vão para o núcleo de cálculo.
        if (containsAny(text,
                "quanto e",
                "calcule",
                "calcular",
                "faz a conta",
                "faca a conta",
                "resultado de",
                "raiz quadrada",
                "porcentagem de",
                "equacao",
                "resolver a equacao",
                "resolva a equacao",
                "somar",
                "subtrair",
                "multiplicar",
                "dividir",
                "dividido por",
                "vezes",
                "matematica",
                "calculo matematico")) {
            return result(
                    Intent.MATHEMATICS,
                    Specialist.CORE,
                    0.84f,
                    text,
                    "Possível pedido de cálculo ou resolução matemática."
            );
        }

        // 9. Ajuda escolar vai para KAREN.
        if (containsAny(text,
                "atividade da escola",
                "dever de casa",
                "licao de casa",
                "me ajuda na escola",
                "me ajude na escola",
                "questao da escola",
                "questao escolar",
                "explica essa materia",
                "me ensina",
                "trabalho escolar",
                "estudar para a prova",
                "me ajuda a estudar",
                "ajuda com a tarefa",
                "ajuda com o dever",
                "exercicio da escola",
                "exercicio escolar")) {
            return result(
                    Intent.SCHOOL_HELP,
                    Specialist.KAREN,
                    0.86f,
                    text,
                    "Pedido de explicação, estudo ou ajuda escolar encaminhado à KAREN."
            );
        }

        // 10. Orientação prática, explicações graduais e apoio contextual.
        if (containsAny(text,
                "passo a passo",
                "me explica",
                "me explique",
                "explica melhor",
                "explique melhor",
                "como eu faco",
                "como fazer",
                "como resolver",
                "nao entendi",
                "nao consigo entender",
                "estou com dificuldade",
                "tenho dificuldade",
                "me orienta",
                "me orientar",
                "ajuda a entender",
                "me ajuda a entender",
                "pode explicar",
                "pode simplificar",
                "simplifica para mim",
                "explicacao simples",
                "explicacao detalhada",
                "por onde comeco",
                "como eu comeco",
                "qual o primeiro passo",
                "me ajuda a planejar",
                "me ajuda a organizar",
                "quero aprender",
                "me da um exemplo",
                "me de um exemplo",
                "me ajuda com isso",
                "me ajude com isso")) {
            return result(
                    Intent.GUIDANCE,
                    Specialist.KAREN,
                    0.84f,
                    text,
                    "Pedido de orientação contextual ou explicação passo a passo."
            );
        }

        // 11. Fallback conservador.
        if (text.isEmpty()) {
            return result(
                    Intent.UNKNOWN,
                    Specialist.CORE,
                    0.0f,
                    text,
                    "Mensagem sem conteúdo identificável."
            );
        }

        return result(
                Intent.GENERAL_CONVERSATION,
                Specialist.FRIDAY,
                0.55f,
                text,
                "Nenhuma intenção específica foi identificada com confiança suficiente."
        );
    }

    /** Alias em português para componentes que usam o nome analisar(). */
    public Analysis analisar(String mensagem) {
        return analyze(mensagem);
    }

    /**
     * Retorna o especialista recomendado.
     * A recomendação não concede permissões ao módulo.
     */
    public Specialist recommendSpecialist(String message) {
        return analyze(message).getSpecialist();
    }

    /** Produz uma descrição legível da classificação para diagnóstico. */
    public String describeIntent(String message) {
        Analysis analysis = analyze(message);

        return "Intenção: " + analysis.getIntent().name()
                + "; especialista: " + analysis.getSpecialist().name()
                + "; confiança: " + analysis.getConfidence()
                + "; motivo: " + analysis.getReason();
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
                confidence,
                normalizedText,
                reason
        );
    }

    private static boolean isGreeting(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        String candidate = text.replaceAll("[!?.]+$", "").trim();

        return candidate.equals("oi")
                || candidate.equals("ola")
                || candidate.equals("e ai")
                || candidate.equals("bom dia")
                || candidate.equals("boa tarde")
                || candidate.equals("boa noite")
                || candidate.equals("opa")
                || candidate.equals("tudo bem")
                || candidate.equals("oi jarvis")
                || candidate.equals("ola jarvis")
                || candidate.equals("opa jarvis");
    }

    private static boolean containsAny(String text, String... terms) {
        if (text == null || text.isEmpty() || terms == null) {
            return false;
        }

        for (String term : terms) {
            String normalizedTerm = normalize(term);

            if (!normalizedTerm.isEmpty()
                    && text.contains(normalizedTerm)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Normaliza acentos usando a biblioteca padrão do Java e padroniza espaços.
     * Não depende de internet nem de APIs específicas do Android.
     */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }

        String lower = text.trim().toLowerCase(Locale.ROOT);
        String decomposed = Normalizer.normalize(
                lower,
                Normalizer.Form.NFD
        );
        String withoutMarks = decomposed.replaceAll("\\p{M}+", "");

        // Remove caracteres de controle e normaliza espaços.
        return withoutMarks
                .replaceAll("[\\p{Cc}&&[^\\t]]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static float clamp(
            float value,
            float minimum,
            float maximum
    ) {
        if (Float.isNaN(value)) {
            return minimum;
        }

        if (value == Float.POSITIVE_INFINITY) {
            return maximum;
        }

        if (value == Float.NEGATIVE_INFINITY) {
            return minimum;
        }

        return Math.max(
                minimum,
                Math.min(maximum, value)
        );
    }
}
