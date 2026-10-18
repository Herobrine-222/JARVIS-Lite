
package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro descritivo da arquitetura cognitiva.
 *
 * Nao cria nem substitui os componentes existentes.
 * Os nomes registrados nao garantem que cada modulo
 * esteja integrado ou operacional.
 */
public final class JarvisCognitiveArchitecture {

    public enum Role {
        CORE_COORDINATION,
        COGNITIVE_PROCESSING,
        REASONING,
        SPECIALIST_ROUTING,
        MEMORY,
        CONTEXT,
        EVIDENCE,
        SAFETY,
        RESPONSE_VALIDATION,
        TASK_PLANNING,
        ATTENTION,
        TEMPORARY_WORKSPACE,
        METACOGNITION,
        NEURAL_INFERENCE,
        UNKNOWN
    }

    public static final class ComponentInfo {
        private final String className;
        private final Role role;
        private final String responsibility;

        private ComponentInfo(
                String className,
                Role role,
                String responsibility) {
            this.className = className;
            this.role = role;
            this.responsibility = responsibility;
        }

        public String getClassName() {
            return className;
        }

        public Role getRole() {
            return role;
        }

        public String getResponsibility() {
            return responsibility;
        }
    }

    private final Map<String, ComponentInfo> components =
            new LinkedHashMap<>();

    public JarvisCognitiveArchitecture() {
        registerExistingComponents();
        registerPlannedComponents();
    }

    private void registerExistingComponents() {
        register(
                "JarvisCore",
                Role.CORE_COORDINATION,
                "Coordenacao geral existente.");

        register(
                "JarvisVisionCore",
                Role.SPECIALIST_ROUTING,
                "Coordenacao de modulos especialistas existente.");

        register(
                "JarvisReasoningEngine",
                Role.REASONING,
                "Classificacao e encaminhamento por regras.");

        register(
                "JarvisMemory",
                Role.MEMORY,
                "Memoria persistente existente.");

        register(
                "JarvisContextManager",
                Role.CONTEXT,
                "Gerenciamento de contexto da conversa.");

        register(
                "JarvisEvidenceEngine",
                Role.EVIDENCE,
                "Classificacao de evidencias e inferencias.");

        register(
                "JarvisSafetyGateway",
                Role.SAFETY,
                "Verificacoes de seguranca existentes.");

        register(
                "JarvisResponseVerifier",
                Role.RESPONSE_VALIDATION,
                "Verificacao de respostas existente.");

        register(
                "JarvisSpecialistCoordinator",
                Role.SPECIALIST_ROUTING,
                "Coordenacao de especialistas existente.");
    }

    private void registerPlannedComponents() {
        register(
                "JarvisCognitiveCore",
                Role.COGNITIVE_PROCESSING,
                "Coordenacao cognitiva em desenvolvimento.");

        register(
                "JarvisNeuralCore",
                Role.NEURAL_INFERENCE,
                "Interface para mecanismo de inferencia.");

        register(
                "JarvisCognitiveWorkspace",
                Role.TEMPORARY_WORKSPACE,
                "Organizacao temporaria de dados.");

        register(
                "JarvisAttentionManager",
                Role.ATTENTION,
                "Priorizacao heuristica de tarefas.");

        register(
                "JarvisCognitiveCycle",
                Role.COGNITIVE_PROCESSING,
                "Coordenacao de etapas de processamento.");

        register(
                "JarvisMetacognition",
                Role.METACOGNITION,
                "Avaliacao heuristica de respostas.");

        register(
                "JarvisTaskPlanner",
                Role.TASK_PLANNING,
                "Planejamento de etapas e dependencias.");
    }

    private void register(
            String className,
            Role role,
            String responsibility) {

        if (className == null
                || className.trim().isEmpty()
                || role == null
                || responsibility == null) {
            return;
        }

        components.put(
                className,
                new ComponentInfo(
                        className,
                        role,
                        responsibility));
    }

    public synchronized ComponentInfo getComponent(
            String className) {

        if (className == null) {
            return null;
        }

        return components.get(className);
    }

    public synchronized List<ComponentInfo> getComponents() {
        return Collections.unmodifiableList(
                new ArrayList<>(components.values()));
    }

    public synchronized List<ComponentInfo> getComponentsByRole(
            Role role) {

        if (role == null) {
            return Collections.emptyList();
        }

        List<ComponentInfo> result = new ArrayList<>();

        for (ComponentInfo info : components.values()) {
            if (info.role == role) {
                result.add(info);
            }
        }

        return Collections.unmodifiableList(result);
    }

    public synchronized int getComponentCount() {
        return components.size();
    }

    public synchronized boolean contains(String className) {
        return className != null
                && components.containsKey(className);
    }
}
