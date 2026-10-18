package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registra feedback explícito sobre respostas do JARVIS.
 *
 * Esta versão mantém dados somente em memória durante a execução.
 * Não treina redes neurais, acessa a internet ou executa ações no dispositivo.
 */
public final class JarvisLearningManager {

    public static final int DEFAULT_MAX_RECORDS = 200;
    public static final int MAX_TEXT_LENGTH = 2000;

    public enum Feedback {
        HELPFUL,
        NOT_HELPFUL,
        CORRECT,
        INCORRECT,
        UNCERTAIN
    }

    public static final class LearningRecord {
        private final long id;
        private final String prompt;
        private final String response;
        private final Feedback feedback;
        private final String note;
        private final long timestamp;

        private LearningRecord(long id, String prompt, String response,
                               Feedback feedback, String note, long timestamp) {
            this.id = id;
            this.prompt = prompt;
            this.response = response;
            this.feedback = feedback;
            this.note = note;
            this.timestamp = timestamp;
        }

        public long getId() { return id; }
        public String getPrompt() { return prompt; }
        public String getResponse() { return response; }
        public Feedback getFeedback() { return feedback; }
        public String getNote() { return note; }
        public long getTimestamp() { return timestamp; }
    }

    private final int maxRecords;
    private final LinkedHashMap<Long, LearningRecord> records =
            new LinkedHashMap<Long, LearningRecord>();

    private long nextId = 1L;
    private long acceptedCount;
    private long rejectedCount;

    public JarvisLearningManager() {
        this(DEFAULT_MAX_RECORDS);
    }

    public JarvisLearningManager(int maxRecords) {
        this.maxRecords = Math.max(1, Math.min(maxRecords, 2000));
    }

    /**
     * Registra feedback fornecido explicitamente.
     * Retorna -1 quando os dados são inválidos.
     */
    public synchronized long recordFeedback(
            String prompt,
            String response,
            Feedback feedback,
            String note) {

        String safePrompt = normalize(prompt);
        String safeResponse = normalize(response);

        if (safePrompt.isEmpty()
                || safeResponse.isEmpty()
                || feedback == null) {
            rejectedCount++;
            return -1L;
        }

        long id = nextId++;

        LearningRecord record = new LearningRecord(
                id,
                limit(safePrompt, MAX_TEXT_LENGTH),
                limit(safeResponse, MAX_TEXT_LENGTH),
                feedback,
                limit(normalize(note), 500),
                System.currentTimeMillis());

        records.put(id, record);

        while (records.size() > maxRecords) {
            Long oldestId = records.keySet().iterator().next();
            records.remove(oldestId);
        }

        acceptedCount++;
        return id;
    }

    public synchronized LearningRecord getRecord(long id) {
        return records.get(id);
    }

    public synchronized List<LearningRecord> getRecentRecords(int limit) {
        int requested = Math.max(0, Math.min(limit, maxRecords));
        List<LearningRecord> all =
                new ArrayList<LearningRecord>(records.values());
        List<LearningRecord> result = new ArrayList<LearningRecord>();

        for (int i = all.size() - 1;
             i >= 0 && result.size() < requested; i--) {
            result.add(all.get(i));
        }

        return Collections.unmodifiableList(result);
    }

    public synchronized List<LearningRecord> getRecordsByFeedback(
            Feedback feedback) {

        List<LearningRecord> result = new ArrayList<LearningRecord>();

        if (feedback == null) {
            return Collections.unmodifiableList(result);
        }

        for (LearningRecord record : records.values()) {
            if (record.getFeedback() == feedback) {
                result.add(record);
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Estatísticas descritivas; não representam uma medida real
     * de inteligência ou precisão do modelo.
     */
    public synchronized Map<String, Long> getStatistics() {
        Map<String, Long> result = new LinkedHashMap<String, Long>();

        result.put("storedRecords", (long) records.size());
        result.put("maxRecords", (long) maxRecords);
        result.put("acceptedFeedback", acceptedCount);
        result.put("rejectedFeedback", rejectedCount);
        result.put("helpful", count(Feedback.HELPFUL));
        result.put("notHelpful", count(Feedback.NOT_HELPFUL));
        result.put("correct", count(Feedback.CORRECT));
        result.put("incorrect", count(Feedback.INCORRECT));
        result.put("uncertain", count(Feedback.UNCERTAIN));

        return Collections.unmodifiableMap(result);
    }

    public synchronized boolean removeRecord(long id) {
        return records.remove(id) != null;
    }

    public synchronized void clear() {
        records.clear();
    }

    public synchronized int size() {
        return records.size();
    }

    public int getMaxRecords() {
        return maxRecords;
    }

    private long count(Feedback feedback) {
        long total = 0L;

        for (LearningRecord record : records.values()) {
            if (record.getFeedback() == feedback) {
                total++;
            }
        }

        return total;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String limit(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength);
    }
}
