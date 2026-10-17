
package com.jarvis.lite;

/**
 * KAREN: especialista em orientação contextual e explicações.
 *
 * Responsabilidades:
 * - oferecer orientações práticas e graduais;
 * - ajudar com dúvidas escolares;
 * - consultar a base de conhecimento offline já existente;
 * - explicar limitações sem inventar informações;
 * - manter uma comunicação clara, paciente e respeitosa.
 *
 * Este módulo não é uma IA generativa e não possui emoções reais.
 * Suas respostas dependem das regras locais e dos recursos existentes.
 */
public final class JarvisKarenModule implements JarvisVisionModule {

    @Override
    public String getName() {
        return "KAREN";
    }

    @Override
    public boolean supports(
            JarvisReasoningEngine.Analysis analysis
    ) {
        if (analysis == null) {
            return false;
        }

        JarvisReasoningEngine.Intent intent =
                analysis.getIntent();

        JarvisReasoningEngine.Specialist specialist =
                analysis.getSpecialist();

        return specialist
                == JarvisReasoningEngine.Specialist.KAREN
                && (intent
                        == JarvisReasoningEngine.Intent.GUIDANCE
                    || intent
                        == JarvisReasoningEngine.Intent.SCHOOL_HELP);
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
                || core == null
                || !supports(analysis)) {
            return null;
        }

        /*
         * Primeiro, tenta responder usando a base de conhecimento
         * offline já existente, sem criar uma segunda base.
         */
        String knownAnswer =
                core.answerFromExistingKnowledgeBase(input);

        if (knownAnswer != null
                && !knownAnswer.trim().isEmpty()) {
            return "Vamos por partes!\n\n"
                    + knownAnswer.trim()
                    + "\n\nSe quiser, posso ajudar a entender "
                    + "a explicação passo a passo.";
        }

        /*
         * Se a base offline não reconhecer a pergunta, oferece
         * orientação inicial sem inventar uma resposta factual.
         */
        switch (analysis.getIntent()) {
            case SCHOOL_HELP:
                return "Vamos resolver isso juntos, passo a passo. "
                        + "Envie o enunciado completo da atividade "
                        + "ou diga qual parte ficou difícil. "
                        + "Vou ajudar a entender o raciocínio, "
                        + "não apenas apresentar uma resposta.";

            case GUIDANCE:
                return buildGuidanceResponse(
                        analysis.getNormalizedText()
                );

            default:
                return null;
        }
    }

    /**
     * Fornece uma orientação inicial baseada no tipo de pedido.
     * Não afirma que o problema foi resolvido sem informações
     * suficientes para isso.
     */
    private String buildGuidanceResponse(String normalizedText) {
        if (normalizedText == null
                || normalizedText.trim().isEmpty()) {
            return "Pode me explicar um pouco melhor o que você "
                    + "precisa? Assim consigo orientar você "
                    + "com mais precisão.";
        }

        if (containsAny(
                normalizedText,
                "passo a passo",
                "por onde comeco",
                "qual o primeiro passo",
                "como eu comeco")) {
            return "Vamos começar pelo primeiro passo:\n\n"
                    + "1. Identifique o que você quer alcançar.\n"
                    + "2. Separe o problema em partes menores.\n"
                    + "3. Resolva uma parte de cada vez.\n"
                    + "4. Confira o resultado antes de continuar.\n\n"
                    + "Conte qual é a tarefa ou o objetivo, "
                    + "e eu adapto os passos à sua situação.";
        }

        if (containsAny(
                normalizedText,
                "nao entendi",
                "nao consigo entender",
                "tenho dificuldade",
                "estou com dificuldade",
                "explica melhor",
                "explique melhor",
                "simplifica",
                "explicacao simples")) {
            return "Sem problema: podemos simplificar a explicação. "
                    + "Envie a parte que ficou confusa ou descreva "
                    + "o que você já entendeu. Vou dividir o assunto "
                    + "em etapas menores e usar um exemplo quando "
                    + "for útil.";
        }

        if (containsAny(
                normalizedText,
                "me ajuda a planejar",
                "me ajuda a organizar",
                "como fazer",
                "como eu faco")) {
            return "Vamos organizar isso de forma prática:\n\n"
                    + "1. Defina o resultado que você deseja.\n"
                    + "2. Liste o que já sabe e o que falta.\n"
                    + "3. Escolha uma primeira ação simples.\n"
                    + "4. Verifique o progresso antes de avançar.\n\n"
                    + "Se você me disser qual é o objetivo, "
                    + "posso ajudar a montar uma sequência "
                    + "mais específica.";
        }

        return "Claro! Vamos analisar isso com calma. "
                + "Explique o que você está tentando fazer, "
                + "o que aconteceu até agora e em qual parte "
                + "precisa de ajuda. Com esses detalhes, "
                + "posso orientar você de maneira mais precisa.";
    }

    private boolean containsAny(
            String text,
            String... terms
    ) {
        if (text == null || terms == null) {
            return false;
        }

        for (String term : terms) {
            if (term != null
                    && !term.isEmpty()
                    && text.contains(term)) {
                return true;
            }
        }

        return false;
    }
}
