package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Grafo de conhecimento local do JARVIS Lite.
 *
 * Armazena relações explícitas entre conceitos.
 * Esta versão mantém os dados somente em memória.
 * Não substitui a memória SQLite nem confirma a veracidade dos fatos.
 */
public final class JarvisKnowledgeGraph {

    public static final int DEFAULT_MAX_NODES = 500;
    public static final int DEFAULT_MAX_RELATIONS = 2000;
    public static final int MAX_TEXT_LENGTH = 200;

    public enum Result {
        ADDED,
        ALREADY_EXISTS,
        INVALID_INPUT,
        NODE_LIMIT_REACHED,
        RELATION_LIMIT_REACHED
    }

    public static final class Node {
        private final String id;
        private final String label;

        private Node(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }
    }

    public static final class Relation {
        private final String sourceId;
        private final String relationType;
        private final String targetId;
        private final long createdAt;

        private Relation(
                String sourceId,
                String relationType,
                String targetId,
                long createdAt
        ) {
            this.sourceId = sourceId;
            this.relationType = relationType;
            this.targetId = targetId;
            this.createdAt = createdAt;
        }

        public String getSourceId() {
            return sourceId;
        }

        public String getRelationType() {
            return relationType;
        }

        public String getTargetId() {
            return targetId;
        }

        public long getCreatedAt() {
            return createdAt;
        }
    }

    private final int maxNodes;
    private final int maxRelations;

    private final Map<String, Node> nodes =
            new LinkedHashMap<String, Node>();

    private final List<Relation> relations =
            new ArrayList<Relation>();

    public JarvisKnowledgeGraph() {
        this(DEFAULT_MAX_NODES, DEFAULT_MAX_RELATIONS);
    }

    public JarvisKnowledgeGraph(int maxNodes, int maxRelations) {
        this.maxNodes = Math.max(1, Math.min(maxNodes, 5000));
        this.maxRelations = Math.max(
                1, Math.min(maxRelations, 20000));
    }

    /**
     * Adiciona um conceito ao grafo.
     */
    public synchronized Result addNode(String label) {
        String safeLabel = normalize(label);

        if (!isValidText(safeLabel)) {
            return Result.INVALID_INPUT;
        }

        String id = createId(safeLabel);

        if (nodes.containsKey(id)) {
            return Result.ALREADY_EXISTS;
        }

        if (nodes.size() >= maxNodes) {
            return Result.NODE_LIMIT_REACHED;
        }

        nodes.put(id, new Node(id, safeLabel));
        return Result.ADDED;
    }

    /**
     * Registra uma relação entre dois conceitos.
     * Os dois conceitos precisam existir previamente.
     *
     * Exemplo:
     * adicionar "Terra" e "Sistema Solar";
     * relacionar "Terra", "faz parte de", "Sistema Solar".
     */
    public synchronized Result addRelation(
            String source,
            String relationType,
            String target
    ) {
        String sourceId = createId(normalize(source));
        String type = normalize(relationType);
        String targetId = createId(normalize(target));

        if (!isValidText(sourceId)
                || !isValidText(type)
                || !isValidText(targetId)) {
            return Result.INVALID_INPUT;
        }

        if (!nodes.containsKey(sourceId)
                || !nodes.containsKey(targetId)) {
            return Result.INVALID_INPUT;
        }

        String normalizedType = type.toLowerCase(Locale.ROOT);

        for (Relation relation : relations) {
            if (relation.sourceId.equals(sourceId)
                    && relation.targetId.equals(targetId)
                    && relation.relationType.equalsIgnoreCase(
                            normalizedType)) {
                return Result.ALREADY_EXISTS;
            }
        }

        if (relations.size() >= maxRelations) {
            return Result.RELATION_LIMIT_REACHED;
        }

        relations.add(new Relation(
                sourceId,
                type,
                targetId,
                System.currentTimeMillis()
        ));

        return Result.ADDED;
    }

    /**
     * Procura um conceito pelo nome, sem diferenciar maiúsculas
     * e minúsculas.
     */
    public synchronized Node findNode(String label) {
        String id = createId(normalize(label));

        if (!isValidText(id)) {
            return null;
        }

        return nodes.get(id);
    }

    /**
     * Retorna todas as relações que saem de um conceito.
     */
    public synchronized List<Relation> getRelationsFrom(
            String source
    ) {
        String id = createId(normalize(source));
        List<Relation> result = new ArrayList<Relation>();

        for (Relation relation : relations) {
            if (relation.sourceId.equals(id)) {
                result.add(relation);
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Retorna todas as relações que chegam a um conceito.
     */
    public synchronized List<Relation> getRelationsTo(
            String target
    ) {
        String id = createId(normalize(target));
        List<Relation> result = new ArrayList<Relation>();

        for (Relation relation : relations) {
            if (relation.targetId.equals(id)) {
                result.add(relation);
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Retorna uma cópia da lista de conceitos.
     */
    public synchronized List<Node> getAllNodes() {
        return Collections.unmodifiableList(
                new ArrayList<Node>(nodes.values())
        );
    }

    /**
     * Retorna uma cópia de todas as relações.
     */
    public synchronized List<Relation> getAllRelations() {
        return Collections.unmodifiableList(
                new ArrayList<Relation>(relations)
        );
    }

    /**
     * Remove um conceito e todas as relações associadas.
     */
    public synchronized boolean removeNode(String label) {
        String id = createId(normalize(label));

        if (!isValidText(id) || nodes.remove(id) == null) {
            return false;
        }

        List<Relation> remaining = new ArrayList<Relation>();

        for (Relation relation : relations) {
            if (!relation.sourceId.equals(id)
                    && !relation.targetId.equals(id)) {
                remaining.add(relation);
            }
        }

        relations.clear();
        relations.addAll(remaining);

        return true;
    }

    /**
     * Limpa somente os dados deste grafo em memória.
     */
    public synchronized void clear() {
        nodes.clear();
        relations.clear();
    }

    public synchronized boolean containsNode(String label) {
        String id = createId(normalize(label));
        return isValidText(id) && nodes.containsKey(id);
    }

    public synchronized int getNodeCount() {
        return nodes.size();
    }

    public synchronized int getRelationCount() {
        return relations.size();
    }

    public int getMaxNodes() {
        return maxNodes;
    }

    public int getMaxRelations() {
        return maxRelations;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String createId(String label) {
        return normalize(label).toLowerCase(Locale.ROOT);
    }

    private static boolean isValidText(String value) {
        return value != null
                && !value.trim().isEmpty()
                && value.length() <= MAX_TEXT_LENGTH;
    }
}
