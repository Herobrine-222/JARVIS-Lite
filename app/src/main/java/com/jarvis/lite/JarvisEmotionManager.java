
package com.jarvis.lite;

import java.util.Locale;

/**
 * Gerencia estados emocionais simulados do JARVIS Lite.
 *
 * Estes estados são apenas parâmetros de comportamento.
 * Não representam sentimentos reais e nunca devem substituir
 * o raciocínio, a verificação dos fatos ou as regras de segurança.
 */
public final class JarvisEmotionManager {

    public enum Emotion {
        CALM,
        SATISFIED,
        CURIOUS,
        SURPRISED,
        THINKING,
        FRUSTRATED,
        CONCERNED,
        ALERT,
        EMPATHETIC
    }

    public static final class EmotionState {
        private final Emotion emotion;
        private final int intensity;
        private final String reason;
        private final long updatedAt;

        private EmotionState(
                Emotion emotion,
                int intensity,
                String reason,
                long updatedAt) {
            this.emotion = emotion;
            this.intensity = intensity;
            this.reason = reason;
            this.updatedAt = updatedAt;
        }

        public Emotion getEmotion() {
            return emotion;
        }

        public int getIntensity() {
            return intensity;
        }

        public String getReason() {
            return reason;
        }

        public long getUpdatedAt() {
            return updatedAt;
        }

        @Override
        public String toString() {
            return emotion.name() + " (" + intensity + "/100)";
        }
    }

    private EmotionState currentState = new EmotionState(
            Emotion.CALM, 20, "Estado inicial", System.currentTimeMillis());

    private long lastUpdateTime = System.currentTimeMillis();

    /**
     * Retorna uma cópia imutável do estado atual.
     */
    public synchronized EmotionState getCurrentState() {
        return currentState;
    }

    /**
     * Define um estado simulado.
     *
     * @param emotion estado desejado
     * @param intensity intensidade de 0 a 100
     * @param reason motivo descritivo
     */
    public synchronized void setEmotion(
            Emotion emotion, int intensity, String reason) {

        if (emotion == null) {
            emotion = Emotion.CALM;
        }

        int safeIntensity = clamp(intensity, 0, 100);
        String safeReason = reason == null ? "" : reason.trim();

        currentState = new EmotionState(
                emotion,
                safeIntensity,
                safeReason,
                System.currentTimeMillis());

        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Atalho para definir estados comuns.
     */
    public synchronized void setCalm() {
        setEmotion(Emotion.CALM, 20, "Conversa normal");
    }

    public synchronized void setCurious(String reason) {
        setEmotion(Emotion.CURIOUS, 55, reason);
    }

    public synchronized void setThinking(String reason) {
        setEmotion(Emotion.THINKING, 60, reason);
    }

    public synchronized void setSurprised(String reason) {
        setEmotion(Emotion.SURPRISED, 65, reason);
    }

    public synchronized void setSatisfied(String reason) {
        setEmotion(Emotion.SATISFIED, 55, reason);
    }

    public synchronized void setFrustrated(String reason) {
        setEmotion(Emotion.FRUSTRATED, 45, reason);
    }

    public synchronized void setConcerned(String reason) {
        setEmotion(Emotion.CONCERNED, 70, reason);
    }

    public synchronized void setAlert(String reason) {
        setEmotion(Emotion.ALERT, 90, reason);
    }

    public synchronized void setEmpathetic(String reason) {
        setEmotion(Emotion.EMPATHETIC, 65, reason);
    }

    /**
     * Reduz gradualmente a intensidade, sem alterar o tipo
     * do estado nem permitir que um alerta desapareça sozinho.
     *
     * Chame este método quando o sistema decidir atualizar
     * o estado emocional. Não cria threads ou tarefas automáticas.
     */
    public synchronized void decay(int reduction) {
        int amount = Math.max(0, reduction);

        if (currentState.getEmotion() == Emotion.ALERT
                || currentState.getEmotion() == Emotion.CONCERNED) {
            return;
        }

        int newIntensity = Math.max(
                0, currentState.getIntensity() - amount);

        Emotion newEmotion = currentState.getEmotion();
        if (newIntensity == 0) {
            newEmotion = Emotion.CALM;
        }

        currentState = new EmotionState(
                newEmotion,
                newIntensity,
                currentState.getReason(),
                System.currentTimeMillis());

        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Retorna uma descrição neutra para outros módulos.
     * Não gera uma resposta ao usuário.
     */
    public synchronized String describeState() {
        return String.format(
                Locale.ROOT,
                "Estado simulado: %s; intensidade: %d/100; motivo: %s",
                currentState.getEmotion().name(),
                currentState.getIntensity(),
                currentState.getReason());
    }

    public synchronized long getLastUpdateTime() {
        return lastUpdateTime;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
