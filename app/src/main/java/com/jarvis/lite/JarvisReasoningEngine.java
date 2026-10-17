package com.jarvis.lite;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Motor local de classificacao e roteamento do JARVIS Lite.
 *
 * Responsabilidades:
 * - identificar a intencao provavel de uma mensagem;
 * - recomendar o especialista mais adequado;
 * - atribuir uma estimativa simples de confianca;
 * - normalizar texto em portugues, incluindo letras acentuadas;
 * - funcionar localmente, sem internet;
 * - nao executar comandos, acessar sensores ou alterar o aparelho.
 *
 * Esta classe classifica mensagens. Ela nao e, sozinha,
 * um modelo de linguagem generativo e nao produz as respostas finais.
 */
public final class JarvisReasoningEngine {

    /** Tipos de intencao reconhecidos pelo mecanismo. */
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

    /** Especialistas disponiveis para receber solicitacoes. */
    public enum Specialist {
        CORE,
        FRIDAY,
        KAREN,
        EDITH,
        DUM_E,
        U,
        MEMORY
    }

    /** Resultado imutavel da analise de uma mensagem. */
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
            this.intent = intent == null
                    ? Intent.UNKNOWN
                    : intent;

            this.specialist = specialist == null
                    ? Specialist.CORE
                    : specialist;

            this.confidence = clamp(
                    confidence,
                    0.0f,
                    1.0f
            );

            this.normalizedText = normalizedText == null
                    ? ""
                    : normalizedText;

            this.reason = reason == null
                    ? "Analise concluida."
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
         * Texto em minusculas, sem acentos e com espacos normalizados.
         */
        public String getNormalizedText() {
            return normalizedText;
        }

        public String getReason() {
            return reason;
        }

        /**
         * Informa se a estimativa de confianca alcanca
         * o minimo informado.
         */
        public boolean isConfident(float minimum) {
            return confidence >= clamp(
                    minimum,
                    0.0f,
                    1.0f
            );
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
     * Analisa uma mensagem localmente, sem executar acoes.
     *
     * A ordem das regras e intencional:
     * primeiro comandos e consultas especificas;
     * depois conhecimento offline e matematica;
     * por ultimo, perguntas gerais e conversa.
     */
    public Analysis analyze(String message) {

        final String text = normalize(message);

        if (text.isEmpty()) {
            return result(
                    Intent.UNKNOWN,
                    Specialist.CORE,
                    0.0f,
                    text,
                    "Mensagem vazia ou sem conteudo util."
            );
        }

        // 1. Pedido explicito para encerrar a sessao.
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
                    "Pedido explicito para encerrar ou suspender a sessao."
            );
        }

        // 2. Saudacoes curtas.
        if (isGreeting(text)) {
            return result(
                    Intent.GREETING,
                    Specialist.FRIDAY,
                    0.96f,
                    text,
                    "Saudacao reconhecida."
            );
        }

        // 3. Verificacao informativa do aparelho.
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
                    "Pedido de verificacao informativa do aparelho."
            );
        }

        // 4. Consultas sobre bateria, carga e temperatura.
        if (containsAny(text,
                "bateria",
                "por cento de bateria",
                "porcentagem da bateria",
                "percentual da bateria",
                "percentual de bateria",
                "nivel da bateria",
                "nivel de bateria",
                "status da bateria",
                "estado da bateria",
                "temperatura da bateria",
                "temperatura bateria",
                "temperatura do celular",
                "celular esta carregando",
                "esta carregando",
                "esta na tomada",
                "estado de carregamento",
                "carregamento do celular",
                "quanto de bateria",
                "quanto resta de bateria")) {

            return result(
                    Intent.BATTERY,
                    Specialist.DUM_E,
                    0.94f,
                    text,
                    "Possivel consulta local sobre a bateria."
            );
        }

        // 5. Consultas sobre armazenamento.
        if (containsAny(text,
                "armazenamento",
                "espaco livre",
                "espaco disponivel",
                "espaco no celular",
                "espaco no aparelho",
                "quanto de espaco",
                "quanto espaco",
                "quanto armazenamento",
                "memoria interna",
                "armazenamento interno",
                "armazenamento do celular",
                "armazenamento do aparelho",
                "espaco ocupado",
                "espaco utilizado",
                "quanto resta de espaco")) {

            return result(
                    Intent.STORAGE,
                    Specialist.DUM_E,
                    0.92f,
                    text,
                    "Possivel consulta local sobre armazenamento."
            );
        }

        /*
         * 6. Consultas sobre memoria RAM.
         *
         * O DUM-E ja implementa essa consulta por meio de
         * JarvisDeviceTools.resumoRam().
         *
         * Mantemos GENERAL_CONVERSATION como intencao para
         * preservar a compatibilidade com os modulos existentes.
         * O especialista DUM_E indica o destino real da solicitacao.
         */
        if (containsAny(text,
                "memoria ram",
                "ram disponivel",
                "ram utilizada",
                "ram usada",
                "ram total",
                "uso da ram",
                "uso de ram",
                "quanto de ram",
                "ram do celular",
                "memoria de acesso aleatorio")
                || containsWholeWord(text, "ram")) {

            return result(
                    Intent.GENERAL_CONVERSATION,
                    Specialist.DUM_E,
                    0.92f,
                    text,
                    "Consulta sobre memoria RAM encaminhada ao DUM-E."
            );
        }

        /*
         * 7. Informacoes gerais do aparelho.
         *
         * O DUM-E consulta as informacoes atraves das ferramentas
         * locais ja existentes. Esta classificacao nao concede
         * permissoes nem adiciona acesso especial ao Android.
         */
        if (containsAny(text,
                "informacoes do aparelho",
                "informacao do aparelho",
                "dados do aparelho",
                "informacoes do celular",
                "informacao do celular",
                "dados do celular",
                "resumo do aparelho",
                "detalhes do aparelho",
                "qual e meu aparelho",
                "modelo do meu celular",
                "modelo do celular",
                "modelo do aparelho",
                "marca do celular",
                "qual android",
                "versao do android",
                "qual versao do android",
                "sistema do celular",
                "informacoes do dispositivo")) {

            return result(
                    Intent.GENERAL_CONVERSATION,
                    Specialist.DUM_E,
                    0.90f,
                    text,
                    "Consulta de informacoes do aparelho encaminhada ao DUM-E."
            );
        }

        /*
         * 8. Memoria persistente e historico.
         *
         * A FRIDAY ja implementa a consulta por meio do nucleo
         * e da memoria existente. Por isso, o especialista
         * recomendado passa a ser FRIDAY, evitando anunciar
         * um modulo MEMORY que nao esta registrado.
         *
         * A intencao MEMORY e preservada para compatibilidade.
         */
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
                    Specialist.FRIDAY,
                    0.88f,
                    text,
                    "Consulta de memoria encaminhada a FRIDAY, "
                            + "que reutiliza o armazenamento existente."
            );
        }

        // 9. Desenvolvimento do projeto e codigo.
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
                    "Mensagem relacionada ao desenvolvimento "
                            + "do JARVIS Lite ou a codigo."
            );
        }

        /*
         * 10. Perguntas que correspondem diretamente a assuntos
         * cadastrados na base de conhecimento offline.
         *
         * O modulo U tem prioridade para consultar a base existente.
         * Se nao encontrar resposta, pode devolver null e permitir
         * que a FRIDAY tente responder.
         *
         * Perguntas que incluem numeros sao deixadas para as
         * regras posteriores, para nao confundir um calculo
         * concreto com uma simples consulta de formula.
         */
        if (isKnownKnowledgeQuestion(text)) {
            return result(
                    Intent.GENERAL_CONVERSATION,
                    Specialist.U,
                    0.86f,
                    text,
                    "Pergunta reconhecida como candidata a consulta "
                            + "na base de conhecimento offline pelo modulo U."
            );
        }

        /*
         * 11. Calculos e matematica.
         *
         * Evita classificar toda frase que contenha "quanto e"
         * ou "vezes" como matematica.
         *
         * IMPORTANTE: este resultado ainda depende de um modulo
         * compativel com Specialist.CORE no fluxo VISION.
         */
        if (isLikelyMathematics(text)) {
            return result(
                    Intent.MATHEMATICS,
                    Specialist.CORE,
                    0.84f,
                    text,
                    "Possivel pedido de calculo ou resolucao matematica."
            );
        }

        // 12. Ajuda escolar.
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
                    "Pedido de explicacao, estudo ou ajuda escolar "
                            + "encaminhado a KAREN."
            );
        }

        // 13. Orientacao pratica e explicacoes graduais.
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
                    "Pedido de orientacao contextual ou explicacao "
                            + "passo a passo."
            );
        }

        /*
         * 14. Outras perguntas podem tentar primeiro a base
         * offline por meio do modulo U.
         *
         * O U so responde quando a base existente encontra algo.
         * Caso contrario, o nucleo pode continuar ate a FRIDAY,
         * que trata conversas gerais e pedidos de esclarecimento.
         */
        if (looksLikeKnowledgeQuestion(text)) {
            return result(
                    Intent.GENERAL_CONVERSATION,
                    Specialist.U,
                    0.70f,
                    text,
                    "Pergunta geral encaminhada primeiro ao modulo U "
                            + "para consulta offline, com fallback conversacional."
            );
        }

        // 15. Fallback conservador.
        return result(
                Intent.GENERAL_CONVERSATION,
                Specialist.FRIDAY,
                0.55f,
                text,
                "Nenhuma intencao especifica foi identificada "
                        + "com confianca suficiente."
        );
    }

    /** Alias em portugues para componentes que usam o nome analisar(). */
    public Analysis analisar(String mensagem) {
        return analyze(mensagem);
    }

    /**
     * Retorna o especialista recomendado.
     * A recomendacao nao concede permissoes ao modulo.
     */
    public Specialist recommendSpecialist(String message) {
        return analyze(message).getSpecialist();
    }

    /** Descricao legivel da classificacao para diagnostico. */
    public String describeIntent(String message) {
        Analysis analysis = analyze(message);

        return "Intencao: " + analysis.getIntent().name()
                + "; especialista: " + analysis.getSpecialist().name()
                + "; confianca: " + analysis.getConfidence()
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

    /**
     * Detecta perguntas que correspondem a assuntos ja cadastrados
     * na JarvisKnowledgeBase.
     *
     * Nao tenta resolver calculos que contenham valores numericos.
     */
    private static boolean isKnownKnowledgeQuestion(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        if (containsDigit(text)) {
            return false;
        }

        return containsAny(text,
                "quem descobriu o brasil",
                "quem descobriu brasil",
                "descobridor do brasil",
                "quando o brasil ficou independente",
                "quando foi a independencia do brasil",
                "capital do brasil",
                "qual planeta nos vivemos",
                "em qual planeta nos vivemos",
                "qual e o nosso planeta",
                "qual e a estrela do sistema solar",
                "qual estrela fica no centro do sistema solar",
                "qual e o planeta vermelho",
                "qual planeta e conhecido como planeta vermelho",
                "planeta vermelho",
                "fotossintese",
                "formula quimica da agua",
                "formula da agua",
                "formula da area do triangulo",
                "area do triangulo",
                "area de um triangulo",
                "formula da area do retangulo",
                "area do retangulo",
                "formula da area do quadrado",
                "area do quadrado",
                "formula do perimetro do quadrado",
                "perimetro de um quadrado",
                "formula da area do circulo",
                "area do circulo",
                "teorema de pitagoras",
                "o que e um substantivo",
                "explique o substantivo",
                "o que e um verbo",
                "explique o verbo");
    }

    /**
     * Identifica pedidos matematicos sem tratar qualquer ocorrencia
     * de "quanto e" ou "vezes" como um calculo.
     */
    private static boolean isLikelyMathematics(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        if (containsAny(text,
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
                "calculo matematico")) {
            return true;
        }

        return hasArithmeticExpression(text);
    }

    /**
     * Reconhece expressoes simples como "2 + 2", "3 x 4"
     * e "10 dividido por 2".
     */
    private static boolean hasArithmeticExpression(String text) {
        return text.matches(
                ".*\\d+\\s*(?:\\+|\\-|\\*|/|×|÷|x|vezes|"
                        + "dividido por|mais|menos)\\s*\\d+.*"
        );
    }

    private static boolean containsDigit(String text) {
        if (text == null) {
            return false;
        }

        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Detecta formatos comuns de perguntas factuais.
     */
    private static boolean looksLikeKnowledgeQuestion(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        if (text.contains("?")) {
            return true;
        }

        return startsWithAny(text,
                "quem ",
                "qual ",
                "quais ",
                "quando ",
                "onde ",
                "aonde ",
                "por que ",
                "porque ",
                "o que ",
                "o que e ",
                "como funciona ",
                "como surgiu ",
                "como acontece ",
                "explique ",
                "me diga ",
                "me conte ",
                "me fale sobre ",
                "quero saber ");
    }

    private static boolean isGreeting(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        String candidate = text
                .replaceAll("[!?.]+$", "")
                .trim();

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

    private static boolean startsWithAny(
            String text,
            String... prefixes
    ) {
        if (text == null
                || text.isEmpty()
                || prefixes == null) {
            return false;
        }

        for (String prefix : prefixes) {
            if (prefix != null
                    && !prefix.isEmpty()
                    && text.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Reconhece uma palavra isolada mesmo quando há pontuação
     * ao redor. Evita que "ram" seja detectado dentro de
     * palavras como "programa".
     */
    private static boolean containsWholeWord(
            String text,
            String word
    ) {
        if (text == null || text.trim().isEmpty()
                || word == null || word.trim().isEmpty()) {
            return false;
        }

        String cleanText = text
                .replaceAll("[^a-z0-9]+", " ")
                .trim();

        String cleanWord = normalize(word)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();

        if (cleanText.isEmpty() || cleanWord.isEmpty()) {
            return false;
        }

        return (" " + cleanText + " ")
                .contains(" " + cleanWord + " ");
    }

    private static boolean containsAny(
            String text,
            String... terms
    ) {
        if (text == null
                || text.isEmpty()
                || terms == null) {
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
     * Normaliza acentos usando a biblioteca padrao do Java.
     * Nao depende de internet nem de APIs especificas do Android.
     */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }

        String lower = text
                .trim()
                .toLowerCase(Locale.ROOT);

        String decomposed = Normalizer.normalize(
                lower,
                Normalizer.Form.NFD
        );

        String withoutMarks = decomposed.replaceAll(
                "\\p{M}+",
                ""
        );

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
