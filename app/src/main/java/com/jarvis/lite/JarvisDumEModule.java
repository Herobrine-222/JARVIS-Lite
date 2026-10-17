
package com.jarvis.lite;

/**
 * DUM-E: especialista em consultas informativas
 * sobre o aparelho.
 *
 * Utiliza JarvisDeviceTools, já existente no projeto.
 * Não altera configurações nem executa ações destrutivas.
 */
public final class JarvisDumEModule
        implements JarvisVisionModule {

    @Override
    public String getName() {
        return "DUM-E";
    }

    @Override
    public boolean supports(
            JarvisReasoningEngine.Analysis analysis
    ) {
        if (analysis == null
                || analysis.getSpecialist() == null) {
            return false;
        }

        return analysis.getSpecialist()
                == JarvisReasoningEngine.Specialist.DUM_E;
    }

    @Override
    public String process(
            String input,
            JarvisReasoningEngine.Analysis analysis,
            JarvisVisionCore core
    ) {
        if (input == null
                || input.trim().isEmpty()
                || core == null
                || !supports(analysis)) {
            return null;
        }

        JarvisContextManager contextManager =
                core.getContextManager();

        if (contextManager == null) {
            return "O DUM-E precisa receber o contexto "
                    + "do aplicativo para consultar "
                    + "as informações do aparelho.";
        }

        String normalized =
                analysis.getNormalizedText();

        if (normalized == null) {
            normalized = input.toLowerCase();
        }

        JarvisDeviceTools tools;

        try {
            tools = new JarvisDeviceTools(
                    getApplicationContext(contextManager)
            );
        } catch (RuntimeException exception) {
            return "Não foi possível acessar as "
                    + "informações locais do aparelho.";
        }

        if (normalized.contains("bateria")
                || normalized.contains("carregando")
                || normalized.contains("temperatura do celular")) {
            return tools.resumoBateria();
        }

        if (normalized.contains("armazenamento")
                || normalized.contains("espaco livre")
                || normalized.contains("memoria interna")) {
            return tools.resumoArmazenamento();
        }

        if (normalized.contains("verificacao do aparelho")
                || normalized.contains("verificar o aparelho")
                || normalized.contains("verificar seguranca")
                || normalized.contains("verificacao basica")) {
            return "Posso consultar informações básicas "
                    + "do aparelho, mas isso não equivale "
                    + "a uma verificação antivírus completa.";
        }

        return null;
    }

    private android.content.Context getApplicationContext(
            JarvisContextManager contextManager
    ) {
        throw new UnsupportedOperationException(
                "A integração com o Context do aplicativo "
                        + "precisa ser definida."
        );
    }
}
