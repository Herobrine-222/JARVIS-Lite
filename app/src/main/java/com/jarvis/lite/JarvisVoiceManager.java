package com.jarvis.lite;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.io.IOException;
import java.util.Locale;

/**
 * Gerenciador de voz do JARVIS.
 *
 * Responsabilidades:
 * - Falar respostas usando o TTS do Android.
 * - Controlar início e parada da fala.
 * - Permitir selecionar voz padrão ou voz personalizada.
 * - Reproduzir um arquivo de áudio personalizado quando necessário.
 * - Deixar a arquitetura preparada para futura integração
 *   com clonagem de voz.
 *
 * Este módulo não acessa a Internet.
 */
public class JarvisVoiceManager {

    public enum VoiceMode {
        ANDROID_TTS,
        CUSTOM_AUDIO,
        CLONED_VOICE
    }

    private final Context context;

    private TextToSpeech textToSpeech;
    private MediaPlayer mediaPlayer;

    private VoiceMode voiceMode = VoiceMode.ANDROID_TTS;

    private boolean initialized = false;
    private boolean speaking = false;

    private String customAudioPath = null;

    private float speechRate = 1.0f;
    private float pitch = 1.0f;

    private OnVoiceListener listener;

    /**
     * Construtor principal.
     */
    public JarvisVoiceManager(Context context) {
        this.context = context.getApplicationContext();

        inicializarTts();
    }

    /**
     * Inicializa o mecanismo TTS do Android.
     */
    private void inicializarTts() {

        textToSpeech = new TextToSpeech(
                context,
                status -> {

                    if (status == TextToSpeech.SUCCESS) {

                        int result = textToSpeech.setLanguage(
                                new Locale("pt", "BR")
                        );

                        if (result == TextToSpeech.LANG_MISSING_DATA
                                || result == TextToSpeech.LANG_NOT_SUPPORTED) {

                            textToSpeech.setLanguage(Locale.getDefault());
                        }

                        textToSpeech.setSpeechRate(speechRate);
                        textToSpeech.setPitch(pitch);

                        textToSpeech.setOnUtteranceProgressListener(
                                new UtteranceProgressListener() {

                                    @Override
                                    public void onStart(String utteranceId) {
                                        speaking = true;

                                        if (listener != null) {
                                            listener.onStart();
                                        }
                                    }

                                    @Override
                                    public void onDone(String utteranceId) {
                                        speaking = false;

                                        if (listener != null) {
                                            listener.onDone();
                                        }
                                    }

                                    @Override
                                    public void onError(String utteranceId) {
                                        speaking = false;

                                        if (listener != null) {
                                            listener.onError();
                                        }
                                    }
                                }
                        );

                        initialized = true;

                        if (listener != null) {
                            listener.onInitialized();
                        }

                    } else {

                        initialized = false;

                        if (listener != null) {
                            listener.onInitializationError();
                        }
                    }
                }
        );
    }

    /**
     * Fala um texto usando o modo de voz atualmente selecionado.
     */
    public void speak(String text) {

        if (text == null) {
            return;
        }

        String mensagem = text.trim();

        if (mensagem.isEmpty()) {
            return;
        }

        switch (voiceMode) {

            case ANDROID_TTS:
                falarComAndroidTts(mensagem);
                break;

            case CUSTOM_AUDIO:
                reproduzirAudioPersonalizado();
                break;

            case CLONED_VOICE:
                /*
                 * A clonagem de voz será conectada posteriormente.
                 *
                 * Por enquanto utilizamos o TTS do Android como
                 * fallback para impedir que o JARVIS fique sem voz.
                 */
                falarComAndroidTts(mensagem);
                break;
        }
    }

    /**
     * Fala utilizando o TTS nativo do Android.
     */
    private void falarComAndroidTts(String texto) {

        if (!initialized || textToSpeech == null) {

            if (listener != null) {
                listener.onInitializationError();
            }

            return;
        }

        pararAudioPersonalizado();

        String utteranceId =
                "jarvis_" + System.currentTimeMillis();

        int resultado = textToSpeech.speak(
                texto,
                TextToSpeech.QUEUE_FLUSH,
                null,
                utteranceId
        );

        if (resultado == TextToSpeech.ERROR) {

            speaking = false;

            if (listener != null) {
                listener.onError();
            }
        }
    }

    /**
     * Reproduz o arquivo de áudio personalizado configurado.
     *
     * Observação:
     * Um único MP3 não consegue gerar qualquer frase dinamicamente.
     * Ele serve para reproduzir aquele áudio específico.
     *
     * A futura clonagem de voz será responsável por gerar novas
     * frases usando a voz personalizada.
     */
    private void reproduzirAudioPersonalizado() {

        if (customAudioPath == null
                || customAudioPath.trim().isEmpty()) {

            if (listener != null) {
                listener.onCustomAudioMissing();
            }

            return;
        }

        pararTts();
        pararAudioPersonalizado();

        try {

            mediaPlayer = new MediaPlayer();

            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(
                                    AudioAttributes.CONTENT_TYPE_SPEECH
                            )
                            .build()
            );

            mediaPlayer.setDataSource(customAudioPath);

            mediaPlayer.setOnPreparedListener(mp -> {

                speaking = true;

                if (listener != null) {
                    listener.onStart();
                }

                mp.start();
            });

            mediaPlayer.setOnCompletionListener(mp -> {

                speaking = false;

                if (listener != null) {
                    listener.onDone();
                }

                liberarMediaPlayer();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {

                speaking = false;

                if (listener != null) {
                    listener.onError();
                }

                liberarMediaPlayer();

                return true;
            });

            mediaPlayer.prepareAsync();

        } catch (IOException | IllegalArgumentException e) {

            speaking = false;

            liberarMediaPlayer();

            if (listener != null) {
                listener.onError();
            }
        }
    }

    /**
     * Define o modo de voz.
     */
    public void setVoiceMode(VoiceMode mode) {

        if (mode == null) {
            return;
        }

        parar();

        voiceMode = mode;
    }

    /**
     * Retorna o modo atual.
     */
    public VoiceMode getVoiceMode() {
        return voiceMode;
    }

    /**
     * Define o caminho do áudio personalizado.
     */
    public void setCustomAudioPath(String path) {

        if (path == null || path.trim().isEmpty()) {
            customAudioPath = null;
            return;
        }

        customAudioPath = path;
    }

    /**
     * Retorna o caminho do áudio personalizado.
     */
    public String getCustomAudioPath() {
        return customAudioPath;
    }

    /**
     * Verifica se existe áudio personalizado configurado.
     */
    public boolean hasCustomAudio() {

        return customAudioPath != null
                && !customAudioPath.trim().isEmpty();
    }

    /**
     * Define velocidade da fala.
     */
    public void setSpeechRate(float rate) {

        if (rate <= 0) {
            rate = 1.0f;
        }

        speechRate = rate;

        if (textToSpeech != null) {
            textToSpeech.setSpeechRate(speechRate);
        }
    }

    /**
     * Retorna velocidade atual.
     */
    public float getSpeechRate() {
        return speechRate;
    }

    /**
     * Define o tom da voz.
     */
    public void setPitch(float value) {

        if (value <= 0) {
            value = 1.0f;
        }

        pitch = value;

        if (textToSpeech != null) {
            textToSpeech.setPitch(pitch);
        }
    }

    /**
     * Retorna o tom atual.
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * Verifica se o TTS está pronto.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Verifica se o JARVIS está falando.
     */
    public boolean isSpeaking() {

        if (textToSpeech != null
                && voiceMode == VoiceMode.ANDROID_TTS) {

            return textToSpeech.isSpeaking();
        }

        return speaking;
    }

    /**
     * Para qualquer fala em andamento.
     */
    public void parar() {

        pararTts();
        pararAudioPersonalizado();

        speaking = false;
    }

    /**
     * Compatibilidade com serviços que utilizam o nome stop().
     *
     * O novo JarvisVoiceService utiliza este método
     * para interromper a fala antes de destruir o gerenciador.
     */
    public void stop() {
        parar();
    }

    /**
     * Para somente o TTS.
     */
    private void pararTts() {

        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    /**
     * Para somente o áudio personalizado.
     */
    private void pararAudioPersonalizado() {

        if (mediaPlayer != null) {

            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException ignored) {
            }

            liberarMediaPlayer();
        }
    }

    /**
     * Libera o MediaPlayer.
     */
    private void liberarMediaPlayer() {

        if (mediaPlayer != null) {

            try {
                mediaPlayer.reset();
            } catch (Exception ignored) {
            }

            try {
                mediaPlayer.release();
            } catch (Exception ignored) {
            }

            mediaPlayer = null;
        }
    }

    /**
     * Define listener de eventos da voz.
     */
    public void setOnVoiceListener(OnVoiceListener listener) {
        this.listener = listener;
    }

    /**
     * Libera os recursos do gerenciador.
     */
    public void destroy() {

        parar();

        if (textToSpeech != null) {

            try {
                textToSpeech.shutdown();
            } catch (Exception ignored) {
            }

            textToSpeech = null;
        }

        initialized = false;
        listener = null;
    }

    /**
     * Listener para eventos da voz.
     */
    public interface OnVoiceListener {

        void onInitialized();

        void onInitializationError();

        void onStart();

        void onDone();

        void onError();

        void onCustomAudioMissing();
    }
}
