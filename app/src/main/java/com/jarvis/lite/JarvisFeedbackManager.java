
package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registra feedback sobre respostas do JARVIS Lite.
 *
 * Esta versão mantém os registros apenas em memória.
 * Não altera código-fonte, não executa comandos e não
 * realiza treinamento automático de modelos neurais.
 */
public final class JarvisFeedbackManager {

    public static final int DEFAULT_MAX_RECORDS = 500;
    public static final int MAX_RECORDS_LIMIT = 5000;
    public static final int MAX_TEXT_LENGTH = 2000;

    public enum Rating {
        VERY_BAD,
        BAD,
        NEUTRAL,
        GOOD,
        VERY_GOOD
    }

    public enum FeedbackType {
        ACCURACY,
        RELEVANCE,
        CLARITY,
        COMPLETENESS,
        SAFETY,
        GENERAL
    }

    public enum ResultCode {
        SUCCESS,
        INVALID_INPUT,
        RECORD_NOT_FOUND,
        RECORD_LIMIT_REACHED
    }

    public static final class FeedbackRecord {

        private final long id;
        private final String question;
        private final String answer;
        private final Rating rating;
        private final FeedbackType type;
        private final String comment;
        private final long createdAt;

        private FeedbackRecord(
                long id,
                String question,
                String answer,
                Rating rating,
                FeedbackType type,
                String comment
        ) {
            this.id = id;
            this.question = question;
            this.answer = answer;
            this.rating = rating;
            this.type = type;
            this.comment = comment;
            this.createdAt = System.currentTimeMillis();
        }

        public long getId() {
            return id;
        }

        public String getQuestion() {
            return question;
        }

        public String getAnswer() {
            return answer;
        }

        public Rating getRating() {
            return rating;
        }

        public FeedbackType getType() {
            return type;
        }

        public String getComment() {
            return comment;
        }

        public long getCreatedAt() {
            return createdAt;
        }
    }

    public static final class FeedbackSummary {

        private final int total;
        private final int positive;
        private final int neutral;
        private final int negative;
        private final double averageScore;

        private FeedbackSummary(
                int total,
                int positive,
                int neutral,
                int negative,
                double averageScore
        ) {
            this.total = total;
            this.positive = positive;
            this.neutral = neutral;
            this.negative = negative;
            this.averageScore = averageScore;
        }

        public int getTotal() {
            return total;
        }

        public int getPositive() {
            return positive;
        }

        public int getNeutral() {
            return neutral;
        }

        public int getNegative() {
            return negative;
        }

        public double getAverageScore() {
            return averageScore;
        }
    }

    private final int maxRecords;

    private final Map<Long, FeedbackRecord> records =
            new LinkedHashMap<Long, FeedbackRecord>();

    private long nextId = 1L;

    public JarvisFeedbackManager() {
        this(DEFAULT_MAX_RECORDS);
    }

    public JarvisFeedbackManager(int maxRecords) {
        this.maxRecords = Math.max(
                1,
                Math.min(maxRecords, MAX_RECORDS_LIMIT)
        );
    }

    /**
     * Registra uma avaliação da resposta.
     *
     * @return ID do registro ou -1 se os dados forem inválidos
     *         ou o limite de registros tiver sido atingido.
     */
    public synchronized long recordFeedback(
            String question,
            String answer,
            Rating rating,
            FeedbackType type,
            String comment
    ) {
        String safeQuestion = normalize(question);
        String safeAnswer = normalize(answer);
        String safeComment = normalize(comment);

        if (safeQuestion.isEmpty()
                || safeAnswer.isEmpty()
                || safeQuestion.length() > MAX_TEXT_LENGTH
                || safeAnswer.length() > MAX_TEXT_LENGTH
                || safeComment.length() > MAX_TEXT_LENGTH
                || rating == null
                || type == null) {
            return -1L;
        }

        if (records.size() >= maxRecords) {
            return -1L;
        }

        long id = nextId++;

        FeedbackRecord record = new FeedbackRecord(
                id,
                safeQuestion,
                safeAnswer,
                rating,
                type,
                safeComment
        );

        records.put(id, record);

        return id;
    }

    public synchronized FeedbackRecord getFeedback(long id) {
        return records.get(id);
    }

    public synchronized List<FeedbackRecord> getAllFeedback() {
        return Collections.unmodifiableList(
                new ArrayList<FeedbackRecord>(records.values())
        );
    }

    public synchronized List<FeedbackRecord> getFeedbackByRating(
            Rating rating
    ) {
        List<FeedbackRecord> result =
                new ArrayList<FeedbackRecord>();

        if (rating == null) {
            return Collections.unmodifiableList(result);
        }

        for (FeedbackRecord record : records.values()) {
            if (record.getRating() == rating) {
                result.add(record);
            }
        }

        return Collections.unmodifiableList(result);
    }

    public synchronized List<FeedbackRecord> getFeedbackByType(
            FeedbackType type
    ) {
        List<FeedbackRecord> result =
                new ArrayList<FeedbackRecord>();

        if (type == null) {
            return Collections.unmodifiableList(result);
        }

        for (FeedbackRecord record : records.values()) {
            if (record.getType() == type) {
                result.add(record);
            }
        }

        return Collections.unmodifiableList(result);
    }

    public synchronized ResultCode removeFeedback(long id) {
        return records.remove(id) == null
                ? ResultCode.RECORD_NOT_FOUND
                : ResultCode.SUCCESS;
    }

    /**
     * Calcula um resumo das avaliações.
     *
     * Escala numérica:
     * VERY_BAD = 1, BAD = 2, NEUTRAL = 3,
     * GOOD = 4, VERY_GOOD = 5.
     */
    public synchronized FeedbackSummary getSummary() {
        int total = records.size();
        int positive = 0;
        int neutral = 0;
        int negative = 0;
        int scoreSum = 0;

        for (FeedbackRecord record : records.values()) {
            int score = ratingToScore(record.getRating());

            scoreSum += score;

            if (score >= 4) {
                positive++;
            } else if (score == 3) {
                neutral++;
            } else {
                negative++;
            }
        }

        double average = total == 0
                ? 0.0
                : (double) scoreSum / (double) total;

        return new FeedbackSummary(
                total,
                positive,
                neutral,
                negative,
                average
        );
    }

    public synchronized int getRecordCount() {
        return records.size();
    }

    public int getMaxRecords() {
        return maxRecords;
    }

    /**
     * Apaga apenas os registros temporários deste módulo.
     * Não modifica a memória SQLite nem outros arquivos.
     */
    public synchronized void clear() {
        records.clear();
    }

    private static int ratingToScore(Rating rating) {
        switch (rating) {
            case VERY_BAD:
                return 1;

            case BAD:
                return 2;

            case NEUTRAL:
                return 3;

            case GOOD:
                return 4;

            case VERY_GOOD:
                return 5;

            default:
                return 3;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
