
package com.jarvis.lite;

/**
 * EDITH: especialista em análise e encaminhamento.
 *
 * Reutiliza o mecanismo de raciocínio existente.
 * Não executa ações no dispositivo.
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
        return analysis != null;
    }

    @Override
    public String process(
            String input,
            JarvisReasoningEngine.Analysis analysis,
            JarvisVisionCore core
    ) {
        if (input == null || input.trim().isEmpty()
                || analysis == null || core == null) {
            return null;
        }

        JarvisReasoningEngine.Specialist specialist =
                core.recommendSpecialist(input);

        if (specialist == null) {
            return null;
        }

        return "EDITH analisou seu pedido. "
                + "O mecanismo de raciocínio recomenda "
                + "o especialista: "
                + specialist.toString()
                + ".";
    }
}
