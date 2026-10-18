
package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registro leve de eventos tecnicos do JARVIS.
 *
 * Esta versao nao persiste registros, nao acessa a Internet
 * e nao armazena automaticamente mensagens de conversas.
 *
 * O chamador deve registrar apenas dados tecnicos necessarios,
 * evitando senhas, tokens, mensagens privadas e dados pessoais.
 */
public final class JarvisAuditTrail {

    private static final int MAX_CODE_LENGTH = 80;
    private static final int MAX_DETAILS_LENGTH = 240;
    private static final int MAX_BATCH_SIZE = 500;

    private JarvisAuditTrail() {
        // Classe utilitaria.
    }

    public enum Outcome {
        SUCCESS,
        DENIED,
        REVIEW_REQUIRED,
        FAILURE,
        INFORMATION
    }

    public static final class Entry {
        private final long timestamp;
        private final String component;
        private final String eventCode;
        private final Outcome outcome;
        private final String details;

        private Entry(
                long timestamp,
                String component,
                String eventCode,
                Outcome outcome,
                String details) {

            this.timestamp = timestamp;
            this.component = component;
            this.eventCode = eventCode;
            this.outcome = outcome;
            this.details = details;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getComponent() {
            return component;
        }

        public String getEventCode() {
            return eventCode;
        }

        public Outcome getOutcome() {
            return outcome;
        }

        public String getDetails() {
            return details;
        }
    }

    /**
     * Cria um registro sem guarda-lo permanentemente.
     */
    public static Entry createEntry(
            String component,
            String eventCode,
            Outcome outcome,
            String technicalDetails) {

        if (isBlank(component)
                || isBlank(eventCode)
                || outcome == null) {
            return null;
        }

        return new Entry(
                System.currentTimeMillis(),
                sanitize(component, MAX_CODE_LENGTH),
                sanitize(eventCode, MAX_CODE_LENGTH),
                outcome,
                sanitize(technicalDetails, MAX_DETAILS_LENGTH)
        );
    }

    /**
     * Cria uma lista imutavel com os registros recebidos.
     * Nao grava os dados em arquivo ou banco de dados.
     */
    public static List<Entry> createBatch(List<Entry> entries) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        List<Entry> safeEntries = new ArrayList<>();
        int limit = Math.min(entries.size(), MAX_BATCH_SIZE);

        for (int i = 0; i < limit; i++) {
            Entry entry = entries.get(i);

            if (entry != null) {
                safeEntries.add(entry);
            }
        }

        return Collections.unmodifiableList(safeEntries);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String sanitize(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        int limit = Math.min(value.length(), maxLength);

        for (int i = 0; i < limit; i++) {
            char c = value.charAt(i);

            if (!Character.isISOControl(c)) {
                result.append(c);
            } else {
                result.append(' ');
            }
        }

        return result.toString().trim();
    }
}
