
package com.jarvis.lite;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

/**
 * Núcleo global de voz do JARVIS Lite.
 *
 * Responsabilidades:
 * - funcionar como VoiceInteractionService oficial;
 * - verificar se o JARVIS é o assistente ativo;
 * - respeitar a permissão do microfone;
 * - respeitar a configuração microfone_ativo;
 * - iniciar e encerrar o motor de voz;
 * - não abrir a MainActivity automaticamente.
 *
 * Fluxo:
 *
 * Android
 *   -> VoiceInteractionService
 *   -> JarvisVoiceService
 *   -> Wake Word
 *   -> Sessão JARVIS
 */
public class VoiceInteractionService
        extends android.service.voice.VoiceInteractionService {

    private static final String TAG = "JARVIS_VOICE_CORE";

    private static final String PREF_CONFIG = "jarvis_config";
    private static final String PREF_MICROFONE_ATIVO =
            "microfone_ativo";

    private boolean sistemaPronto = false;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "VoiceInteractionService criado.");
    }

    /**
     * Chamado pelo Android quando o serviço de voz
     * está pronto para receber interações.
     */
    @Override
    public void onReady() {
        super.onReady();

        sistemaPronto = true;

        Log.d(TAG, "Sistema de voz preparado.");

        iniciarMotorDeVoz();
    }

    /**
     * Verifica se o usuário concedeu a permissão
     * necessária para utilizar o microfone.
     *
     * Este método não solicita a permissão:
     * isso deve ser feito pela interface do aplicativo.
     */
    private boolean temPermissaoMicrofone() {
        return checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Lê a configuração salva pelo aplicativo.
     * Por padrão, mantém o comportamento anterior:
     * microfone ativo quando não existe preferência salva.
     */
    private boolean microfoneAtivadoPeloUsuario() {
        return getSharedPreferences(
                PREF_CONFIG,
                MODE_PRIVATE
        ).getBoolean(
                PREF_MICROFONE_ATIVO,
                true
        );
    }

    /**
     * Verifica as condições antes de iniciar
     * o motor global de voz.
     */
    private void iniciarMotorDeVoz() {

        if (!sistemaPronto) {
            Log.d(TAG, "Sistema ainda não está pronto.");
            return;
        }

        try {
            ComponentName componente =
                    new ComponentName(
                            this,
                            VoiceInteractionService.class
                    );

            boolean assistenteAtivo =
                    android.service.voice
                            .VoiceInteractionService
                            .isActiveService(
                                    this,
                                    componente
                            );

            if (!assistenteAtivo) {
                Log.d(
                        TAG,
                        "JARVIS não é o assistente ativo."
                );

                encerrarMotorDeVoz();
                return;
            }

            if (!microfoneAtivadoPeloUsuario()) {
                Log.d(
                        TAG,
                        "Microfone desativado nas configurações."
                );

                encerrarMotorDeVoz();
                return;
            }

            if (!temPermissaoMicrofone()) {
                Log.w(
                        TAG,
                        "Permissão do microfone não concedida."
                );

                encerrarMotorDeVoz();
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
             * O VoiceInteractionService ativo solicita
             * a inicialização do motor de voz.
             *
             * O JarvisVoiceService é responsável por
             * promover-se a serviço em primeiro plano.
             */
            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O) {

                startForegroundService(intent);

            } else {

                startService(intent);
            }

            Log.d(
                    TAG,
                    "Solicitação de inicialização enviada."
            );

        } catch (SecurityException e) {

            Log.e(
                    TAG,
                    "Permissão ou segurança impediu a inicialização.",
                    e
            );

            encerrarMotorDeVoz();

        } catch (IllegalStateException e) {

            Log.e(
                    TAG,
                    "O Android não permitiu iniciar o motor.",
                    e
            );

            encerrarMotorDeVoz();

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Falha ao iniciar o motor global de voz.",
                    e
            );

            encerrarMotorDeVoz();
        }
    }

    /**
     * Encerra o serviço de voz para evitar que
     * ele continue tentando escutar quando o assistente
     * deixa de estar ativo ou quando o microfone é desativado.
     */
    private void encerrarMotorDeVoz() {
        try {
            Intent intent =
                    new Intent(
                            this,
                            JarvisVoiceService.class
                    );

            boolean solicitado = stopService(intent);

            Log.d(
                    TAG,
                    solicitado
                            ? "Encerramento do motor solicitado."
                            : "Motor já estava encerrado."
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Falha ao encerrar o motor de voz.",
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
                "Assistente global entrando em shutdown."
        );

        encerrarMotorDeVoz();

        super.onShutdown();
    }

    /**
     * Não abre a MainActivity apenas porque o Android
     * preparou uma sessão de voz.
     */
    @Override
    public void onPrepareToShowSession(
            android.os.Bundle args,
            int flags) {

        super.onPrepareToShowSession(args, flags);

        Log.d(
                TAG,
                "Android preparou uma sessão de voz."
        );
    }

    /**
     * Mantém o comportamento anterior na tela de bloqueio:
     * não abre automaticamente a interface do JARVIS.
     */
    @Override
    public void onLaunchVoiceAssistFromKeyguard() {

        Log.d(
                TAG,
                "Assistente solicitado na tela de bloqueio."
        );
    }

    @Override
    public void onDestroy() {

        sistemaPronto = false;

        Log.d(
                TAG,
                "VoiceInteractionService destruído."
        );

        encerrarMotorDeVoz();

        super.onDestroy();
    }
}
