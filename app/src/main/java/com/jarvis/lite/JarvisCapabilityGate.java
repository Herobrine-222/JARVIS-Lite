
package com.jarvis.lite;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * JARVIS Lite — controle local de capacidades.
 *
 * Responsabilidades:
 * - Avaliar se uma capacidade pode prosseguir.
 * - Aplicar bloqueio por padrao a operacoes sensiveis.
 * - Exigir modo online para acesso a rede.
 * - Exigir permissao Android real para o microfone.
 * - Exigir confirmacao explicita para operacoes protegidas.
 * - Fornecer resultados estruturados e motivos auditaveis.
 *
 * LIMITES IMPORTANTES:
 * Esta classe nao executa operacoes, nao concede permissoes,
 * nao verifica diretamente o estado do Android e nao substitui
 * validacoes no ponto em que cada recurso e utilizado.
 *
 * ALLOW significa apenas que as verificacoes deste controle
 * foram satisfeitas. O chamador ainda deve validar as condicoes
 * reais imediatamente antes de executar a operacao.
 *
 * Compativel com Java sem recursos recentes da linguagem.
 */
public final class JarvisCapabilityGate {

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MAX_REASON_CODE_LENGTH = 100;

    private JarvisCapabilityGate() {
        // Classe utilitaria: nao deve ser instanciada.
    }

    /**
     * Capacidades conhecidas pelo controle.
     *
     * Novas capacidades devem ser adicionadas explicitamente
     * e avaliadas antes de serem liberadas.
     */
    public enum Capability {
        READ_LOCAL_KNOWLEDGE,
        READ_MEMORY,
        READ_DEVICE_INFO,
        USE_MICROPHONE,
        SPEECH_OUTPUT,
        NETWORK_ACCESS,
        OPEN_ANDROID_SETTINGS,
        WRITE_MEMORY,
        MODIFY_DEVICE,
        EXECUTE_DEVICE_COMMAND
    }

    public enum Decision {
        ALLOW,
        DENY,
        REQUIRE_ANDROID_PERMISSION,
        REQUIRE_ONLINE_MODE,
        REQUIRE_USER_CONFIRMATION,
        REVIEW_REQUIRED
    }

    /**
     * Contexto minimo usado na avaliacao.
     *
     * Os valores sao informados pelo chamador. Nao sao prova
     * independente de que o Android concedeu uma permissao,
     * de que existe conexao ou de que o usuario confirmou uma
     * operacao especifica.
     */
    public static final class RequestContext {

        private final boolean onlineModeEnabled;
        private final boolean androidPermissionGranted;
        private final boolean userConfirmed;
        private final boolean operationIsReadOnly;

        public RequestContext(
                boolean onlineModeEnabled,
                boolean androidPermissionGranted,
                boolean userConfirmed,
                boolean operationIsReadOnly) {

            this.onlineModeEnabled = onlineModeEnabled;
            this.androidPermissionGranted = androidPermissionGranted;
            this.userConfirmed = userConfirmed;
            this.operationIsReadOnly = operationIsReadOnly;
        }

        public boolean isOnlineModeEnabled() {
            return onlineModeEnabled;
        }

        public boolean isAndroidPermissionGranted() {
            return androidPermissionGranted;
        }

        public boolean isUserConfirmed() {
            return userConfirmed;
        }

        public boolean isOperationReadOnly() {
            return operationIsReadOnly;
        }

        /**
         * Contexto conservador para chamadas sem informacoes.
         */
        public static RequestContext restrictive() {
            return new RequestContext(false, false, false, false);
        }
    }

    /**
     * Resultado imutavel da avaliacao.
     */
    public static final class Result {

        private final Capability capability;
        private final Decision decision;
        private final String reasonCode;
        private final String message;

        private Result(
                Capability capability,
                Decision decision,
                String reasonCode,
                String message) {

            this.capability = capability;
            this.decision = decision == null
                    ? Decision.REVIEW_REQUIRED
                    : decision;

            this.reasonCode = safeText(
                    reasonCode,
                    MAX_REASON_CODE_LENGTH,
                    "UNSPECIFIED"
            );

            this.message = safeText(
                    message,
                    MAX_MESSAGE_LENGTH,
                    "A avaliacao nao forneceu detalhes."
            );
        }

        public Capability getCapability() {
            return capability;
        }

        public Decision getDecision() {
            return decision;
        }

        public String getReasonCode() {
            return reasonCode;
        }

        public String getMessage() {
            return message;
        }

        public boolean isAllowed() {
            return decision == Decision.ALLOW;
        }

        public boolean requiresReview() {
            return decision == Decision.REVIEW_REQUIRED;
        }

        public boolean requiresUserConfirmation() {
            return decision == Decision.REQUIRE_USER_CONFIRMATION;
        }

        public boolean requiresAndroidPermission() {
            return decision == Decision.REQUIRE_ANDROID_PERMISSION;
        }

        public boolean requiresOnlineMode() {
            return decision == Decision.REQUIRE_ONLINE_MODE;
        }

        @Override
        public String toString() {
            return "Result{"
                    + "capability=" + capability
                    + ", decision=" + decision
                    + ", reasonCode='" + reasonCode + '\''
                    + '}';
        }
    }

    /**
     * API de compatibilidade com a versao anterior.
     *
     * Mantem a assinatura publica existente.
     */
    public static Result check(
            Capability capability,
            boolean onlineModeEnabled,
            boolean permissionGranted,
            boolean userConfirmed) {

        RequestContext context = new RequestContext(
                onlineModeEnabled,
                permissionGranted,
                userConfirmed,
                false
        );

        return evaluate(capability, context);
    }

    /**
     * Avalia uma capacidade usando um contexto explicito.
     *
     * Este metodo nao executa a operacao.
     */
    public static Result evaluate(
            Capability capability,
            RequestContext context) {

        if (capability == null) {
            return result(
                    null,
                    Decision.REVIEW_REQUIRED,
                    "INVALID_CAPABILITY",
                    "A capacidade solicitada nao foi identificada."
            );
        }

        if (context == null) {
            return result(
                    capability,
                    Decision.REVIEW_REQUIRED,
                    "MISSING_REQUEST_CONTEXT",
                    "O contexto da solicitacao esta ausente."
            );
        }

        switch (capability) {

            case READ_LOCAL_KNOWLEDGE:
                return allow(
                        capability,
                        "LOCAL_KNOWLEDGE_ALLOWED",
                        "A consulta ao conhecimento local pode prosseguir."
                );

            case READ_MEMORY:
                return allow(
                        capability,
                        "MEMORY_READ_ALLOWED",
                        "A leitura da memoria pode prosseguir, respeitando as regras de privacidade."
                );

            case READ_DEVICE_INFO:
                /*
                 * Informacoes como data, bateria e algumas medidas
                 * do dispositivo podem ser acessiveis sem permissao
                 * perigosa. O chamador deve verificar cada dado e
                 * cada permissao realmente necessaria.
                 */
                return allow(
                        capability,
                        "DEVICE_INFO_CHECK_REQUIRED_AT_USE",
                        "A consulta pode prosseguir apenas para informacoes acessiveis e autorizadas pelo Android."
                );

            case USE_MICROPHONE:
                if (!context.isAndroidPermissionGranted()) {
                    return result(
                            capability,
                            Decision.REQUIRE_ANDROID_PERMISSION,
                            "MICROPHONE_PERMISSION_REQUIRED",
                            "Verifique e solicite a permissao real do microfone pelo Android."
                    );
                }

                return allow(
                        capability,
                        "MICROPHONE_PERMISSION_REPORTED",
                        "A permissao foi informada como concedida; confirme novamente no ponto de uso."
                );

            case SPEECH_OUTPUT:
                return allow(
                        capability,
                        "SPEECH_OUTPUT_ALLOWED",
                        "A fala sintetizada pode ser solicitada se houver um mecanismo de voz disponivel."
                );

            case NETWORK_ACCESS:
                if (!context.isOnlineModeEnabled()) {
                    return result(
                            capability,
                            Decision.REQUIRE_ONLINE_MODE,
                            "ONLINE_MODE_DISABLED",
                            "O acesso a rede exige que o modo online esteja habilitado pelo usuario."
                    );
                }

                return allow(
                        capability,
                        "ONLINE_MODE_REPORTED_ENABLED",
                        "O modo online foi informado como habilitado; confirme a conexao, o provedor e as regras de rede."
                );

            case OPEN_ANDROID_SETTINGS:
                if (!context.isOperationReadOnly()) {
                    return result(
                            capability,
                            Decision.REVIEW_REQUIRED,
                            "SETTINGS_OPERATION_NOT_READ_ONLY",
                            "A solicitacao pode alterar configuracoes; revise a operacao antes de prosseguir."
                    );
                }

                if (!context.isUserConfirmed()) {
                    return result(
                            capability,
                            Decision.REQUIRE_USER_CONFIRMATION,
                            "SETTINGS_CONFIRMATION_REQUIRED",
                            "Solicite confirmacao explicita antes de abrir as configuracoes para esta operacao."
                    );
                }

                return allow(
                        capability,
                        "SETTINGS_CONFIRMATION_REPORTED",
                        "A confirmacao foi informada; valide a tela e a acao exatas antes de continuar."
                );

            case WRITE_MEMORY:
                if (!context.isUserConfirmed()) {
                    return result(
                            capability,
                            Decision.REQUIRE_USER_CONFIRMATION,
                            "MEMORY_WRITE_CONFIRMATION_REQUIRED",
                            "A alteracao da memoria exige confirmacao explicita para esta operacao."
                    );
                }

                return allow(
                        capability,
                        "MEMORY_WRITE_CONFIRMATION_REPORTED",
                        "A confirmacao foi informada; valide o conteudo e as regras de memoria antes de gravar."
                );

            case MODIFY_DEVICE:
                return result(
                        capability,
                        Decision.DENY,
                        "DEVICE_MODIFICATION_BLOCKED",
                        "Alteracoes diretas no dispositivo permanecem bloqueadas por esta politica."
                );

            case EXECUTE_DEVICE_COMMAND:
                return result(
                        capability,
                        Decision.DENY,
                        "DEVICE_COMMAND_BLOCKED",
                        "A execucao de comandos diretos no dispositivo permanece bloqueada."
                );

            default:
                /*
                 * Falhar de forma segura: uma nova capacidade nao
                 * deve ser liberada automaticamente.
                 */
                return result(
                        capability,
                        Decision.REVIEW_REQUIRED,
                        "UNCLASSIFIED_CAPABILITY",
                        "Esta capacidade ainda nao possui uma regra de autorizacao revisada."
                );
        }
    }

    /**
     * Lista imutavel das capacidades bloqueadas por padrao.
     *
     * Serve para consulta e diagnostico; nao substitui evaluate().
     */
    public static Set<Capability> getDefaultDeniedCapabilities() {
        EnumSet<Capability> denied = EnumSet.of(
                Capability.MODIFY_DEVICE,
                Capability.EXECUTE_DEVICE_COMMAND
        );

        return Collections.unmodifiableSet(denied);
    }

    /**
     * Retorna true somente quando o resultado existe e permite
     * prosseguir segundo este controle local.
     */
    public static boolean isAllowed(Result result) {
        return result != null && result.isAllowed();
    }

    private static Result allow(
            Capability capability,
            String reasonCode,
            String message) {

        return result(
                capability,
                Decision.ALLOW,
                reasonCode,
                message
        );
    }

    private static Result result(
            Capability capability,
            Decision decision,
            String reasonCode,
            String message) {

        return new Result(
                capability,
                decision,
                reasonCode,
                message
        );
    }

    private static String safeText(
            String value,
            int maxLength,
            String fallback) {

        if (value == null) {
            return fallback;
        }

        String cleaned = value.trim();

        if (cleaned.isEmpty()) {
            return fallback;
        }

        if (cleaned.length() > maxLength) {
            return cleaned.substring(0, maxLength);
        }

        return cleaned;
    }
}
