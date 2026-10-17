
package com.jarvis.lite;

/**
 * FRIDAY: especialista inicial em contexto e memória.
 *
 * Consulta informações já armazenadas no JARVIS Lite.
 * Não inventa lembranças quando não encontra resultados.
 */
public final class JarvisFridayModule
        implements JarvisVisionModule {

    @Override
    public String getName() {
        return "FRIDAY";
    }

    @Override
    public boolean supports(
            JarvisReasoningEngine.Analysis analysis
    ) {
        return analysis != null;
    }

    @Override
    public String process(
            String input,
            JarvisReasoningEngine.Analysis analysis,
            JarvisVisionCore core
    ) {
        if (input == null || core == null) {
            return null;
        }

        String normalized = input.trim().toLowerCase();

        if (normalized.isEmpty()) {
            return null;
        }

        boolean asksAboutMemory =
                normalized.contains("o que você lembra")
                || normalized.contains("o que voce lembra")
                || normalized.contains("o que você sabe sobre mim")
                || normalized.contains("o que voce sabe sobre mim")
                || normalized.contains("minhas lembranças")
                || normalized.contains("minhas lembrancas")
                || normalized.contains("lembra de mim");

        if (!asksAboutMemory) {
            return null;
        }

        String context = core.getRelevantMemory(input);

        if (context == null || context.trim().isEmpty()) {
            return "Ainda não encontrei lembranças relevantes "
                    + "na memória do JARVIS Lite para essa pergunta.";
        }

        return "Encontrei estas informações na memória:\n"
                + context.trim();
    }
}
