package com.jarvis.lite;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * Cria resumos locais e determinísticos de textos de conversa.
 *
 * Usa regras e seleção de frases.
 * Não utiliza internet, não treina modelos e não altera
 * arquivos, memória persistente ou código-fonte.
 */
public final class JarvisConversationSummarizer {

    public static final int DEFAULT_MAX_SENTENCES = 5;
    public static final int MAX_SENTENCES_LIMIT = 20;
    public static final int DEFAULT_MAX_INPUT_LENGTH = 50000;
    public static final int MAX_INPUT_LENGTH_LIMIT = 200000;
    public static final int DEFAULT_MAX_OUTPUT_LENGTH = 2000;
    public static final int MAX_OUTPUT_LENGTH_LIMIT = 10000;

    public enum Status {
        SUCCESS,
        EMPTY_INPUT,
        INPUT_TOO_LONG,
        NO_USABLE_SENTENCES
    }

    public static final class SummaryResult {

        private final Status status;
        private final String summary;
        private final int inputCharacters;
        private final int sentenceCount;
        private final int selectedSentenceCount;

        private SummaryResult(
                Status status,
                String summary,
                int inputCharacters,
                int sentenceCount,
                int selectedSentenceCount
        ) {
            this.status = status;
            this.summary = summary;
            this.inputCharacters = inputCharacters;
            this.sentenceCount = sentenceCount;
            this.selectedSentenceCount = selectedSentenceCount;
        }

        public Status getStatus() {
            return status;
        }

        public String getSummary() {
            return summary;
        }

        public int getInputCharacters() {
            return inputCharacters;
        }

        public int getSentenceCount() {
            return sentenceCount;
        }

        public int getSelectedSentenceCount() {
            return selectedSentenceCount;
        }

        public boolean isSuccessful() {
            return status == Status.SUCCESS;
        }
    }

    private static final class SentenceScore {

        private final String text;
        private final int originalIndex;
        private final double score;

        private SentenceScore(
                String text,
                int originalIndex,
                double score
        ) {
            this.text = text;
            this.originalIndex = originalIndex;
            this.score = score;
        }
    }

    private final int maxSentences;
    private final int maxInputLength;
    private final int maxOutputLength;

    public JarvisConversationSummarizer() {
        this(
                DEFAULT_MAX_SENTENCES,
                DEFAULT_MAX_INPUT_LENGTH,
                DEFAULT_MAX_OUTPUT_LENGTH
        );
    }

    public JarvisConversationSummarizer(
            int maxSentences,
            int maxInputLength,
            int maxOutputLength
    ) {
        this.maxSentences = clamp(
                maxSentences, 1, MAX_SENTENCES_LIMIT
        );

        this.maxInputLength = clamp(
                maxInputLength, 1, MAX_INPUT_LENGTH_LIMIT
        );

        this.maxOutputLength = clamp(
                maxOutputLength, 1, MAX_OUTPUT_LENGTH_LIMIT
        );
    }

    /**
     * Resume o texto selecionando frases relevantes.
     */
    public SummaryResult summarize(String input) {

        if (input == null || input.trim().isEmpty()) {
            return result(
                    Status.EMPTY_INPUT, "", 0, 0, 0
            );
        }

        if (input.length() > maxInputLength) {
            return result(
                    Status.INPUT_TOO_LONG,
                    "",
                    input.length(),
                    0,
                    0
            );
        }

        List<String> sentences = splitSentences(input);

        if (sentences.isEmpty()) {
            return result(
                    Status.NO_USABLE_SENTENCES,
                    "",
                    input.length(),
                    0,
                    0
            );
        }

        if (sentences.size() <= maxSentences) {
            String complete = joinWithinLimit(
                    sentences,
                    maxOutputLength
            );

            if (complete.isEmpty()) {
                return result(
                        Status.NO_USABLE_SENTENCES,
                        "",
                        input.length(),
                        sentences.size(),
                        0
                );
            }

            return result(
                    Status.SUCCESS,
                    complete,
                    input.length(),
                    sentences.size(),
                    countSentences(complete)
            );
        }

        List<SentenceScore> scored =
                scoreSentences(sentences);

        Collections.sort(
                scored,
                new Comparator<SentenceScore>() {
                    @Override
                    public int compare(
                            SentenceScore a,
                            SentenceScore b
                    ) {
                        int byScore = Double.compare(
                                b.score, a.score
                        );

                        if (byScore != 0) {
                            return byScore;
                        }

                        return Integer.compare(
                                a.originalIndex,
                                b.originalIndex
                        );
                    }
                }
        );

        List<SentenceScore> chosen =
                new ArrayList<SentenceScore>();

        for (SentenceScore item : scored) {
            if (chosen.size() >= maxSentences) {
                break;
            }

            chosen.add(item);
        }

        Collections.sort(
                chosen,
                new Comparator<SentenceScore>() {
                    @Override
                    public int compare(
                            SentenceScore a,
                            SentenceScore b
                    ) {
                        return Integer.compare(
                                a.originalIndex,
                                b.originalIndex
                        );
                    }
                }
        );

        List<String> selectedTexts =
                new ArrayList<String>();

        for (SentenceScore item : chosen) {
            selectedTexts.add(item.text);
        }

        String summary = joinWithinLimit(
                selectedTexts,
                maxOutputLength
        );

        if (summary.isEmpty()) {
            return result(
                    Status.NO_USABLE_SENTENCES,
                    "",
                    input.length(),
                    sentences.size(),
                    0
            );
        }

        return result(
                Status.SUCCESS,
                summary,
                input.length(),
                sentences.size(),
                countSentences(summary)
        );
    }

    /**
     * Divide o texto em frases usando pontuação básica.
     */
    public List<String> splitSentences(String input) {

        if (input == null || input.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> sentences = new ArrayList<String>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            current.append(c);

            boolean endOfSentence =
                    c == '.' || c == '!' || c == '?'
                    || c == '\n';

            if (endOfSentence) {
                addSentence(sentences, current);
            }
        }

        addSentence(sentences, current);

        return Collections.unmodifiableList(sentences);
    }

    private List<SentenceScore> scoreSentences(
            List<String> sentences
    ) {
        List<SentenceScore> result =
                new ArrayList<SentenceScore>();

        Set<String> keywords = new LinkedHashSet<String>();

        for (String sentence : sentences) {
            for (String word : words(sentence)) {
                if (word.length() >= 4 && !isStopWord(word)) {
                    keywords.add(word);
                }
            }
        }

        for (int i = 0; i < sentences.size(); i++) {

            String sentence = sentences.get(i);
            List<String> sentenceWords = words(sentence);

            int usefulWords = 0;
            int keywordMatches = 0;

            for (String word : sentenceWords) {
                if (word.length() >= 4 && !isStopWord(word)) {
                    usefulWords++;

                    if (keywords.contains(word)) {
                        keywordMatches++;
                    }
                }
            }

            double score = 0.0;

            if (usefulWords > 0) {
                score += (double) keywordMatches / usefulWords;
            }

            String normalized = normalize(sentence);

            if (containsAny(
                    normalized,
                    "importante",
                    "objetivo",
                    "conclusao",
                    "problema",
                    "solucao",
                    "decidimos",
                    "precisamos",
                    "resultado",
                    "resumo",
                    "lembre",
                    "memoria"
            )) {
                score += 1.5;
            }

            if (i == 0) {
                score += 0.5;
            }

            if (sentence.length() >= 30
                    && sentence.length() <= 240) {
                score += 0.5;
            }

            if (sentenceWords.size() < 3) {
                score -= 1.0;
            }

            result.add(
                    new SentenceScore(sentence, i, score)
            );
        }

        return result;
    }

    private static List<String> words(String sentence) {

        List<String> result = new ArrayList<String>();

        String normalized = normalize(sentence);
        String[] parts = normalized.split("[^a-z0-9]+");

        for (String part : parts) {
            if (!part.isEmpty()) {
                result.add(part);
            }
        }

        return result;
    }

    private static boolean isStopWord(String word) {

        switch (word) {
            case "para":
            case "como":
            case "mais":
            case "menos":
            case "muito":
            case "muita":
            case "muitos":
            case "muitas":
            case "isso":
            case "essa":
            case "esse":
            case "aqui":
            case "sobre":
            case "entre":
            case "quando":
            case "onde":
            case "porque":
            case "tambem":
            case "entao":
            case "depois":
            case "antes":
            case "ainda":
            case "estao":
            case "foram":
            case "seria":
            case "sendo":
            case "pode":
            case "podem":
            case "deve":
            case "devem":
            case "cada":
            case "todo":
            case "toda":
            case "todos":
            case "todas":
            case "com":
            case "sem":
            case "dos":
            case "das":
            case "que":
            case "uma":
            case "uns":
            case "umas":
            case "por":
            case "nos":
            case "nas":
            case "aos":
            case "sua":
            case "seu":
            case "suas":
            case "seus":
            case "eles":
            case "elas":
            case "este":
            case "esta":
            case "estes":
            case "estas":
            case "aquela":
            case "aquele":
            case "aquilo":
            case "esta":
            case "ser":
            case "ter":
            case "tem":
            case "foi":
            case "sao":
            case "era":
            case "e":
            case "ou":
            case "de":
            case "do":
            case "da":
            case "em":
            case "no":
            case "na":
            case "o":
            case "a":
            case "os":
            case "as":
            case "um":
            case "ao":
            case "me":
            case "te":
            case "se":
            case "eu":
            case "tu":
            case "ele":
            case "ela":
            case "nos":
            case "vos":
            case "lhe":
            case "lhes":
            case "ja":
            case "nao":
            case "sim":
                return true;

            default:
                return false;
        }
    }

    private static boolean containsAny(
            String text,
            String... terms
    ) {
        for (String term : terms) {
            if (text.contains(normalize(term))) {
                return true;
            }
        }

        return false;
    }

    private static String normalize(String text) {

        if (text == null) {
            return "";
        }

        String decomposed = Normalizer.normalize(
                text.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );

        return decomposed
                .replaceAll("\\p{M}+", "")
                .trim();
    }

    private static void addSentence(
            List<String> sentences,
            StringBuilder current
    ) {
        String sentence = current.toString().trim();

        if (!sentence.isEmpty()) {
            sentences.add(sentence);
        }

        current.setLength(0);
    }

    private static String joinWithinLimit(
            List<String> sentences,
            int limit
    ) {
        StringBuilder result = new StringBuilder();

        for (String sentence : sentences) {

            String candidate = result.length() == 0
                    ? sentence
                    : result.toString() + " " + sentence;

            if (candidate.length() > limit) {

                if (result.length() == 0) {
                    result.append(
                            sentence.substring(
                                    0,
                                    Math.min(limit, sentence.length())
                            )
                    );
                }

                break;
            }

            if (result.length() > 0) {
                result.append(' ');
            }

            result.append(sentence);
        }

        return result.toString().trim();
    }

    private static int countSentences(String text) {

        if (text == null || text.trim().isEmpty()) {
            return 0;
        }

        return text.split("[.!?]+\\s*|\\n+").length;
    }

    private static int clamp(
            int value,
            int minimum,
            int maximum
    ) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    private SummaryResult result(
            Status status,
            String summary,
            int inputCharacters,
            int sentenceCount,
            int selectedSentenceCount
    ) {
        return new SummaryResult(
                status,
                summary,
                inputCharacters,
                sentenceCount,
                selectedSentenceCount
        );
    }

    public int getMaxSentences() {
        return maxSentences;
    }

    public int getMaxInputLength() {
        return maxInputLength;
    }

    public int getMaxOutputLength() {
        return maxOutputLength;
    }
}
