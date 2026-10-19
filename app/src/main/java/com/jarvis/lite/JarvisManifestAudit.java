
package com.jarvis.lite;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * JARVIS Lite - Manifest Audit
 *
 * Auditoria local e somente de leitura das informações do próprio aplicativo.
 *
 * Este módulo:
 * - Não altera permissões.
 * - Não inicia nem interrompe serviços.
 * - Não modifica arquivos ou configurações do aparelho.
 * - Não acessa a internet.
 *
 * IMPORTANTE:
 * A auditoria em tempo de execução não substitui a revisão do
 * AndroidManifest.xml, do Gradle e do Manifest mesclado da compilação.
 */
public final class JarvisManifestAudit {

    private static final String TAG = "JarvisManifestAudit";

    private static final String PERMISSION_RECORD_AUDIO =
            Manifest.permission.RECORD_AUDIO;

    private static final String PERMISSION_INTERNET =
            Manifest.permission.INTERNET;

    private static final String PERMISSION_FOREGROUND_SERVICE =
            "android.permission.FOREGROUND_SERVICE";

    private static final String PERMISSION_FOREGROUND_MICROPHONE =
            "android.permission.FOREGROUND_SERVICE_MICROPHONE";

    private static final String COMPONENT_MAIN =
            "com.jarvis.lite.MainActivity";

    private static final String COMPONENT_VOICE_SERVICE =
            "com.jarvis.lite.JarvisVoiceService";

    private static final String COMPONENT_VOICE_INTERACTION =
            "com.jarvis.lite.VoiceInteractionService";

    private static final String COMPONENT_SESSION_SERVICE =
            "com.jarvis.lite.VoiceInteractionSessionService";

    private static final String BIND_VOICE_INTERACTION =
            "android.permission.BIND_VOICE_INTERACTION";

    private JarvisManifestAudit() {
        // Classe utilitária: não deve ser instanciada.
    }

    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }

    public static final class Finding {
        private final Severity severity;
        private final String code;
        private final String message;
        private final String recommendation;

        private Finding(
                Severity severity,
                String code,
                String message,
                String recommendation
        ) {
            this.severity = severity;
            this.code = code;
            this.message = message;
            this.recommendation = recommendation;
        }

        public Severity getSeverity() {
            return severity;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public String getRecommendation() {
            return recommendation;
        }

        @Override
        public String toString() {
            return "[" + severity + "] " + code + ": " + message
                    + (recommendation.isEmpty()
                    ? ""
                    : " Recomendação: " + recommendation);
        }
    }

    public static final class AuditReport {
        private final String packageName;
        private final long auditTimeMillis;
        private final List<Finding> findings;
        private final boolean auditCompleted;

        private AuditReport(
                String packageName,
                long auditTimeMillis,
                List<Finding> findings,
                boolean auditCompleted
        ) {
            this.packageName = packageName;
            this.auditTimeMillis = auditTimeMillis;
            this.findings = Collections.unmodifiableList(
                    new ArrayList<>(findings)
            );
            this.auditCompleted = auditCompleted;
        }

        public String getPackageName() {
            return packageName;
        }

        public long getAuditTimeMillis() {
            return auditTimeMillis;
        }

        public List<Finding> getFindings() {
            return findings;
        }

        public boolean isAuditCompleted() {
            return auditCompleted;
        }

        public int count(Severity severity) {
            int total = 0;

            for (Finding finding : findings) {
                if (finding.getSeverity() == severity) {
                    total++;
                }
            }

            return total;
        }

        public boolean hasErrors() {
            return count(Severity.ERROR) > 0;
        }

        public boolean hasWarnings() {
            return count(Severity.WARNING) > 0;
        }

        public String toText() {
            StringBuilder result = new StringBuilder();

            result.append("JARVIS Lite - Relatório de auditoria\n");
            result.append("Pacote: ").append(packageName).append('\n');
            result.append("Auditoria concluída: ")
                    .append(auditCompleted ? "sim" : "não")
                    .append('\n');
            result.append("Erros: ").append(count(Severity.ERROR)).append('\n');
            result.append("Avisos: ").append(count(Severity.WARNING)).append('\n');
            result.append("Informações: ")
                    .append(count(Severity.INFO)).append('\n');
            result.append('\n');

            for (Finding finding : findings) {
                result.append(finding.toString()).append('\n');
            }

            if (findings.isEmpty()) {
                result.append(
                        "Nenhuma inconsistência detectada pelas verificações "
                                + "executadas. Isso não garante segurança total."
                );
            }

            return result.toString();
        }
    }

    /**
     * Audita somente as informações disponíveis para o próprio aplicativo.
     *
     * @param context Contexto do aplicativo.
     * @return Relatório local; não executa correções automáticas.
     */
    public static AuditReport audit(Context context) {
        List<Finding> findings = new ArrayList<>();

        if (context == null) {
            findings.add(new Finding(
                    Severity.ERROR,
                    "CONTEXT_NULL",
                    "Não foi fornecido um contexto válido.",
                    "Chame audit() com o contexto do aplicativo."
            ));

            return new AuditReport(
                    "desconhecido",
                    System.currentTimeMillis(),
                    findings,
                    false
            );
        }

        Context appContext = context.getApplicationContext();

        if (appContext == null) {
            appContext = context;
        }

        String packageName = appContext.getPackageName();
        PackageManager packageManager = appContext.getPackageManager();

        if (packageManager == null) {
            findings.add(new Finding(
                    Severity.ERROR,
                    "PACKAGE_MANAGER_UNAVAILABLE",
                    "Não foi possível obter o PackageManager.",
                    "Tente executar a auditoria novamente."
            ));

            return new AuditReport(
                    packageName,
                    System.currentTimeMillis(),
                    findings,
                    false
            );
        }

        try {
            PackageInfo packageInfo = getOwnPackageInfo(
                    packageManager,
                    packageName
            );

            if (packageInfo == null) {
                findings.add(new Finding(
                        Severity.ERROR,
                        "PACKAGE_INFO_UNAVAILABLE",
                        "Não foi possível ler os dados do próprio aplicativo.",
                        "Verifique a instalação e tente novamente."
                ));

                return new AuditReport(
                        packageName,
                        System.currentTimeMillis(),
                        findings,
                        false
                );
            }

            inspectRequestedPermissions(
                    appContext,
                    packageInfo,
                    findings
            );

            inspectActivities(packageInfo, findings);
            inspectServices(packageInfo, findings);
            inspectVoiceServicePermissions(packageInfo, findings);

            findings.add(new Finding(
                    Severity.INFO,
                    "AUDIT_SCOPE",
                    "A verificação examinou informações disponíveis em "
                            + "tempo de execução.",
                    "Revise também o Manifest de origem, o Gradle e o "
                            + "Manifest mesclado da compilação."
            ));

            return new AuditReport(
                    packageName,
                    System.currentTimeMillis(),
                    findings,
                    true
            );

        } catch (SecurityException exception) {
            findings.add(new Finding(
                    Severity.ERROR,
                    "SECURITY_EXCEPTION",
                    "O Android bloqueou o acesso a uma informação necessária "
                            + "para a auditoria.",
                    "Verifique as limitações do sistema e os dados acessíveis "
                            + "ao aplicativo."
            ));

        } catch (RuntimeException exception) {
            findings.add(new Finding(
                    Severity.ERROR,
                    "AUDIT_RUNTIME_FAILURE",
                    "A auditoria não conseguiu concluir todas as verificações.",
                    "Consulte os logs de desenvolvimento e investigue a causa."
            ));
        }

        return new AuditReport(
                packageName,
                System.currentTimeMillis(),
                findings,
                false
        );
    }

    private static PackageInfo getOwnPackageInfo(
            PackageManager packageManager,
            String packageName
    ) {
        int flags = PackageManager.GET_ACTIVITIES
                | PackageManager.GET_SERVICES;

        // Compatível com versões antigas e novas do Android.
        return packageManager.getPackageInfo(packageName, flags);
    }

    private static void inspectRequestedPermissions(
            Context context,
            PackageInfo packageInfo,
            List<Finding> findings
    ) {
        List<String> requested = new ArrayList<>();

        if (packageInfo.requestedPermissions != null) {
            Collections.addAll(
                    requested,
                    packageInfo.requestedPermissions
            );
        }

        checkDeclaredPermission(
                requested,
                PERMISSION_RECORD_AUDIO,
                true,
                "O microfone é necessário para os recursos de voz.",
                "Confirme se o uso do microfone faz parte dos recursos desejados.",
                findings
        );

        checkDeclaredPermission(
                requested,
                PERMISSION_FOREGROUND_SERVICE,
                false,
                "O aplicativo declara serviços em primeiro plano.",
                "Confirme a necessidade e a compatibilidade com o SDK de destino.",
                findings
        );

        checkDeclaredPermission(
                requested,
                PERMISSION_FOREGROUND_MICROPHONE,
                false,
                "O aplicativo declara um serviço de microfone em primeiro plano.",
                "Confira a versão do Android, o SDK de destino e o uso real do serviço.",
                findings
        );

        if (requested.contains(PERMISSION_INTERNET)) {
            findings.add(new Finding(
                    Severity.INFO,
                    "INTERNET_DECLARED",
                    "A permissão INTERNET está declarada.",
                    "Isso permite que o aplicativo solicite acesso à rede, "
                            + "mas não comprova que exista comunicação em segundo plano."
            ));
        } else {
            findings.add(new Finding(
                    Severity.WARNING,
                    "INTERNET_NOT_DECLARED",
                    "A permissão INTERNET não foi encontrada entre as "
                            + "permissões declaradas.",
                    "Se o modo online precisar de rede, confira o Manifest "
                            + "e a configuração final de compilação. Não adicione "
                            + "a permissão se o projeto não precisar dela."
            ));
        }

        if (Build.VERSION.SDK_INT >= 23) {
            boolean granted = context.checkSelfPermission(
                    PERMISSION_RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED;

            if (granted) {
                findings.add(new Finding(
                        Severity.INFO,
                        "MICROPHONE_PERMISSION_GRANTED",
                        "O Android informa que a permissão de microfone "
                                + "está concedida.",
                        "O uso real ainda depende das restrições do sistema "
                                + "e do estado do microfone."
                ));
            } else {
                findings.add(new Finding(
                        Severity.WARNING,
                        "MICROPHONE_PERMISSION_NOT_GRANTED",
                        "A permissão de microfone não está concedida neste momento.",
                        "Se o usuário quiser usar a voz, solicite a permissão "
                                + "pelo fluxo normal do Android."
                ));
            }
        }
    }

    private static void checkDeclaredPermission(
            List<String> requested,
            String permission,
            boolean requiredByCurrentDesign,
            String presentMessage,
            String missingRecommendation,
            List<Finding> findings
    ) {
        if (requested.contains(permission)) {
            findings.add(new Finding(
                    Severity.INFO,
                    "PERMISSION_DECLARED_" + safeCode(permission),
                    presentMessage,
                    ""
            ));
        } else {
            findings.add(new Finding(
                    requiredByCurrentDesign ? Severity.WARNING : Severity.INFO,
                    "PERMISSION_NOT_DECLARED_" + safeCode(permission),
                    "A permissão " + permission + " não foi encontrada.",
                    missingRecommendation
            ));
        }
    }

    private static String safeCode(String value) {
        return value.toUpperCase(Locale.ROOT)
                .replace('.', '_')
                .replace('-', '_');
    }

    private static void inspectActivities(
            PackageInfo packageInfo,
            List<Finding> findings
    ) {
        boolean mainFound = false;

        if (packageInfo.activities != null) {
            for (ActivityInfo activity : packageInfo.activities) {
                if (activity == null || activity.name == null) {
                    continue;
                }

                if (COMPONENT_MAIN.equals(activity.name)) {
                    mainFound = true;

                    if (!activity.exported) {
                        findings.add(new Finding(
                                Severity.ERROR,
                                "MAIN_ACTIVITY_NOT_EXPORTED",
                                "A MainActivity não está exportada.",
                                "Confira o intent-filter de inicialização e "
                                        + "a configuração android:exported."
                        ));
                    } else {
                        findings.add(new Finding(
                                Severity.INFO,
                                "MAIN_ACTIVITY_FOUND",
                                "A MainActivity está registrada e exportada.",
                                "Confirme que o intent-filter MAIN/LAUNCHER "
                                        + "está correto no Manifest de origem."
                        ));
                    }
                }
            }
        }

        if (!mainFound) {
            findings.add(new Finding(
                    Severity.ERROR,
                    "MAIN_ACTIVITY_NOT_FOUND",
                    "A MainActivity esperada não foi encontrada nos dados "
                            + "do pacote.",
                    "Confira o nome completo da classe e o Manifest mesclado."
            ));
        }
    }

    private static void inspectServices(
            PackageInfo packageInfo,
            List<Finding> findings
    ) {
        boolean voiceServiceFound = false;
        boolean voiceInteractionFound = false;
        boolean sessionServiceFound = false;

        if (packageInfo.services != null) {
            for (ServiceInfo service : packageInfo.services) {
                if (service == null || service.name == null) {
                    continue;
                }

                if (COMPONENT_VOICE_SERVICE.equals(service.name)) {
                    voiceServiceFound = true;

                    if (service.exported) {
                        findings.add(new Finding(
                                Severity.WARNING,
                                "VOICE_SERVICE_EXPORTED",
                                "JarvisVoiceService está exportado.",
                                "Se nenhum outro aplicativo precisar iniciá-lo, "
                                        + "considere android:exported=\"false\"."
                        ));
                    } else {
                        findings.add(new Finding(
                                Severity.INFO,
                                "VOICE_SERVICE_FOUND",
                                "JarvisVoiceService está registrado como "
                                        + "serviço não exportado.",
                                ""
                        ));
                    }

                    if (Build.VERSION.SDK_INT >= 29) {
                        int microphoneType =
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;

                        if ((service.foregroundServiceType
                                & microphoneType) == 0) {
                            findings.add(new Finding(
                                    Severity.WARNING,
                                    "VOICE_FOREGROUND_TYPE",
                                    "JarvisVoiceService não informa o tipo "
                                            + "microphone no campo consultado.",
                                    "Confira se o serviço realmente precisa "
                                            + "de microfone em primeiro plano."
                            ));
                        }
                    }
                }

                if (COMPONENT_VOICE_INTERACTION.equals(service.name)) {
                    voiceInteractionFound = true;
                    inspectVoiceInteractionService(service, findings);
                }

                if (COMPONENT_SESSION_SERVICE.equals(service.name)) {
                    sessionServiceFound = true;
                    inspectSessionService(service, findings);
                }
            }
        }

        if (!voiceServiceFound) {
            findings.add(new Finding(
                    Severity.WARNING,
                    "VOICE_SERVICE_NOT_FOUND",
                    "JarvisVoiceService não foi encontrado.",
                    "Se o serviço ainda fizer parte da arquitetura, confira "
                            + "o nome da classe e o Manifest."
            ));
        }

        if (!voiceInteractionFound) {
            findings.add(new Finding(
                    Severity.WARNING,
                    "VOICE_INTERACTION_NOT_FOUND",
                    "VoiceInteractionService não foi encontrado.",
                    "Confirme se o serviço é necessário para o modo de "
                            + "interação por voz planejado."
            ));
        }

        if (!sessionServiceFound) {
            findings.add(new Finding(
                    Severity.WARNING,
                    "VOICE_SESSION_NOT_FOUND",
                    "VoiceInteractionSessionService não foi encontrado.",
                    "Confira o serviço de sessões e o XML de metadados "
                            + "da interação por voz."
            ));
        }
    }

    private static void inspectVoiceInteractionService(
            ServiceInfo service,
            List<Finding> findings
    ) {
        if (!service.exported) {
            findings.add(new Finding(
                    Severity.ERROR,
                    "VOICE_INTERACTION_NOT_EXPORTED",
                    "O serviço de interação por voz não está exportado.",
                    "O Android pode precisar acessá-lo por meio da permissão "
                            + "de vinculação específica."
            ));
        }

        if (!BIND_VOICE_INTERACTION.equals(service.permission)) {
            findings.add(new Finding(
                    Severity.ERROR,
                    "VOICE_INTERACTION_PERMISSION",
                    "O serviço de interação por voz não declara a permissão "
                            + "de vinculação esperada.",
                    "Confira android.permission.BIND_VOICE_INTERACTION "
                            + "no Manifest."
            ));
        } else {
            findings.add(new Finding(
                    Severity.INFO,
                    "VOICE_INTERACTION_PERMISSION_OK",
                    "O serviço declara a permissão de vinculação esperada.",
                    "Isso não garante que o serviço esteja selecionado ou "
                            + "ativo como assistente do sistema."
            ));
        }

        if (service.metaData == null
                || !service.metaData.containsKey(
                        "android.voice_interaction_service"
                )) {
            findings.add(new Finding(
                    Severity.WARNING,
                    "VOICE_INTERACTION_METADATA",
                    "Não foi encontrada a chave de metadados esperada "
                            + "nos dados consultados.",
                    "Confira a declaração android.voice_interaction_service "
                            + "e o recurso XML referenciado."
            ));
        }
    }

    private static void inspectSessionService(
            ServiceInfo service,
            List<Finding> findings
    ) {
        if (!service.exported) {
            findings.add(new Finding(
                    Severity.WARNING,
                    "VOICE_SESSION_NOT_EXPORTED",
                    "O serviço de sessões não está exportado.",
                    "Confira se essa configuração corresponde ao modelo "
                            + "de interação por voz usado pelo aplicativo."
            ));
        }

        if (!BIND_VOICE_INTERACTION.equals(service.permission)) {
            findings.add(new Finding(
                    Severity.WARNING,
                    "VOICE_SESSION_PERMISSION",
                    "O serviço de sessões não declara a permissão "
                            + "de vinculação esperada.",
                    "Confira a configuração exigida pelo serviço de "
                            + "interação por voz do Android."
            ));
        } else {
            findings.add(new Finding(
                    Severity.INFO,
                    "VOICE_SESSION_PERMISSION_OK",
                    "O serviço de sessões declara a permissão de vinculação.",
                    ""
            ));
        }
    }

    private static void inspectVoiceServicePermissions(
            PackageInfo packageInfo,
            List<Finding> findings
    ) {
        // Verifica se os nomes esperados são consistentes com o desenho
        // conhecido do projeto. Não presume que todos os serviços precisem
        // de todas as permissões em qualquer versão do Android.
        if (Build.VERSION.SDK_INT < 29) {
            findings.add(new Finding(
                    Severity.INFO,
                    "FOREGROUND_TYPE_PLATFORM",
                    "A verificação do tipo de serviço em primeiro plano "
                            + "não se aplica da mesma forma a esta versão "
                            + "do Android.",
                    "Considere a versão mínima e a versão-alvo do projeto."
            ));
        }

        if (packageInfo.requestedPermissions == null) {
            findings.add(new Finding(
                    Severity.WARNING,
                    "REQUESTED_PERMISSIONS_UNAVAILABLE",
                    "A lista de permissões declaradas não está disponível "
                            + "nos dados consultados.",
                    "Confira diretamente o Manifest mesclado da compilação."
            ));
        }
    }
}
