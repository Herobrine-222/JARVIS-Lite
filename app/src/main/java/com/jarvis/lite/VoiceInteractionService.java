package com.jarvis.lite;

import android.os.Bundle;

/**
 * Serviço principal de interação por voz do JARVIS Lite.
 *
 * Este serviço é mantido pelo Android quando o JARVIS
 * estiver selecionado como assistente de voz do sistema.
 *
 * A sessão de voz será criada pelo
 * VoiceInteractionSessionService.
 */
public class VoiceInteractionService
        extends android.service.voice.VoiceInteractionService {

    @Override
    public void onReady() {
        super.onReady();

        /*
         * O Android chegou ao ponto em que o serviço
         * está pronto para receber interações.
         *
         * Não iniciamos o microfone manualmente aqui.
         * A captura de voz deve acontecer dentro do
         * fluxo oficial de interação por voz do Android.
         */
    }

    @Override
    public void onShutdown() {
        /*
         * Limpeza quando o Android encerra o serviço.
         */
        super.onShutdown();
    }

    @Override
    public void onPrepareToShowSession(
            Bundle args,
            int flags) {

        /*
         * Chamado pelo sistema antes de uma sessão
         * de interação ser apresentada.
         *
         * Não abrimos MainActivity aqui.
         */
        super.onPrepareToShowSession(
                args,
                flags
        );
    }
 }
