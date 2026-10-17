package com.jarvis.lite;

/**
 * U: especialista em conhecimento offline.
 *
 * Reutiliza a base de conhecimento existente do JARVIS Lite.
 * Não inventa respostas quando a base não encontra conteúdo.
 */
public final class JarvisUModule implements JarvisVisionModule {

    @Override
    public String getName() {
        return "U";
    }

    @Override
    public boolean supports(
            JarvisReasoningEngine.Analysis analysis
    ) {
        return analysis != null
                && analysis.getSpecialist()
                == JarvisReasoningEngine.Specialist.U;
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

        String answer =
                core.answerFromExistingKnowledgeBase(input);

        if (answer == null || answer.trim().isEmpty()) {
            return null;
        }

        return answer.trim();
    }
}
