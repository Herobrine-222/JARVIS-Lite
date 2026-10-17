
package com.jarvis.lite;

import java.util.Locale;

/**
 * DUM-E: especialista em consultas informativas
 * sobre o aparelho.
 *
 * Utiliza o JarvisDeviceTools já mantido pelo
 * JarvisContextManager.
 *
 * Não altera configurações, não executa ações
 * destrutivas e não realiza uma verificação
 * antivírus completa.
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
        return analysis != null
                && analysis.getSpecialist()
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
            return "O DUM-E precisa do contexto "
                    + "do aplicativo para consultar "
                    + "as informações do aparelho.";
        }

        /*
         * Reutiliza a ferramenta já criada pelo
         * JarvisContextManager.
         *
         * Não cria uma segunda instância de
         * JarvisDeviceTools nem tenta obter o Context
         * por um método inexistente.
         */
        JarvisDeviceTools tools =
                contextManager.getDeviceTools();

        if (tools == null) {
            return "As informações locais do aparelho "
                    + "estão indisponíveis no momento.";
        }

        String normalized =
                analysis.getNormalizedText();

        if (normalized == null
                || normalized.trim().isEmpty()) {
            normalized = input
                    .trim()
                    .toLowerCase(Locale.ROOT);
        }

        /*
         * CONSULTAS SOBRE BATERIA
         */
        if (normalized.contains("bateria")
                || normalized.contains("carregando")
                || normalized.contains("temperatura do celular")) {

            return tools.resumoBateria();
        }

        /*
         * CONSULTAS SOBRE ARMAZENAMENTO
         */
        if (normalized.contains("armazenamento")
                || normalized.contains("espaco livre")
                || normalized.contains("memoria interna")
                || normalized.contains("quanto de espaco")) {

            return tools.resumoArmazenamento();
        }

        /*
         * PEDIDOS DE VERIFICAÇÃO DO APARELHO
         *
         * Esta resposta explica o limite do módulo.
         * Não afirma que uma verificação antivírus
         * completa foi realizada.
         */
        if (normalized.contains("verificacao do aparelho")
                || normalized.contains("verificar o aparelho")
                || normalized.contains("verificar seguranca")
                || normalized.contains("verificacao basica")) {

            return "Posso consultar informações básicas "
                    + "do aparelho, mas isso não equivale "
                    + "a uma verificação antivírus completa.";
        }

        /*
         * A mensagem não corresponde a nenhuma
         * consulta implementada neste módulo.
         *
         * null permite que o núcleo continue o fluxo
         * e tente encontrar outro módulo responsável.
         */
        return null;
    }
}
