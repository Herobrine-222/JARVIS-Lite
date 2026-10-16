package com.jarvis.lite;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

/**
 * Núcleo global de voz do JARVIS.
 *
 * Responsabilidades:
 * - ser o VoiceInteractionService oficial do JARVIS;
 * - detectar quando o Android tornou o JARVIS o assistente ativo;
 * - iniciar o motor de voz sem abrir a MainActivity;
 * - respeitar a preferência global do microfone;
 * - evitar manter a escuta ativa quando o microfone
 *   estiver desativado nas configurações do JARVIS.
 */
public class VoiceInteractionService
        extends android.service.voice.VoiceInteractionService {

    private static final String TAG =
            "JARVIS_VOICE_CORE";

    private static final String PREFS_NAME =
            "jarvis_config";

    private static final String KEY_MICROPHONE_ENABLED =
            "microfone_ativo";

    private boolean sistemaPronto = false;

    private SharedPreferences preferencias;

    /*
     * Observa alterações feitas pela MainActivity
     * na preferência global do microfone.
     */
    private final SharedPreferences.OnSharedPreferenceChangeListener
            preferenciasListener =
            (sharedPreferences, key) -> {

                if (!KEY_MICROPHONE_ENABLED.equals(key)) {
                    return;
                }

                if (!sistemaPronto) {
                    return;
                }

                boolean microfoneAtivado =
                        sharedPreferences.getBoolean(
                                KEY_MICROPHONE_ENABLED,
                                true
                        );

                if (microfoneAtivado) {

                    Log.d(
                            TAG,
                            "Microfone ativado nas configurações."
                    );

                    iniciarMotorDeVoz();

                } else {

                    Log.d(
                            TAG,
                            "Microfone desativado nas configurações."
                    );

                    pararMotorDeVoz(
                            "Microfone desativado nas configurações."
                    );
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();

        preferencias =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        preferencias.registerOnSharedPreferenceChangeListener(
                preferenciasListener
        );

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
                "VoiceInteractionService está pronto."
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

        if (preferencias == null) {

            Log.e(
                    TAG,
                    "Preferências do JARVIS não disponíveis."
            );

            return;
        }

        /*
         * A preferência usada aqui é a mesma
         * que a MainActivity salva em jarvis_config.
         */
        boolean microfoneAtivado =
                preferencias.getBoolean(
                        KEY_MICROPHONE_ENABLED,
                        true
                );

        if (!microfoneAtivado) {

            Log.d(
                    TAG,
                    "Motor não iniciado: microfone desativado."
            );

            pararMotorDeVoz(
                    "Microfone desativado."
            );

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

                pararMotorDeVoz(
                        "JARVIS não é o assistente ativo."
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
             * Solicita a retomada do serviço de voz
             * sem abrir a MainActivity.
             */
            if (Build.VERSION.SDK_INT >= 26) {

                startForegroundService(intent);

            } else {

                startService(intent);
            }

            Log.d(
                    TAG,
                    "Solicitação de início do motor enviada."
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
     * Encerra o serviço responsável pela escuta.
     *
     * O onDestroy() de JarvisVoiceService executa
     * a limpeza do reconhecimento e da reprodução
     * de voz. A memória persistente é armazenada
     * separadamente e não é apagada por este método.
     */
    private void pararMotorDeVoz(String motivo) {

        try {

            Intent intent =
                    new Intent(
                            this,
                            JarvisVoiceService.class
                    );

            boolean solicitado =
                    stopService(intent);

            Log.d(
                    TAG,
                    motivo
                            + " Serviço encerrado: "
                            + solicitado
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Falha ao encerrar motor de voz.",
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

        pararMotorDeVoz(
                "Assistente global desligado."
        );

        super.onShutdown();
    }

    /**
     * Chamado quando o Android prepara uma sessão
     * de interação por voz.
     *
     * O JARVIS não abre a MainActivity aqui.
     */
    @Override
    public void onPrepareToShowSession(
            Bundle args,
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
     * Chamado quando o usuário solicita o assistente
     * pela interface da tela de bloqueio.
     *
     * Não abre a MainActivity automaticamente.
     */
    @Override
    public void onLaunchVoiceAssistFromKeyguard() {

        Log.d(
                TAG,
                "Assistente solicitado pela tela de bloqueio."
        );
    }

    @Override
    public void onDestroy() {

        sistemaPronto = false;

        if (preferencias != null) {

            preferencias.unregisterOnSharedPreferenceChangeListener(
                    preferenciasListener
            );
        }

        /*
         * Se este serviço for destruído sem que o
         * Android tenha chamado onShutdown(), não
         * deixamos o motor global de escuta órfão.
         */
        pararMotorDeVoz(
                "VoiceInteractionService destruído."
        );

        Log.d(
                TAG,
                "VoiceInteractionService destruído."
        );

        super.onDestroy();
    }
}
