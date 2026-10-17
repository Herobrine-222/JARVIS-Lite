package com.jarvis.lite;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Classificador local e roteador deterministico do JARVIS Lite.
 *
 * Esta classe identifica intencoes por regras; nao e um modelo
 * generativo e nao executa comandos, altera o dispositivo ou acessa
 * sensores. A confianca e uma pontuacao heuristica, nao uma
 * probabilidade estatistica.
 *
 * Compatibilidade: preserva os enums, metodos publicos e getters
 * existentes para reduzir o impacto nos demais modulos do projeto.
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

    /** Resultado imutavel de uma classificacao. */
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

        /** Texto em minusculas, sem acentos e com espacos normalizados. */
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
     * Analisa a mensagem usando regras ordenadas por especificidade.
     * Consultas claras sobre o aparelho precedem conversa geral, para
     * que U/FRIDAY nao interceptem algo destinado ao DUM-E.
     */
    public Analysis analyze(String message) {
        final String text = normalize(message);

        if (text.isEmpty()) {
            return result(Intent.UNKNOWN, Specialist.CORE, 0.0f, text,
                    "Mensagem vazia ou sem conteudo util.");
        }

        // 1. Comandos de encerramento explicitos.
        if (containsAny(text,
                "encerrar sessao", "encerrar a sessao",
                "finalizar sessao", "terminar sessao",
                "jarvis dormir", "jarvis pode dormir",
                "voltar ao modo de espera", "entrar em modo de espera",
                "pode entrar em espera", "ativar modo de espera")) {
            return result(Intent.END_SESSION, Specialist.CORE, 0.98f, text,
                    "Pedido explicito para encerrar ou suspender a sessao.");
        }

        // 2. Saudacoes curtas, nao frases que apenas comecam com saudacao.
        if (isGreeting(text)) {
            return result(Intent.GREETING, Specialist.FRIDAY, 0.96f, text,
                    "Saudacao reconhecida.");
        }

        // 3. Verificacao informativa do aparelho. Nao e uma varredura antivirus.
        if (containsAny(text,
                "verificacao do aparelho", "verificar o aparelho",
                "verificar meu aparelho", "verificar aparelho",
                "verificar seguranca", "verificacao de seguranca",
                "verificacao basica", "fazer uma verificacao",
                "fazer verificacao do celular", "analisar seguranca do celular",
                "analisar a seguranca do celular", "ver se tem virus",
                "verificar se tem virus", "ver se meu celular tem virus",
                "celular com virus", "virus no celular", "procurar virus",
                "checar malware", "verificacao de malware", "verificar malware",
                "analisar malware", "meu aparelho esta seguro",
                "meu celular esta seguro")) {
            return result(Intent.DEVICE_CHECK, Specialist.DUM_E, 0.94f, text,
                    "Pedido de verificacao informativa do aparelho.");
        }

        // 4. Bateria, carga e temperatura divulgada pelas ferramentas locais.
        if (containsAny(text,
                "bateria", "por cento de bateria", "porcentagem da bateria",
                "percentual da bateria", "percentual de bateria",
                "nivel da bateria", "nivel de bateria", "status da bateria",
                "estado da bateria", "temperatura da bateria", "temperatura bateria",
                "temperatura do celular", "temperatura do aparelho",
                "celular esta carregando", "celular ta carregando",
                "esta carregando", "ta carregando", "esta na tomada",
                "estado de carregamento", "carregamento do celular",
                "quanto de bateria", "quanto resta de bateria",
                "carga do celular", "carga da bateria")) {
            return result(Intent.BATTERY, Specialist.DUM_E, 0.94f, text,
                    "Consulta local sobre bateria, carga ou temperatura.");
        }

        // 5. Armazenamento interno. Termos genericos como "memoria" nao
        // sao suficientes para distinguir armazenamento de RAM.
        if (containsAny(text,
                "armazenamento", "espaco livre", "espaco disponivel",
                "espaco no celular", "espaco no aparelho", "quanto de espaco",
                "quanto espaco", "quanto armazenamento", "memoria interna",
                "armazenamento interno", "armazenamento do celular",
                "armazenamento do aparelho", "espaco ocupado", "espaco utilizado",
                "espaco usado", "quanto resta de espaco", "memoria do celular cheia",
                "memoria interna cheia", "liberar espaco no celular")) {
            return result(Intent.STORAGE, Specialist.DUM_E, 0.93f, text,
                    "Consulta local sobre armazenamento do aparelho.");
        }

        // 6. RAM: esta regra vem antes da memoria persistente e de perguntas gerais.
        if (containsAny(text,
                "memoria ram", "ram disponivel", "ram utilizada", "ram usada",
                "ram total", "uso da ram", "uso de ram", "quanto de ram",
                "ram do celular", "ram do aparelho", "memoria de acesso aleatorio",
                "consumo de ram", "consumo da ram", "ver minha ram", "consultar ram")
                || containsWholeWord(text, "ram")) {
            return result(Intent.GENERAL_CONVERSATION, Specialist.DUM_E, 0.92f, text,
                    "Consulta sobre memoria RAM encaminhada ao DUM-E.");
        }

        // 7. Informacoes gerais e identificacao basica do aparelho.
        if (containsAny(text,
                "informacoes do aparelho", "informacao do aparelho",
                "dados do aparelho", "informacoes do celular",
                "informacao do celular", "dados do celular", "resumo do aparelho",
                "detalhes do aparelho", "qual e meu aparelho", "qual e o meu aparelho",
                "modelo do meu celular", "modelo do celular", "modelo do aparelho",
                "marca do meu celular", "marca do celular", "qual e a marca do celular",
                "qual android", "versao do android", "qual versao do android",
                "versao do sistema", "sistema do celular", "informacoes do dispositivo",
                "dados do dispositivo", "qual e a versao do meu android",
                "qual a versao do android", "qual e a versao do android",
                "qual e meu modelo", "identificar meu celular")) {
            return result(Intent.GENERAL_CONVERSATION, Specialist.DUM_E, 0.90f, text,
                    "Consulta de informacoes basicas do aparelho encaminhada ao DUM-E.");
        }

        // 8. Memoria persistente e historico: usa a memoria ja existente via FRIDAY.
        if (containsAny(text,
                "o que voce lembra de mim", "o que voce lembra sobre mim",
                "voce lembra de mim", "lembra de mim", "o que voce sabe sobre mim",
                "o que sabe sobre mim", "minhas lembrancas", "minha memoria",
                "memoria do jarvis", "memoria do sistema", "historico da conversa",
                "historico das conversas", "nossas conversas anteriores",
                "o que conversamos antes", "o que eu te contei",
                "o que eu falei anteriormente", "consultar minhas lembrancas",
                "pesquisar na minha memoria", "buscar na memoria do jarvis")) {
            return result(Intent.MEMORY, Specialist.FRIDAY, 0.88f, text,
                    "Consulta de memoria encaminhada a FRIDAY, que reutiliza o armazenamento existente.");
        }

        // 9. Desenvolvimento do projeto e codigo.
        if (containsAny(text,
                "jarvis lite", "nosso projeto", "nosso aplicativo", "nosso app",
                "codigo do jarvis", "programacao do jarvis", "programacao do aplicativo",
                "repositorio do projeto", "branch feature vision core", "github",
                "erro de compilacao", "erro no codigo", "corrigir esse codigo",
                "corrigir o codigo", "implementar um modulo", "criar um modulo java",
                "classe java", "arquivo java", "build do aplicativo")) {
            return result(Intent.PROJECT, Specialist.EDITH, 0.88f, text,
                    "Mensagem relacionada ao desenvolvimento do JARVIS Lite ou a codigo.");
        }

        // 10. Perguntas para as quais a base offline tem assuntos cadastrados.
        if (isKnownKnowledgeQuestion(text)) {
            return result(Intent.GENERAL_CONVERSATION, Specialist.U, 0.86f, text,
                    "Pergunta candidata a consulta na base de conhecimento offline pelo modulo U.");
        }

        // 11. Matematica: reconhece operacoes sem encaminhar qualquer frase para calculo.
        if (isLikelyMathematics(text)) {
            return result(Intent.MATHEMATICS, Specialist.CORE, 0.84f, text,
                    "Possivel pedido de calculo ou resolucao matematica.");
        }

        // 12. Ajuda escolar.
        if (containsAny(text,
                "atividade da escola", "dever de casa", "licao de casa",
                "me ajuda na escola", "me ajude na escola", "questao da escola",
                "questao escolar", "explica essa materia", "me ensina",
                "trabalho escolar", "estudar para a prova", "me ajuda a estudar",
                "ajuda com a tarefa", "ajuda com o dever", "exercicio da escola",
                "exercicio escolar", "resumo para a escola", "me ajuda na atividade")) {
            return result(Intent.SCHOOL_HELP, Specialist.KAREN, 0.86f, text,
                    "Pedido de explicacao, estudo ou ajuda escolar encaminhado a KAREN.");
        }

        // 13. Orientacao e explicacao passo a passo.
        if (containsAny(text,
                "passo a passo", "me explica", "me explique", "explica melhor",
                "explique melhor", "como eu faco", "como fazer", "como resolver",
                "nao entendi", "nao consigo entender", "estou com dificuldade",
                "tenho dificuldade", "me orienta", "me orientar", "ajuda a entender",
                "me ajuda a entender", "pode explicar", "pode simplificar",
                "simplifica para mim", "explicacao simples", "explicacao detalhada",
                "por onde comeco", "como eu comeco", "qual o primeiro passo",
                "me ajuda a planejar", "me ajuda a organizar", "quero aprender",
                "me da um exemplo", "me de um exemplo", "me ajuda com isso",
                "me ajude com isso")) {
            return result(Intent.GUIDANCE, Specialist.KAREN, 0.84f, text,
                    "Pedido de orientacao contextual ou explicacao passo a passo.");
        }

        // 14. Perguntas factuais gerais tentam U primeiro; FRIDAY continua como fallback.
        if (looksLikeKnowledgeQuestion(text)) {
            return result(Intent.GENERAL_CONVERSATION, Specialist.U, 0.70f, text,
                    "Pergunta geral encaminhada ao modulo U para consulta offline, com fallback conversacional.");
        }

        // 15. Fallback conservador.
        return result(Intent.GENERAL_CONVERSATION, Specialist.FRIDAY, 0.55f, text,
                "Nenhuma intencao especifica foi identificada com confianca suficiente.");
    }

    /** Alias em portugues mantido por compatibilidade. */
    public Analysis analisar(String mensagem) {
        return analyze(mensagem);
    }

    /** Retorna o especialista recomendado, sem executar o modulo. */
    public Specialist recommendSpecialist(String message) {
        return analyze(message).getSpecialist();
    }

    /** Diagnostico legivel; nao inclui logs externos nem dados do dispositivo. */
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
        return new Analysis(intent, specialist, confidence, normalizedText, reason);
    }

    private static boolean isKnownKnowledgeQuestion(String text) {
        if (text == null || text.isEmpty() || containsDigit(text)) {
            return false;
        }

        return containsAny(text,
                "quem descobriu o brasil", "quem descobriu brasil",
                "descobridor do brasil", "quando o brasil ficou independente",
                "quando foi a independencia do brasil", "capital do brasil",
                "qual planeta nos vivemos", "em qual planeta nos vivemos",
                "qual e o nosso planeta", "qual e a estrela do sistema solar",
                "qual estrela fica no centro do sistema solar", "qual e o planeta vermelho",
                "qual planeta e conhecido como planeta vermelho", "planeta vermelho",
                "fotossintese", "formula quimica da agua", "formula da agua",
                "formula da area do triangulo", "area do triangulo", "area de um triangulo",
                "formula da area do retangulo", "area do retangulo",
                "formula da area do quadrado", "area do quadrado",
                "formula do perimetro do quadrado", "perimetro de um quadrado",
                "formula da area do circulo", "area do circulo", "teorema de pitagoras",
                "o que e um substantivo", "explique o substantivo",
                "o que e um verbo", "explique o verbo");
    }

    private static boolean isLikelyMathematics(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        if (containsAny(text,
                "calcule", "calcular", "faz a conta", "faca a conta",
                "resultado de", "raiz quadrada", "porcentagem de", "equacao",
                "resolver a equacao", "resolva a equacao", "somar", "subtrair",
                "multiplicar", "dividir", "dividido por", "calculo matematico",
                "calcule quanto", "resolva esta conta", "resolva essa conta")) {
            return true;
        }

        return hasArithmeticExpression(text);
    }

    /** Reconhece expressoes aritmeticas simples, sem avaliar a conta. */
    private static boolean hasArithmeticExpression(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        return text.matches(".*\\d+(?:[.,]\\d+)?\\s*(?:\\+|\\-|\\*|/|×|÷|x|vezes|dividido por|mais|menos)\\s*\\d+(?:[.,]\\d+)?.*");
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

    private static boolean looksLikeKnowledgeQuestion(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        if (text.indexOf('?') >= 0) {
            return true;
        }

        return startsWithAny(text,
                "quem ", "qual ", "quais ", "quando ", "onde ", "aonde ",
                "por que ", "porque ", "o que ", "como funciona ",
                "como surgiu ", "como acontece ", "explique ", "me diga ",
                "me conte ", "me fale sobre ", "quero saber ", "quanto ",
                "quantos ", "quantas ", "qual e ", "qual a ", "qual o ");
    }

    private static boolean isGreeting(String text) {
        String candidate = canonicalWords(text);
        return equalsAny(candidate,
                "oi", "ola", "e ai", "bom dia", "boa tarde", "boa noite",
                "opa", "tudo bem", "oi jarvis", "ola jarvis", "opa jarvis",
                "fala jarvis", "salve", "e ai jarvis");
    }

    private static boolean startsWithAny(String text, String... prefixes) {
        if (text == null || text.isEmpty() || prefixes == null) {
            return false;
        }
        for (String prefix : prefixes) {
            if (prefix != null && !prefix.isEmpty() && text.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** Busca uma frase inteira, evitando falsos positivos em partes de palavras. */
    private static boolean containsAny(String text, String... terms) {
        if (text == null || text.trim().isEmpty() || terms == null) {
            return false;
        }

        String searchable = " " + canonicalWords(text) + " ";
        for (String term : terms) {
            if (term == null || term.trim().isEmpty()) {
                continue;
            }
            String phrase = canonicalWords(term);
            if (!phrase.isEmpty() && searchable.contains(" " + phrase + " ")) {
                return true;
            }
        }
        return false;
    }

    /** Verifica uma palavra isolada (por exemplo, RAM). */
    private static boolean containsWholeWord(String text, String word) {
        if (text == null || word == null || text.trim().isEmpty() || word.trim().isEmpty()) {
            return false;
        }
        String searchable = " " + canonicalWords(text) + " ";
        String target = canonicalWords(word);
        return !target.isEmpty() && searchable.contains(" " + target + " ");
    }

    private static boolean equalsAny(String text, String... candidates) {
        if (text == null || candidates == null) {
            return false;
        }
        for (String candidate : candidates) {
            if (candidate != null && text.equals(canonicalWords(candidate))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Normaliza acentos e caixa, mas preserva ?, operadores aritmeticos
     * e os numeros para que a deteccao de perguntas e contas continue
     * funcionando. A busca por frases usa canonicalWords() separadamente.
     */
    private static String normalize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String decomposed = Normalizer.normalize(
                text.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );
        String withoutMarks = decomposed.replaceAll("\\p{M}+", "");

        return withoutMarks
                .replaceAll("[\\p{Cc}&&[^\\t]]", " ")
                .replaceAll("[^a-z0-9?+\\-*/×÷.,%x]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Converte pontuacao em separadores para comparacao de palavras completas. */
    private static String canonicalWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        String decomposed = Normalizer.normalize(
                text.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );
        String withoutMarks = decomposed.replaceAll("\\p{M}+", "");
        return withoutMarks
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static float clamp(float value, float minimum, float maximum) {
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
