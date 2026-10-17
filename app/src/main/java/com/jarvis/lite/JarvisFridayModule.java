
package com.jarvis.lite;

import java.text.Normalizer;
import java.util.Locale;

/**
 * FRIDAY — módulo conversacional do JARVIS Lite VISION.
 *
 * Responsabilidades:
 * - responder a saudações e interações conversacionais simples;
 * - identificar perguntas sobre a própria FRIDAY e suas capacidades;
 * - consultar a memória já existente, sem criar outro armazenamento;
 * - aproveitar a base de conhecimento offline existente;
 * - esclarecer limitações sem fingir possuir recursos não implementados;
 * - devolver null quando a análise não pertencer a este módulo.
 *
 * Este módulo não é um modelo generativo de linguagem.
 * Ele utiliza regras locais e os serviços existentes no VISION.
 */
public final class JarvisFridayModule implements JarvisVisionModule {

    private static final String MEMORY_NOT_FOUND =
            "A consulta não retornou lembranças relevantes "
            + "na memória disponível do JARVIS Lite. "
            + "Isso pode significar que não existe um registro "
            + "correspondente ou que a busca não reconheceu "
            + "a forma como a pergunta foi feita.";

    private static final String IDENTITY_RESPONSE =
            "Sou a FRIDAY, o módulo conversacional do VISION "
            + "dentro do JARVIS Lite. Meu comportamento nesta "
            + "versão utiliza regras locais, classificação de "
            + "mensagens e recursos já implementados no projeto. "
            + "Não sou, por mim mesma, um modelo generativo "
            + "completo de inteligência artificial.";

    private static final String CAPABILITIES_RESPONSE =
            "Posso responder a saudações, consultar a memória "
            + "existente e aproveitar respostas disponíveis na "
            + "base de conhecimento offline. O VISION também "
            + "possui especialistas para determinados pedidos, "
            + "como cálculos, ajuda escolar, desenvolvimento "
            + "do projeto e algumas informações do aparelho. "
            + "O resultado depende da classificação da mensagem "
            + "e dos recursos conectados.";

    private static final String ONLINE_LIMITATION_RESPONSE =
            "Este módulo da FRIDAY não consulta serviços online "
            + "por conta própria nesta versão. Informações "
            + "atualizadas, como notícias e previsão do tempo, "
            + "dependem de uma integração online funcional "
            + "e conectada ao fluxo correspondente do aplicativo.";

    @Override
    public String getName() {
        return "FRIDAY";
    }

    /**
     * A FRIDAY trata apenas intenções conversacionais e consultas
     * à memória. Não deve interceptar intenções de outros módulos.
     */
    @Override
    public boolean supports(
            JarvisReasoningEngine.Analysis analysis
    ) {
        if (analysis == null || analysis.getIntent() == null) {
            return false;
        }

        switch (analysis.getIntent()) {
            case GREETING:
            case GENERAL_CONVERSATION:
            case MEMORY:
                return true;

            default:
                return false;
        }
    }

    @Override
    public String process(
            String input,
            JarvisReasoningEngine.Analysis analysis,
            JarvisVisionCore core
    ) {
        if (input == null
                || input.trim().isEmpty()
                || analysis == null
                || !supports(analysis)) {
            return null;
        }

        String normalized = normalize(input);

        if (normalized.isEmpty()) {
            return null;
        }

        switch (analysis.getIntent()) {
            case GREETING:
                return respondToGreeting(normalized);

            case MEMORY:
                return respondToMemory(input, core);

            case GENERAL_CONVERSATION:
                return respondToGeneralConversation(
                        input,
                        normalized,
                        core
                );

            default:
                return null;
        }
    }

    // ============================================================
    // SAUDAÇÕES
    // ============================================================

    private String respondToGreeting(String text) {
        String clean = stripTrailingPunctuation(text);

        if (containsAny(clean,
                "bom dia",
                "bom dia jarvis")) {
            return "Bom dia! Estou pronta para ajudar. "
                    + "O que você precisa hoje?";
        }

        if (containsAny(clean,
                "boa tarde",
                "boa tarde jarvis")) {
            return "Boa tarde! Como posso ajudar?";
        }

        if (containsAny(clean,
                "boa noite",
                "boa noite jarvis")) {
            return "Boa noite! Como posso ajudar?";
        }

        if (containsAny(clean,
                "tudo bem",
                "tudo bem jarvis")) {
            return "Tudo certo por aqui. Obrigada por perguntar! "
                    + "O que você gostaria de fazer?";
        }

        if (containsAny(clean, "opa", "e ai")) {
            return "Opa! Estou aqui para ajudar. "
                    + "Pode mandar sua pergunta.";
        }

        if (containsAny(clean, "oi", "ola")) {
            return "Olá! Sou a FRIDAY do JARVIS Lite. "
                    + "Como posso ajudar?";
        }

        // Mantém uma resposta segura para futuras saudações
        // que sejam encaminhadas corretamente pelo classificador.
        return "Olá! Como posso ajudar você hoje?";
    }

    // ============================================================
    // CONSULTA À MEMÓRIA EXISTENTE
    // ============================================================

    private String respondToMemory(
            String input,
            JarvisVisionCore core
    ) {
        if (core == null) {
            return "A consulta à memória não está disponível "
                    + "nesta chamada do VISION.";
        }

        /*
         * Reutiliza o armazenamento existente por meio do núcleo.
         * Não cria uma nova instância de JarvisMemory nem um
         * segundo banco SQLite.
         */
        String context = core.getRelevantMemory(input);

        if (context == null || context.trim().isEmpty()) {
            return MEMORY_NOT_FOUND;
        }

        return "Encontrei estas informações na memória "
                + "disponível do JARVIS Lite:\n"
                + context.trim();
    }

    // ============================================================
    // CONVERSA GERAL
    // ============================================================

    private String respondToGeneralConversation(
            String originalInput,
            String text,
            JarvisVisionCore core
    ) {
        // Identidade e capacidades do próprio módulo.
        if (containsAny(text,
                "quem e voce",
                "qual e seu nome",
                "como voce se chama",
                "quem e a friday",
                "o que e a friday",
                "me fale sobre voce",
                "me fala sobre voce",
                "quem e voce jarvis")) {
            return IDENTITY_RESPONSE;
        }

        if (containsAny(text,
                "o que voce pode fazer",
                "quais sao suas funcoes",
                "quais suas funcoes",
                "como voce pode me ajudar",
                "para que voce serve",
                "suas capacidades",
                "o que voce sabe fazer")) {
            return CAPABILITIES_RESPONSE;
        }

        if (containsAny(text,
                "voce e uma ia",
                "voce e inteligencia artificial",
                "voce e uma inteligencia artificial")) {
            return "Sou um módulo de software do JARVIS Lite, "
                    + "parte do sistema VISION. Utilizo regras "
                    + "locais e recursos do projeto; isso não "
                    + "significa que eu tenha, sozinha, todas "
                    + "as capacidades de uma IA generativa.";
        }

        if (containsAny(text,
                "voce tem internet",
                "voce acessa a internet",
                "voce pesquisa na internet",
                "noticias de hoje",
                "ultimas noticias",
                "previsao do tempo",
                "clima de hoje",
                "clima agora",
                "tempo agora")) {
            return ONLINE_LIMITATION_RESPONSE;
        }

        // A FRIDAY consulta a memória, mas não grava lembranças
        // automaticamente por meio deste módulo.
        if (containsAny(text,
                "voce pode memorizar",
                "voce consegue memorizar",
                "voce pode guardar isso",
                "voce vai lembrar disso",
                "voce consegue aprender com nossas conversas")) {
            return "Este módulo consegue consultar a memória "
                    + "já existente, mas não grava uma nova "
                    + "lembrança automaticamente por conta própria. "
                    + "O salvamento depende do fluxo de memória "
                    + "conectado ao aplicativo.";
        }

        // Agradecimentos.
        if (containsAny(text,
                "obrigado",
                "obrigada",
                "muito obrigado",
                "muito obrigada",
                "valeu",
                "agradeco",
                "agradeco pela ajuda")) {
            return "Por nada! Quando precisar, pode mandar "
                    + "a próxima pergunta.";
        }

        // Confirmações curtas, quando classificadas como conversa geral.
        if (equalsAny(
                stripTrailingPunctuation(text),
                "ok",
                "okay",
                "certo",
                "entendi",
                "beleza",
                "perfeito",
                "combinado")) {
            return "Certo! Pode continuar quando estiver pronto.";
        }

        /*
         * Antes de pedir para o usuário reformular, consulta a base
         * de conhecimento offline já implementada no projeto.
         * Assim, perguntas conhecidas podem receber respostas sem
         * precisar de internet ou de um modelo generativo.
         */
        if (core != null) {
            String knowledgeAnswer =
                    core.answerFromExistingKnowledgeBase(originalInput);

            if (knowledgeAnswer != null
                    && !knowledgeAnswer.trim().isEmpty()) {
                return knowledgeAnswer.trim();
            }
        }

        // Pedido genérico de ajuda.
        if (containsAny(text,
                "pode me ajudar",
                "pode me ajudar com isso",
                "preciso de ajuda",
                "me ajuda",
                "me ajude",
                "quero sua ajuda")) {
            return "Claro! Explique o que você precisa fazer "
                    + "ou entender. Vou tentar identificar "
                    + "o tipo de pedido e encaminhá-lo ao "
                    + "especialista disponível mais adequado.";
        }

        /*
         * Resposta de contingência: não inventa fatos nem afirma
         * ter entendido algo que o classificador não identificou.
         */
        return "Ainda não consegui identificar com clareza "
                + "o que você quer saber. Pode reformular a "
                + "pergunta ou explicar qual resultado espera? "
                + "Vou tentar encaminhar o pedido corretamente.";
    }

    // ============================================================
    // NORMALIZAÇÃO
    // ============================================================

    /**
     * Padroniza texto em português sem depender da internet
     * ou de APIs específicas do Android.
     */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }

        String lower = text.toLowerCase(Locale.ROOT);

        String decomposed = Normalizer.normalize(
                lower,
                Normalizer.Form.NFD
        );

        return decomposed
                .replaceAll("\\p{M}+", "")
                .replaceAll("[\\p{Cc}&&[^\\t]]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String stripTrailingPunctuation(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replaceAll("[.!?,;:]+$", "")
                .trim();
    }

    private static boolean containsAny(
            String text,
            String... terms
    ) {
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

    private static boolean equalsAny(
            String text,
            String... terms
    ) {
        if (text == null || terms == null) {
            return false;
        }

        for (String term : terms) {
            String normalizedTerm = normalize(term);

            if (text.equals(normalizedTerm)) {
                return true;
            }
        }

        return false;
    }
}
