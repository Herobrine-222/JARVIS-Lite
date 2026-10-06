package com.jarvis.lite;

import android.os.Bundle;
import android.util.Log;

/**
 * Sessão global de interação por voz do JARVIS.
 *
 * Esta classe é criada pelo VoiceInteractionSessionService
 * quando o Android inicia uma interação de voz com o JARVIS.
 *
 * IMPORTANTE:
 * - Não abre a MainActivity.
 * - Não cria uma nova interface visual do JARVIS.
 * - Não substitui o JarvisVoiceService.
 * - Serve como ponte entre o sistema Android e o núcleo JARVIS.
 */
public class VoiceInteractionSession
        extends android.service.voice.VoiceInteractionSession {

    private static final String TAG =
            "JARVIS_VOICE_SESSION";

    private boolean sessaoAtiva = false;

    public VoiceInteractionSession(
            android.content.Context context) {

        super(context);

        Log.d(
                TAG,
                "VoiceInteractionSession criada."
        );
    }

    /**
     * Chamado pelo Android quando a sessão é mostrada.
     */
    @Override
    public void onShow(
            Bundle args,
            int showFlags) {

        super.onShow(
                args,
                showFlags
        );

        sessaoAtiva = true;

        Log.d(
                TAG,
                "Sessão de voz do JARVIS aberta."
        );

        if (args != null) {

            String sessionId =
                    args.getString(
                            android.service.voice
                                    .VoiceInteractionSession
                                    .KEY_SHOW_SESSION_ID
                    );

            if (sessionId != null) {

                Log.d(
                        TAG,
                        "ID da sessão: "
                                + sessionId
                );
            }
        }
    }

    /**
     * Chamado quando o Android esconde a sessão.
     */
    @Override
    public void onHide() {

        sessaoAtiva = false;

        Log.d(
                TAG,
                "Sessão de voz do JARVIS escondida."
        );

        super.onHide();
    }

    /**
     * Chamado quando a sessão é destruída.
     */
    @Override
    public void onDestroy() {

        sessaoAtiva = false;

        Log.d(
                TAG,
                "Sessão de voz do JARVIS destruída."
        );

        super.onDestroy();
    }

    /**
     * Permite consultar internamente se a sessão
     * está atualmente ativa.
     */
    public boolean isSessaoAtiva() {

        return sessaoAtiva;
    }
}
