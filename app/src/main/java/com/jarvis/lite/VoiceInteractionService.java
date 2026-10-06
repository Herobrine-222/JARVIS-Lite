package com.jarvis.lite;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.service.voice.VoiceInteractionService;
import android.util.Log;

/**
 * Núcleo global de voz do JARVIS.
 *
 * Responsabilidades:
 * - ser o VoiceInteractionService oficial do JARVIS;
 * - detectar quando o Android tornou o JARVIS o assistente ativo;
 * - iniciar o motor de voz sem abrir a MainActivity;
 * - manter o serviço global leve;
 * - evitar inicializações duplicadas.
 *
 * Fluxo:
 *
 * Android
 *   ↓
 * VoiceInteractionService
 *   ↓
 * JarvisVoiceService
 *   ↓
 * Wake Word
 *   ↓
 * Sessão JARVIS
 */
public class VoiceInteractionService
        extends android.service.voice.VoiceInteractionService {

    private static final String TAG =
            "JARVIS_VOICE_CORE";

    private boolean sistemaPronto = false;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(
                TAG,
                "VoiceInteractionService criado."
        );
    }

    /**
     * Chamado pelo Android quando o serviço global
     * de voz está pronto para funcionar.
     */
    @Override
    public void onReady() {
        super.onReady();

        sistemaPronto = true;

        Log.d(
                TAG,
                "JARVIS tornou-se VoiceInteractionService ativo."
        );

        iniciarMotorDeVoz();
    }

    /**
     * Inicia o motor real de reconhecimento,
     * sem abrir qualquer Activity.
     */
    private void iniciarMotorDeVoz() {

        if (!sistemaPronto) {
            return;
        }

        try {

            ComponentName componente =
                    new ComponentName(
                            this,
                            VoiceInteractionService.class
                    );

            boolean ativo =
                    android.service.voice
                            .VoiceInteractionService
                            .isActiveService(
                                    this,
                                    componente
                            );

            if (!ativo) {

                Log.d(
                        TAG,
                        "JARVIS não é o assistente global ativo."
                );

                return;
            }

            Intent intent =
                    new Intent(
                            this,
                            JarvisVoiceService.class
                    );

            intent.setAction(
                    JarvisVoiceService.ACTION_RESUME
            );

            /*
             * O VoiceInteractionService é iniciado
             * pelo próprio sistema. Dessa forma,
             * podemos solicitar a retomada do motor
             * de voz sem abrir a interface do JARVIS.
             */
            if (Build.VERSION.SDK_INT >= 26) {

                startForegroundService(intent);

            } else {

                startService(intent);
            }

            Log.d(
                    TAG,
                    "Motor global de voz solicitado."
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Falha ao iniciar motor global de voz.",
                    e
            );
        }
    }

    /**
     * Chamado quando o Android encerra a participação
     * deste serviço como assistente global.
     */
    @Override
    public void onShutdown() {

        sistemaPronto = false;

        Log.d(
                TAG,
                "VoiceInteractionService entrando em shutdown."
        );

        try {

            Intent intent =
                    new Intent(
                            this,
                            JarvisVoiceService.class
                    );

            intent.setAction(
                    JarvisVoiceService.ACTION_PAUSE
            );

            startService(intent);

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Falha ao pausar motor de voz.",
                    e
            );
        }

        super.onShutdown();
    }

    /**
     * Mantemos o serviço global sem interface.
     *
     * O JARVIS não deve abrir MainActivity
     * simplesmente porque o usuário falou a palavra
     * de ativação.
     */
    @Override
    public void onPrepareToShowSession(
            android.os.Bundle args,
            int flags) {

        super.onPrepareToShowSession(
                args,
                flags
        );

        Log.d(
                TAG,
                "Android preparou uma sessão de voz."
        );
    }

    /**
     * Chamado quando o usuário tenta iniciar o
     * assistente pela interface de sistema.
     *
     * Nesta primeira etapa não abrimos a
     * MainActivity automaticamente.
     */
    @Override
    public void onLaunchVoiceAssistFromKeyguard() {

        Log.d(
                TAG,
                "Assistente solicitado pela tela de bloqueio."
        );

        /*
         * Não abrimos Activity aqui.
         *
         * O motor global continuará sendo responsável
         * pela interação de voz.
         */
    }

    @Override
    public void onDestroy() {

        sistemaPronto = false;

        Log.d(
                TAG,
                "VoiceInteractionService destruído."
        );

        super.onDestroy();
    }
}
