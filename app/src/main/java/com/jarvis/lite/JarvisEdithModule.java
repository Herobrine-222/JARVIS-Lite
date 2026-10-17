
package com.jarvis.lite;

/**
 * EDITH: especialista em análise de pedidos relacionados
 * ao projeto JARVIS Lite.
 *
 * Utiliza o classificador existente e não executa ações
 * nem altera o dispositivo.
 */
public final class JarvisEdithModule
        implements JarvisVisionModule {

    @Override
    public String getName() {
        return "EDITH";
    }

    @Override
    public boolean supports(
            JarvisReasoningEngine.Analysis analysis
    ) {
        return analysis != null
                && analysis.getSpecialist()
                == JarvisReasoningEngine.Specialist.EDITH;
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

        int confidence = Math.round(
                analysis.getConfidence() * 100
        );

        return "EDITH identificou um pedido relacionado "
                + "ao projeto JARVIS Lite. "
                + "Motivo da classificação: "
                + analysis.getReason()
                + " Confiança estimada: "
                + confidence
                + "%.";
    }
}
