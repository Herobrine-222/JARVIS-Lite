package com.jarvis.lite;

import android.content.Intent;
import android.service.voice.VoiceInteractionSession;
import android.service.voice.VoiceInteractionSessionService;

/**
 * Serviço responsável por criar as sessões de voz
 * do JARVIS Lite.
 */
public class VoiceInteractionSessionService
        extends VoiceInteractionSessionService {

    @Override
    public VoiceInteractionSession onNewSession(
            android.os.Bundle args) {

        /*
         * Cada nova interação de voz recebe
         * uma nova sessão do JARVIS.
         */
        return new VoiceInteractionSession(
                this
        ) {

            @Override
            public void onShow(
                    android.os.Bundle args,
                    int showFlags) {

                /*
                 * A sessão foi iniciada.
                 *
                 * Não abrimos MainActivity aqui.
                 * O objetivo é manter a interação
                 * dentro do sistema de voz.
                 */
                super.onShow(
                        args,
                        showFlags
                );
            }
        };
    }
}
