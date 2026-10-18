
package com.jarvis.lite;

import java.util.Locale;

/**
 * JarvisDecisionEngine
 *
 * Modulo local de avaliacao e selecao de decisoes.
 *
 * Responsabilidades:
 * - Avaliar a intencao identificada pelo sistema.
 * - Considerar contexto, confianca e restricoes de seguranca.
 * - Selecionar o proximo passo recomendado.
 * - Explicar a justificativa da decisao.
 *
 * Este modulo NAO:
 * - Executa comandos no Android.
 * - Altera configuracoes do aparelho.
 * - Acessa a Internet.
 * - Grava dados na memoria automaticamente.
 * - Substitui JarvisReasoningEngine ou os modulos de seguranca.
 *
 * Importante: a decisao e uma recomendacao para o sistema.
 * A execucao de qualquer acao deve ser tratada separadamente.
 */
public final class JarvisDecisionEngine {

    public static final int MIN_CONFIDENCE = 0;
    public static final int MAX_CONFIDENCE = 100;

    private static final int DEFAULT_CONFIDENCE = 50;
    private static final int HIGH_CONFIDENCE = 80;

    public enum Action {
        RESPOND,
        USE_SPECIALIST,
        ASK_CLARIFICATION,
        REVIEW_REQUIRED,
        REFUSE,
        END_SESSION,
        NO_ACTION
    }

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }

    public enum Status {
        ACCEPTED,
        NEEDS_MORE_INFORMATION,
        BLOCKED_BY_SAFETY,
        LOW_CONFIDENCE,
        INVALID_INPUT
    }

    /**
     * Dados fornecidos ao modulo para avaliar uma decisao.
     * Nenhuma acao e executada ao criar este objeto.
     */
    public static final class DecisionInput {

        private final String userInput;
        private final String intent;
        private final String specialist;
        private final int confidence;
        private final boolean contextAvailable;
        private final boolean safetyApproved;
        private final boolean clarificationNeeded;
        private final boolean sessionEnding;

        public DecisionInput(
                String userInput,
                String intent,
                String specialist,
                int confidence,
                boolean contextAvailable,
                boolean safetyApproved,
                boolean clarificationNeeded,
                boolean sessionEnding) {

            this.userInput = userInput;
            this.intent = intent;
            this.specialist = specialist;
            this.confidence = clampConfidence(confidence);
            this.contextAvailable = contextAvailable;
            this.safetyApproved = safetyApproved;
            this.clarificationNeeded = clarificationNeeded;
            this.sessionEnding = sessionEnding;
        }

        public String getUserInput() {
            return userInput;
        }

        public String getIntent() {
            return intent;
        }

        public String getSpecialist() {
            return specialist;
        }

        public int getConfidence() {
            return confidence;
        }

        public boolean isContextAvailable() {
            return contextAvailable;
        }

        public boolean isSafetyApproved() {
            return safetyApproved;
        }

        public boolean isClarificationNeeded() {
            return clarificationNeeded;
        }

        public boolean isSessionEnding() {
            return sessionEnding;
        }
    }

    /**
     * Resultado imutavel da avaliacao.
     */
    public static final class Decision {

        private final Action action;
        private final Priority priority;
        private final Status status;
        private final String reason;
        private final String intent;
        private final String specialist;
        private final int confidence;
        private final boolean requiresUserConfirmation;

        private Decision(
                Action action,
                Priority priority,
                Status status,
                String reason,
                String intent,
                String specialist,
                int confidence,
                boolean requiresUserConfirmation) {

            this.action = action;
            this.priority = priority;
            this.status = status;
            this.reason = reason;
            this.intent = intent;
            this.specialist = specialist;
            this.confidence = confidence;
            this.requiresUserConfirmation = requiresUserConfirmation;
        }

        public Action getAction() {
            return action;
        }

        public Priority getPriority() {
            return priority;
        }

        public Status getStatus() {
            return status;
        }

        public String getReason() {
            return reason;
        }

        public String getIntent() {
            return intent;
        }

        public String getSpecialist() {
            return specialist;
        }

        public int getConfidence() {
            return confidence;
        }

        public boolean requiresUserConfirmation() {
            return requiresUserConfirmation;
        }

        public boolean isAccepted() {
            return status == Status.ACCEPTED;
        }

        @Override
        public String toString() {
            return "Decision{" +
                    "action=" + action +
                    ", priority=" + priority +
                    ", status=" + status +
                    ", confidence=" + confidence +
                    ", reason='" + reason + '\'' +
                    '}';
        }
    }

    /**
     * Avalia uma entrada e recomenda o proximo passo.
     */
    public Decision decide(DecisionInput input) {

        if (input == null) {
            return createDecision(
                    Action.NO_ACTION,
                    Priority.NORMAL,
                    Status.INVALID_INPUT,
                    "Nenhuma entrada foi fornecida.",
                    null,
                    null,
                    0,
                    false
            );
        }

        String message = normalize(input.getUserInput());
        String intent = normalize(input.getIntent());
        String specialist = normalize(input.getSpecialist());
        int confidence = input.getConfidence();

        if (message.isEmpty() || intent.isEmpty()) {
            return createDecision(
                    Action.ASK_CLARIFICATION,
                    Priority.NORMAL,
                    Status.INVALID_INPUT,
                    "A mensagem ou a intencao esta ausente.",
                    intent,
                    specialist,
                    confidence,
                    false
            );
        }

        /*
         * A seguranca tem prioridade sobre a escolha do especialista,
         * a confianca e o estilo da personalidade.
         *
         * Uma avaliacao de seguranca ausente ou negativa nao deve
         * ser interpretada como autorizacao.
         */
        if (!input.isSafetyApproved()) {
            return createDecision(
                    Action.REVIEW_REQUIRED,
                    Priority.HIGH,
                    Status.BLOCKED_BY_SAFETY,
                    "A avaliacao de seguranca nao autorizou a continuacao.",
                    intent,
                    specialist,
                    confidence,
                    false
            );
        }

        /*
         * Encerrar sessao e uma decisao de fluxo.
         * Este modulo nao encerra servicos nem altera o aparelho.
         */
        if (input.isSessionEnding()
                || contains(intent, "END_SESSION", "ENCERRAR_SESSAO")) {

            return createDecision(
                    Action.END_SESSION,
                    Priority.NORMAL,
                    Status.ACCEPTED,
                    "A entrada foi classificada como pedido para encerrar a sessao.",
                    intent,
                    specialist,
                    confidence,
                    false
            );
        }

        /*
         * Uma intencao pouco confiavel nao deve gerar uma escolha
         * especializada sem esclarecimento.
         */
        if (confidence < HIGH_CONFIDENCE
                && !input.isContextAvailable()) {

            return createDecision(
                    Action.ASK_CLARIFICATION,
                    Priority.NORMAL,
                    Status.LOW_CONFIDENCE,
                    "A confianca e baixa e nao ha contexto suficiente.",
                    intent,
                    specialist,
                    confidence,
                    false
            );
        }

        if (input.isClarificationNeeded()) {
            return createDecision(
                    Action.ASK_CLARIFICATION,
                    Priority.NORMAL,
                    Status.NEEDS_MORE_INFORMATION,
                    "Sao necessarias informacoes adicionais.",
                    intent,
                    specialist,
                    confidence,
                    false
            );
        }

        if (confidence < HIGH_CONFIDENCE) {
            return createDecision(
                    Action.REVIEW_REQUIRED,
                    Priority.NORMAL,
                    Status.LOW_CONFIDENCE,
                    "A decisao precisa de uma verificacao adicional.",
                    intent,
                    specialist,
                    confidence,
                    false
            );
        }

        /*
         * Escolhe encaminhamento para especialista quando existe
         * um nome de especialista fornecido pelo raciocinio.
         * O nome nao executa nem chama esse especialista.
         */
        if (!specialist.isEmpty()
                && !equalsAny(specialist, "CORE", "NONE", "NENHUM")) {

            return createDecision(
                    Action.USE_SPECIALIST,
                    Priority.NORMAL,
                    Status.ACCEPTED,
                    "A intencao tem confianca suficiente para encaminhamento.",
                    intent,
                    specialist,
                    confidence,
                    false
            );
        }

        return createDecision(
                Action.RESPOND,
                Priority.NORMAL,
                Status.ACCEPTED,
                "Ha informacoes suficientes para preparar uma resposta.",
                intent,
                specialist,
                confidence,
                false
        );
    }

    /**
     * Atalho para casos em que os dados ja foram classificados
     * pelo JarvisReasoningEngine.
     *
     * Os valores de seguranca e contexto devem vir de verificacoes
     * reais, nao ser presumidos como verdadeiros.
     */
    public Decision decide(
            String userInput,
            String intent,
            String specialist,
            int confidence,
            boolean contextAvailable,
            boolean safetyApproved) {

        return decide(new DecisionInput(
                userInput,
                intent,
                specialist,
                confidence,
                contextAvailable,
                safetyApproved,
                false,
                false
        ));
    }

    /**
     * Informa se uma decisao requer autorizacao humana antes
     * de uma futura operacao sensivel.
     *
     * A resposta nao concede permissao nem executa a operacao.
     */
    public boolean requiresConfirmationForSensitiveAction(
            String actionDescription) {

        String action = normalize(actionDescription);

        if (action.isEmpty()) {
            return true;
        }

        return contains(action,
                "DELETE",
                "EXCLUIR",
                "APAGAR",
                "UNINSTALL",
                "DESINSTALAR",
                "SEND",
                "ENVIAR",
                "SHARE",
                "COMPARTILHAR",
                "CHANGE_SETTINGS",
                "ALTERAR_CONFIGURACOES",
                "PURCHASE",
                "COMPRAR");
    }

    /**
     * Explica a decisao em uma frase curta.
     */
    public String explain(Decision decision) {

        if (decision == null) {
            return "Nao ha decisao disponivel para explicar.";
        }

        return decision.getReason();
    }

    /**
     * Retorna uma descricao simples do estado do modulo.
     */
    public String getStatusSummary() {
        return "JarvisDecisionEngine pronto; avaliacao local; "
                + "sem execucao automatica de comandos.";
    }

    private Decision createDecision(
            Action action,
            Priority priority,
            Status status,
            String reason,
            String intent,
            String specialist,
            int confidence,
            boolean confirmation) {

        return new Decision(
                action,
                priority,
                status,
                reason,
                intent,
                specialist,
                clampConfidence(confidence),
                confirmation
        );
    }

    private static int clampConfidence(int confidence) {
        return Math.max(
                MIN_CONFIDENCE,
                Math.min(MAX_CONFIDENCE, confidence)
        );
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean contains(String value, String... options) {
        for (String option : options) {
            if (value.contains(option)) {
                return true;
            }
        }
        return false;
    }

    private static boolean equalsAny(String value, String... options) {
        for (String option : options) {
            if (value.equals(option)) {
                return true;
            }
        }
        return false;
    }
}
