
package com.jarvis.lite;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Classificador local e roteador deterministico do JARVIS Lite.
 *
 * Responsabilidades:
 * - identificar intencoes do usuario;
 * - recomendar o especialista mais adequado;
 * - reconhecer consultas locais do dispositivo;
 * - identificar pedidos de matematica e ajuda escolar;
 * - reconhecer consultas de memoria e do projeto;
 * - manter compatibilidade com os modulos existentes.
 *
 * Limites:
 * - nao e um modelo generativo;
 * - nao calcula expressoes;
 * - nao executa comandos Android;
 * - nao acessa sensores, arquivos ou internet;
 * - nao garante que uma resposta esteja factualmente correta.
 *
 * A confianca e uma pontuacao heuristica, nao uma probabilidade.
 */
public final class JarvisReasoningEngine {

    private static final int MAX_INPUT_LENGTH = 4000;

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

    public enum Specialist {
        CORE,
        FRIDAY,
        KAREN,
        EDITH,
        DUM_E,
        U,
        MEMORY
    }

    /**
     * Resultado imutavel de uma classificacao.
     * Os getters publicos sao preservados para compatibilidade.
     */
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
                    ? Intent.UNKNOWN : intent;

            this.specialist = specialist == null
                    ? Specialist.CORE : specialist;

            this.confidence = clamp(confidence, 0.0f, 1.0f);

            this.normalizedText = normalizedText == null
                    ? "" : normalizedText;

            this.reason = reason == null || reason.trim().isEmpty()
                    ? "Analise concluida." : reason.trim();
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
     * Classifica uma mensagem sem executar a solicitacao.
     *
     * A ordem das regras e intencional:
     * comandos explicitos e tarefas especificas sao avaliados
     * antes da conversa geral.
     */
    public Analysis analyze(String message) {
        String text = normalize(message);

        if (text.isEmpty()) {
            return result(
                    Intent.UNKNOWN,
                    Specialist.CORE,
                    0.0f,
                    text,
                    "Mensagem vazia ou sem conteudo util."
            );
        }

        if (text.length() > MAX_INPUT_LENGTH) {
            return result(
                    Intent.UNKNOWN,
                    Specialist.CORE,
                    0.0f,
                    text.substring(0, MAX_INPUT_LENGTH),
                    "Mensagem excede o limite de classificacao."
            );
        }

        // 1. Encerramento explicito da sessao.
        if (isEndSessionRequest(text)) {
            return result(
                    Intent.END_SESSION,
                    Specialist.CORE,
                    0.98f,
                    text,
                    "Pedido explicito para encerrar ou suspender a sessao."
            );
        }

        // 2. Saudacoes isoladas.
        if (isGreeting(text)) {
            return result(
                    Intent.GREETING,
                    Specialist.FRIDAY,
                    0.96f,
                    text,
                    "Saudacao reconhecida."
            );
        }

        // 3. Verificacao informativa de seguranca do aparelho.
        // Isso nao significa uma varredura antivírus completa.
        if (isDeviceCheckRequest(text)) {
            return result(
                    Intent.DEVICE_CHECK,
                    Specialist.DUM_E,
                    0.94f,
                    text,
                    "Pedido de verificacao informativa do aparelho."
            );
        }

        // 4. Consultas especificas sobre bateria.
        if (isBatteryRequest(text)) {
            return result(
                    Intent.BATTERY,
                    Specialist.DUM_E,
                    0.94f,
                    text,
                    "Consulta local sobre bateria ou carregamento."
            );
        }

        // 5. Armazenamento interno.
        if (isStorageRequest(text)) {
            return result(
                    Intent.STORAGE,
                    Specialist.DUM_E,
                    0.93f,
                    text,
                    "Consulta local sobre armazenamento."
            );
        }

        // 6. RAM. A intencao antiga e preservada para nao
        // quebrar consumidores que dependem dos enums existentes.
        if (isRamRequest(text)) {
            return result(
                    Intent.GENERAL_CONVERSATION,
                    Specialist.DUM_E,
                    0.92f,
                    text,
                    "Consulta de RAM encaminhada ao DUM-E."
            );
        }

        // 7. Informacoes basicas do aparelho.
        if (isDeviceInfoRequest(text)) {
            return result(
                    Intent.GENERAL_CONVERSATION,
                    Specialist.DUM_E,
                    0.90f,
                    text,
                    "Consulta de informacoes do dispositivo encaminhada ao DUM-E."
            );
        }

        // 8. Memoria persistente e historico.
        if (isMemoryRequest(text)) {
            return result(
                    Intent.MEMORY,
                    Specialist.FRIDAY,
                    0.88f,
                    text,
                    "Consulta de memoria ou historico."
            );
        }

        // 9. Desenvolvimento do JARVIS Lite.
        if (isProjectRequest(text)) {
            return result(
                    Intent.PROJECT,
                    Specialist.EDITH,
                    0.90f,
                    text,
                    "Solicitacao relacionada ao projeto ou codigo."
            );
        }

        // 10. Matematica explicita vem antes de perguntas gerais.
        if (isLikelyMathematics(text)) {
            return result(
                    Intent.MATHEMATICS,
                    Specialist.CORE,
                    0.86f,
                    text,
                    "Possivel pedido de calculo ou resolucao matematica."
            );
        }

        // 11. Perguntas conhecidas da base offline.
        if (isKnownKnowledgeQuestion(text)) {
            return result(
                    Intent.GENERAL_CONVERSATION,
                    Specialist.U,
                    0.86f,
                    text,
                    "Pergunta candidata a base de conhecimento offline."
            );
        }

        // 12. Ajuda escolar.
        if (isSchoolHelpRequest(text)) {
            return result(
                    Intent.SCHOOL_HELP,
                    Specialist.KAREN,
                    0.86f,
                    text,
                    "Pedido de ajuda escolar encaminhado a KAREN."
            );
        }

        // 13. Orientacao e explicacoes passo a passo.
        if (isGuidanceRequest(text)) {
            return result(
                    Intent.GUIDANCE,
                    Specialist.KAREN,
                    0.84f,
                    text,
                    "Pedido de orientacao ou explicacao."
            );
        }

        // 14. Pergunta factual geral.
        // Nao usamos apenas '?' como criterio: isso confundiria
        // perguntas sociais e tarefas locais com perguntas factuais.
        if (looksLikeKnowledgeQuestion(text)) {
            return result(
                    Intent.GENERAL_CONVERSATION,
                    Specialist.U,
                    0.70f,
                    text,
                    "Possivel pergunta factual para consulta offline."
            );
        }

        // 15. Conversa geral.
        return result(
                Intent.GENERAL_CONVERSATION,
                Specialist.FRIDAY,
                0.55f,
                text,
                "Nenhuma intencao especifica foi identificada."
        );
    }

    /** Alias em portugues preservado. */
    public Analysis analisar(String mensagem) {
        return analyze(mensagem);
    }

    /** Recomenda um especialista sem executar o modulo. */
    public Specialist recommendSpecialist(String message) {
        return analyze(message).getSpecialist();
    }

    /** Retorna um diagnostico textual da classificacao. */
    public String describeIntent(String message) {
        Analysis analysis = analyze(message);

        return "Intencao: " + analysis.getIntent().name()
                + "; especialista: "
                + analysis.getSpecialist().name()
                + "; confianca: "
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
                confidence,
                normalizedText,
                reason
        );
    }

    // ============================================================
    // REGRAS DE CLASSIFICACAO
    // ============================================================

    private static boolean isEndSessionRequest(String text) {
        return containsAny(text,
                "encerrar sessao",
                "encerrar a sessao",
                "finalizar sessao",
                "finalizar a sessao",
                "terminar sessao",
                "terminar a sessao",
                "jarvis dormir",
                "jarvis pode dormir",
                "voltar ao modo de espera",
                "entrar em modo de espera",
                "pode entrar em espera",
                "ativar modo de espera",
                "encerrar modo ativo",
                "jarvis encerrar sessao");
    }

    private static boolean isGreeting(String text) {
        String candidate = canonicalWords(text);

        return equalsAny(candidate,
                "oi",
                "ola",
                "e ai",
                "bom dia",
                "boa tarde",
                "boa noite",
                "opa",
                "tudo bem",
                "oi jarvis",
                "ola jarvis",
                "opa jarvis",
                "fala jarvis",
                "salve",
                "e ai jarvis",
                "bom dia jarvis",
                "boa tarde jarvis",
                "boa noite jarvis");
    }

    private static boolean isDeviceCheckRequest(String text) {
        return containsAny(text,
                "verificacao do aparelho",
                "verificar o aparelho",
                "verificar meu aparelho",
                "verificar aparelho",
                "verificar seguranca",
                "verificacao de seguranca",
                "verificacao basica",
                "fazer uma verificacao",
                "fazer verificacao do celular",
                "analisar seguranca do celular",
                "analisar a seguranca do celular",
                "ver se tem virus",
                "verificar se tem virus",
                "ver se meu celular tem virus",
                "verificar se meu celular tem virus",
                "celular com virus",
                "virus no celular",
                "procurar virus",
                "checar malware",
                "verificacao de malware",
                "verificar malware",
                "analisar malware",
                "meu aparelho esta seguro",
                "meu celular esta seguro",
                "verificar aplicativos e permissoes",
                "analisar aplicativos instalados",
                "verificar permissoes dos aplicativos");
    }

    private static boolean isBatteryRequest(String text) {
        return containsAny(text,
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
                "temperatura do aparelho",
                "celular esta carregando",
                "celular ta carregando",
                "esta carregando",
                "ta carregando",
                "esta na tomada",
                "estado de carregamento",
                "carregamento do celular",
                "quanto de bateria",
                "quanto resta de bateria",
                "carga do celular",
                "carga da bateria",
                "consultar bateria",
                "ver bateria",
                "verificar bateria",
                "bateria restante",
                "bateria atual",
                "bateria do meu celular",
                "bateria do aparelho")
                || equalsAny(canonicalWords(text),
                "bateria",
                "temperatura da bateria",
                "nivel da bateria");
    }

    private static boolean isStorageRequest(String text) {
        return containsAny(text,
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
                "espaco usado",
                "quanto resta de espaco",
                "memoria do celular cheia",
                "memoria interna cheia",
                "liberar espaco no celular",
                "consultar armazenamento",
                "ver armazenamento",
                "armazenamento disponivel",
                "armazenamento livre",
                "armazenamento usado");
    }

    private static boolean isRamRequest(String text) {
        return containsAny(text,
                "memoria ram",
                "ram disponivel",
                "ram utilizada",
                "ram utilizada",
                "ram usada",
                "ram total",
                "uso da ram",
                "uso de ram",
                "quanto de ram",
                "ram do celular",
                "ram do aparelho",
                "memoria de acesso aleatorio",
                "consumo de ram",
                "consumo da ram",
                "ver minha ram",
                "consultar ram",
                "consultar memoria ram",
                "ver memoria ram",
                "memoria ram disponivel",
                "memoria ram usada",
                "memoria ram total",
                "uso de memoria ram")
                || containsWholeWord(text, "ram");
    }

    private static boolean isDeviceInfoRequest(String text) {
        return containsAny(text,
                "informacoes do aparelho",
                "informacao do aparelho",
                "dados do aparelho",
                "informacoes do celular",
                "informacao do celular",
                "dados do celular",
                "resumo do aparelho",
                "detalhes do aparelho",
                "qual e meu aparelho",
                "qual e o meu aparelho",
                "modelo do meu celular",
                "modelo do celular",
                "modelo do aparelho",
                "marca do meu celular",
                "marca do celular",
                "qual e a marca do celular",
                "qual android",
                "versao do android",
                "qual versao do android",
                "versao do sistema",
                "sistema do celular",
                "informacoes do dispositivo",
                "dados do dispositivo",
                "qual e a versao do meu android",
                "qual a versao do android",
                "qual e a versao do android",
                "qual e meu modelo",
                "identificar meu celular",
                "informacoes basicas do celular",
                "informacoes basicas do aparelho");
    }

    private static boolean isMemoryRequest(String text) {
        return containsAny(text,
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
                "consultar minhas lembrancas",
                "pesquisar na minha memoria",
                "buscar na memoria do jarvis",
                "o que voce guardou",
                "o que voce salvou na memoria",
                "quais informacoes voce lembra");
    }

    private static boolean isProjectRequest(String text) {
        return containsAny(text,
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
                "criar um modulo java",
                "classe java",
                "arquivo java",
                "build do aplicativo",
                "construir o aplicativo",
                "compilar o jarvis",
                "erro no build",
                "modulo especialista",
                "integracao dos modulos",
                "arquitetura do jarvis");
    }

    private static boolean isSchoolHelpRequest(String text) {
        return containsAny(text,
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
                "exercicio escolar",
                "resumo para a escola",
                "me ajuda na atividade",
                "resolver atividade escolar",
                "explicar materia escolar");
    }

    private static boolean isGuidanceRequest(String text) {
        return containsAny(text,
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
                "me ajude com isso");
    }

    // ============================================================
    // MATEMATICA E CONHECIMENTO OFFLINE
    // ============================================================

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
                "calculo matematico",
                "calcule quanto",
                "resolva esta conta",
                "resolva essa conta",
                "quanto e",
                "quanto da",
                "qual e o resultado",
                "faca esse calculo",
                "resolva essa expressao")) {
            return true;
        }

        return hasArithmeticExpression(text);
    }

    /**
     * Detecta expressoes aritmeticas simples.
     * Nao executa nem valida matematicamente a operacao.
     */
    private static boolean hasArithmeticExpression(String text) {
        if (text == null || text.trim().isEmpty()
                || text.length() > 500) {
            return false;
        }

        String candidate = canonicalWords(text);

        // Expressao simbolica, como 12 + 8 ou 20 / 4.
        if (text.matches(
                ".*\\d+(?:[.,]\\d+)?\\s*" +
                "(?:\\+|\\-|\\*|/|×|÷|%|\\bx\\b)" +
                "\\s*\\d+(?:[.,]\\d+)?.*")) {
            return true;
        }

        // Operacoes escritas por extenso entre dois numeros.
        return candidate.matches(
                ".*\\d+(?:[.,]\\d+)?\\s+" +
                "(?:mais|menos|vezes|dividido por|multiplicado por)" +
                "\\s+\\d+(?:[.,]\\d+)?.*");
    }

    private static boolean isKnownKnowledgeQuestion(String text) {
        if (text == null || text.isEmpty()
                || containsDigit(text)) {
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

    private static boolean looksLikeKnowledgeQuestion(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String candidate = canonicalWords(text);

        if (candidate.isEmpty()) {
            return false;
        }

        // Nao tratar perguntas sociais como perguntas factuais.
        if (containsAny(candidate,
                "tudo bem",
                "como voce esta",
                "como voce ta",
                "como voce se sente",
                "voce esta ai",
                "jarvis esta ai")) {
            return false;
        }

        return startsWithAny(candidate,
                "quem ",
                "qual ",
                "quais ",
                "quando ",
                "onde ",
                "aonde ",
                "por que ",
                "porque ",
                "o que ",
                "como funciona ",
                "como surgiu ",
                "como acontece ",
                "explique ",
                "me diga ",
                "me conte ",
                "me fale sobre ",
                "quero saber ",
                "quanto ",
                "quantos ",
                "quantas ");
    }

    // ============================================================
    // NORMALIZACAO E UTILITARIOS
    // ============================================================

    private static boolean startsWithAny(
            String text,
            String... prefixes
    ) {
        if (text == null || text.isEmpty() || prefixes == null) {
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
     * Busca frases completas para evitar correspondencias
     * acidentais dentro de palavras maiores.
     */
    private static boolean containsAny(
            String text,
            String... terms
    ) {
        if (text == null || text.trim().isEmpty()
                || terms == null) {
            return false;
        }

        String searchable = " " + canonicalWords(text) + " ";

        for (String term : terms) {
            if (term == null || term.trim().isEmpty()) {
                continue;
            }

            String phrase = canonicalWords(term);

            if (!phrase.isEmpty()
                    && searchable.contains(" " + phrase + " ")) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsWholeWord(
            String text,
            String word
    ) {
        if (text == null || word == null
                || text.trim().isEmpty()
                || word.trim().isEmpty()) {
            return false;
        }

        String searchable = " " + canonicalWords(text) + " ";
        String target = canonicalWords(word);

        return !target.isEmpty()
                && searchable.contains(" " + target + " ");
    }

    private static boolean equalsAny(
            String text,
            String... candidates
    ) {
        if (text == null || candidates == null) {
            return false;
        }

        for (String candidate : candidates) {
            if (candidate != null
                    && text.equals(canonicalWords(candidate))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Normaliza acentos, caixa e espacos.
     * Preserva operadores e pontuacao relevante para perguntas
     * e expressoes matematicas.
     */
    private static String normalize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String decomposed = Normalizer.normalize(
                text.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );

        String withoutMarks =
                decomposed.replaceAll("\\p{M}+", "");

        return withoutMarks
                .replaceAll("[\\p{Cc}&&[^\\t]]", " ")
                .replaceAll("[^a-z0-9?+\\-*/×÷.,%x]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Normalizacao para comparar palavras e frases.
     * Remove pontuacao, preservando letras e numeros.
     */
    private static String canonicalWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String decomposed = Normalizer.normalize(
                text.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );

        String withoutMarks =
                decomposed.replaceAll("\\p{M}+", "");

        return withoutMarks
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
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

        return Math.max(minimum, Math.min(maximum, value));
    }
}
