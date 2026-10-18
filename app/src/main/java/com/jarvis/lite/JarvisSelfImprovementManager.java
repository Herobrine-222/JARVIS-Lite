package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Organiza sugestões de melhoria para o JARVIS Lite.
 *
 * Não modifica arquivos, não executa comandos e não aplica
 * alterações automaticamente. As sugestões precisam ser
 * analisadas antes de qualquer implementação.
 */
public final class JarvisSelfImprovementManager {

    public static final int DEFAULT_MAX_SUGGESTIONS = 200;
    public static final int MAX_TEXT_LENGTH = 2000;

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum Status {
        PROPOSED,
        UNDER_REVIEW,
        APPROVED,
        REJECTED,
        IMPLEMENTED
    }

    public enum Category {
        ACCURACY,
        REASONING,
        MEMORY,
        PERFORMANCE,
        SECURITY,
        VOICE,
        USER_INTERFACE,
        RELIABILITY,
        OTHER
    }

    public static final class Suggestion {

        private final long id;
        private final String title;
        private final String description;
        private final String evidence;
        private final Category category;
        private final Priority priority;
        private final long createdAt;

        private Status status;
        private String reviewNote;
        private long updatedAt;

        private Suggestion(
                long id,
                String title,
                String description,
                String evidence,
                Category category,
                Priority priority,
                long createdAt
        ) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.evidence = evidence;
            this.category = category;
            this.priority = priority;
            this.createdAt = createdAt;
            this.status = Status.PROPOSED;
            this.reviewNote = "";
            this.updatedAt = createdAt;
        }

        public long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getEvidence() {
            return evidence;
        }

        public Category getCategory() {
            return category;
        }

        public Priority getPriority() {
            return priority;
        }

        public Status getStatus() {
            return status;
        }

        public String getReviewNote() {
            return reviewNote;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public long getUpdatedAt() {
            return updatedAt;
        }
    }

    private final int maxSuggestions;

    private final LinkedHashMap<Long, Suggestion> suggestions =
            new LinkedHashMap<Long, Suggestion>();

    private long nextId = 1L;

    public JarvisSelfImprovementManager() {
        this(DEFAULT_MAX_SUGGESTIONS);
    }

    public JarvisSelfImprovementManager(int maxSuggestions) {
        this.maxSuggestions = Math.max(
                1,
                Math.min(maxSuggestions, 2000)
        );
    }

    /**
     * Registra uma nova sugestão.
     * Retorna -1 quando os dados são inválidos ou o limite
     * de registros foi atingido.
     */
    public synchronized long propose(
            String title,
            String description,
            String evidence,
            Category category,
            Priority priority
    ) {
        String safeTitle = normalize(title);
        String safeDescription = normalize(description);
        String safeEvidence = normalize(evidence);

        if (!isValidText(safeTitle)
                || !isValidText(safeDescription)
                || category == null
                || priority == null) {
            return -1L;
        }

        if (suggestions.size() >= maxSuggestions) {
            return -1L;
        }

        long id = nextId++;
        long now = System.currentTimeMillis();

        Suggestion suggestion = new Suggestion(
                id,
                limit(safeTitle, 200),
                limit(safeDescription, MAX_TEXT_LENGTH),
                limit(safeEvidence, MAX_TEXT_LENGTH),
                category,
                priority,
                now
        );

        suggestions.put(id, suggestion);

        return id;
    }

    /**
     * Atualiza o estado de uma sugestão.
     *
     * A implementação não é executada por este método:
     * IMPLEMENTED é apenas um estado informativo que deve
     * ser usado após a implementação ser verificada externamente.
     */
    public synchronized boolean updateStatus(
            long id,
            Status newStatus,
            String reviewNote
    ) {
        if (newStatus == null) {
            return false;
        }

        Suggestion suggestion = suggestions.get(id);

        if (suggestion == null) {
            return false;
        }

        if (!isAllowedTransition(
                suggestion.status,
                newStatus
        )) {
            return false;
        }

        suggestion.status = newStatus;
        suggestion.reviewNote = limit(
                normalize(reviewNote),
                1000
        );
        suggestion.updatedAt = System.currentTimeMillis();

        return true;
    }

    /**
     * Busca uma sugestão pelo identificador.
     */
    public synchronized Suggestion getSuggestion(long id) {
        return suggestions.get(id);
    }

    /**
     * Retorna todas as sugestões em ordem de criação.
     */
    public synchronized List<Suggestion> getAllSuggestions() {
        return Collections.unmodifiableList(
                new ArrayList<Suggestion>(
                        suggestions.values()
                )
        );
    }

    /**
     * Retorna sugestões filtradas por estado.
     */
    public synchronized List<Suggestion> getByStatus(
            Status status
    ) {
        List<Suggestion> result =
                new ArrayList<Suggestion>();

        if (status == null) {
            return Collections.unmodifiableList(result);
        }

        for (Suggestion suggestion : suggestions.values()) {
            if (suggestion.status == status) {
                result.add(suggestion);
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Retorna sugestões filtradas por categoria.
     */
    public synchronized List<Suggestion> getByCategory(
            Category category
    ) {
        List<Suggestion> result =
                new ArrayList<Suggestion>();

        if (category == null) {
            return Collections.unmodifiableList(result);
        }

        for (Suggestion suggestion : suggestions.values()) {
            if (suggestion.category == category) {
                result.add(suggestion);
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Retorna sugestões por prioridade.
     */
    public synchronized List<Suggestion> getByPriority(
            Priority priority
    ) {
        List<Suggestion> result =
                new ArrayList<Suggestion>();

        if (priority == null) {
            return Collections.unmodifiableList(result);
        }

        for (Suggestion suggestion : suggestions.values()) {
            if (suggestion.priority == priority) {
                result.add(suggestion);
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Remove somente sugestões ainda propostas ou rejeitadas.
     * Sugestões em análise, aprovadas ou implementadas não
     * são removidas por este método.
     */
    public synchronized boolean removeSuggestion(long id) {
        Suggestion suggestion = suggestions.get(id);

        if (suggestion == null) {
            return false;
        }

        if (suggestion.status != Status.PROPOSED
                && suggestion.status != Status.REJECTED) {
            return false;
        }

        suggestions.remove(id);
        return true;
    }

    /**
     * Estatísticas do registro de sugestões.
     */
    public synchronized Map<String, Integer> getStatistics() {
        Map<String, Integer> result =
                new LinkedHashMap<String, Integer>();

        result.put("total", suggestions.size());
        result.put("proposed", count(Status.PROPOSED));
        result.put("underReview", count(Status.UNDER_REVIEW));
        result.put("approved", count(Status.APPROVED));
        result.put("rejected", count(Status.REJECTED));
        result.put("implemented", count(Status.IMPLEMENTED));

        return Collections.unmodifiableMap(result);
    }

    public synchronized int size() {
        return suggestions.size();
    }

    public int getMaxSuggestions() {
        return maxSuggestions;
    }

    private int count(Status status) {
        int total = 0;

        for (Suggestion suggestion : suggestions.values()) {
            if (suggestion.status == status) {
                total++;
            }
        }

        return total;
    }

    private static boolean isAllowedTransition(
            Status current,
            Status next
    ) {
        if (current == next) {
            return true;
        }

        switch (current) {
            case PROPOSED:
                return next == Status.UNDER_REVIEW
                        || next == Status.REJECTED;

            case UNDER_REVIEW:
                return next == Status.APPROVED
                        || next == Status.REJECTED
                        || next == Status.PROPOSED;

            case APPROVED:
                return next == Status.IMPLEMENTED
                        || next == Status.REJECTED
                        || next == Status.UNDER_REVIEW;

            case REJECTED:
                return next == Status.PROPOSED;

            case IMPLEMENTED:
                return false;

            default:
                return false;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isValidText(String value) {
        return value != null
                && !value.trim().isEmpty()
                && value.length() <= MAX_TEXT_LENGTH;
    }

    private static String limit(
            String value,
            int maxLength
    ) {
        if (value == null) {
            return "";
        }

        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength);
    }
}
