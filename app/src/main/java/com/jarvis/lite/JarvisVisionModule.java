
package com.jarvis.lite;

/**
 * Contrato comum dos módulos especialistas do VISION.
 *
 * Cada módulo informa seu nome, identifica os pedidos
 * que consegue tratar e tenta produzir uma resposta.
 */
public interface JarvisVisionModule {

    String getName();

    boolean supports(
            JarvisReasoningEngine.Analysis analysis
    );

    /**
     * Tenta processar a mensagem.
     *
     * Retorna null ou texto vazio se não conseguir responder.
     */
    String process(
            String input,
            JarvisReasoningEngine.Analysis analysis,
            JarvisVisionCore core
    );
}
