
package com.jarvis.lite;

/**
 * Controla quais capacidades o JARVIS pode solicitar.
 *
 * Esta classe nao executa operacoes, nao concede permissoes
 * Android e nao substitui as verificacoes no ponto de uso.
 *
 * O resultado ALLOW significa apenas que esta verificacao
 * local foi satisfeita. A operacao real ainda precisa respeitar
 * as permissoes Android e as regras do modulo responsavel.
 */
public final class JarvisCapabilityGate {

    private JarvisCapabilityGate() {
        // Classe utilitaria, sem estado persistente.
    }

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
            this.decision = decision;
            this.reasonCode = reasonCode;
            this.message = message;
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
    }

    /**
     * Avalia uma capacidade sem executar a operacao.
     *
     * permissionGranted deve refletir uma verificacao real
     * da permissao Android, quando ela for necessaria.
     *
     * userConfirmed deve ser true somente quando houver
     * confirmacao explicita do usuario para aquela operacao.
     */
    public static Result check(
            Capability capability,
            boolean onlineModeEnabled,
            boolean permissionGranted,
            boolean userConfirmed) {

        if (capability == null) {
            return result(
                    null,
                    Decision.REVIEW_REQUIRED,
                    "INVALID_CAPABILITY",
                    "A capacidade solicitada nao foi identificada."
            );
        }

        switch (capability) {
            case READ_LOCAL_KNOWLEDGE:
            case READ_MEMORY:
            case SPEECH_OUTPUT:
                return result(
                        capability,
                        Decision.ALLOW,
                        "LOCAL_CAPABILITY_ALLOWED",
                        "A verificacao local permite esta capacidade."
                );

            case READ_DEVICE_INFO:
                if (!permissionGranted) {
                    return result(
                            capability,
                            Decision.REQUIRE_ANDROID_PERMISSION,
                            "DEVICE_PERMISSION_REQUIRED",
                            "Verifique as permissoes e restricoes Android aplicaveis."
                    );
                }

                return result(
                        capability,
                        Decision.ALLOW,
                        "DEVICE_INFO_ALLOWED",
                        "A verificacao local permite consultar as informacoes autorizadas."
                );

            case USE_MICROPHONE:
                if (!permissionGranted) {
                    return result(
                            capability,
                            Decision.REQUIRE_ANDROID_PERMISSION,
                            "MICROPHONE_PERMISSION_REQUIRED",
                            "A permissao real do microfone precisa estar concedida."
                    );
                }

                return result(
                        capability,
                        Decision.ALLOW,
                        "MICROPHONE_PERMISSION_PRESENT",
                        "A verificacao local permite solicitar o uso do microfone."
                );

            case NETWORK_ACCESS:
                if (!onlineModeEnabled) {
                    return result(
                            capability,
                            Decision.REQUIRE_ONLINE_MODE,
                            "ONLINE_MODE_DISABLED",
                            "Ative o modo online antes de solicitar acesso a rede."
                    );
                }

                return result(
                        capability,
                        Decision.ALLOW,
                        "ONLINE_MODE_ENABLED",
                        "O modo online esta habilitado; verifique tambem a autorizacao e a conexao."
                );

            case OPEN_ANDROID_SETTINGS:
            case WRITE_MEMORY:
                if (!userConfirmed) {
                    return result(
                            capability,
                            Decision.REQUIRE_USER_CONFIRMATION,
                            "USER_CONFIRMATION_REQUIRED",
                            "E necessaria uma confirmacao explicita do usuario."
                    );
                }

                return result(
                        capability,
                        Decision.ALLOW,
                        "USER_CONFIRMATION_PRESENT",
                        "A confirmacao foi informada; valide as regras especificas da operacao."
                );

            case MODIFY_DEVICE:
            case EXECUTE_DEVICE_COMMAND:
                return result(
                        capability,
                        Decision.DENY,
                        "DEVICE_ACTION_BLOCKED",
                        "Esta versao do controle nao autoriza alteracoes ou comandos diretos no dispositivo."
                );

            default:
                return result(
                        capability,
                        Decision.REVIEW_REQUIRED,
                        "UNCLASSIFIED_CAPABILITY",
                        "A capacidade precisa ser revisada antes de ser utilizada."
                );
        }
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
}
