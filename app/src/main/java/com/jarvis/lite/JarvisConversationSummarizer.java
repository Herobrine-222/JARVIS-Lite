package com.jarvis.lite;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resumidor local de conversas do JARVIS Lite.
 *
 * Recursos:
 * - Resumo extrativo por relevancia.
 * - Deteccao de perguntas, decisoes e tarefas.
 * - Identificacao de referencias contextuais em portugues.
 * - Historico temporario de mensagens.
 * - Busca da mensagem anterior e da ultima resposta.
 * - Deteccao conservadora de referencias ambiguas.
 *
 * Limitacoes:
 * - Nao e um modelo de linguagem.
 * - Nao resolve perfeitamente todos os pronomes.
 * - O historico existe somente nesta instancia.
 * - Nao acessa internet nem modifica arquivos externos.
 */
public final class JarvisConversationSummarizer {

    public static final int DEFAULT_MAX_SENTENCES = 5;
    public static final int MAX_SENTENCES_LIMIT = 20;

    public static final int DEFAULT_MAX_INPUT_LENGTH = 50000;
    public static final int MAX_INPUT_LENGTH_LIMIT = 200000;

    public static final int DEFAULT_MAX_OUTPUT_LENGTH = 2000;
    public static final int MAX_OUTPUT_LENGTH_LIMIT = 10000;

    public static final int DEFAULT_MAX_MESSAGES = 200;
    public static final int MAX_MESSAGES_LIMIT = 2000;

    public static final int MAX_MESSAGE_LENGTH = 10000;

    public enum Status {
        SUCCESS,
        EMPTY_INPUT,
        INPUT_TOO_LONG,
        NO_USABLE_SENTENCES
    }

    public enum MessageRole {
        USER,
        ASSISTANT,
        SYSTEM,
        OTHER
    }

    public enum ReferenceType {
        PRONOUN,
        DEMONSTRATIVE,
        PREVIOUS_MESSAGE,
        LAST_ANSWER,
        LAST_QUESTION,
        LAST_TOPIC,
        EXPLICIT_REFERENCE,
        UNKNOWN
    }

    public enum ReferenceStatus {
        RESOLVED,
        AMBIGUOUS,
        NOT_FOUND,
        NO_CONTEXT
    }

    public enum SummaryCategory {
        GENERAL,
        QUESTION,
        DECISION,
        GOAL,
        PROBLEM,
        TASK,
        IMPORTANT
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

    public static final class ConversationMessage {

        private final long id;
        private final MessageRole role;
        private final String text;
        private final long timestamp;

        private ConversationMessage(
                long id,
                MessageRole role,
                String text,
                long timestamp
        ) {
            this.id = id;
            this.role = role;
            this.text = text;
            this.timestamp = timestamp;
        }

        public long getId() {
            return id;
        }

        public MessageRole getRole() {
            return role;
        }

        public String getText() {
            return text;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    public static final class ReferenceResult {

        private final ReferenceStatus status;
        private final ReferenceType type;
        private final String originalExpression;
        private final String resolvedText;
        private final long messageId;
        private final String explanation;

        private ReferenceResult(
                ReferenceStatus status,
                ReferenceType type,
                String originalExpression,
                String resolvedText,
                long messageId,
                String explanation
        ) {
            this.status = status;
            this.type = type;
            this.originalExpression = originalExpression;
            this.resolvedText = resolvedText;
            this.messageId = messageId;
            this.explanation = explanation;
        }

        public ReferenceStatus getStatus() {
            return status;
        }

        public ReferenceType getType() {
            return type;
        }

        public String getOriginalExpression() {
            return originalExpression;
        }

        public String getResolvedText() {
            return resolvedText;
        }

        public long getMessageId() {
            return messageId;
        }

        public String getExplanation() {
            return explanation;
        }

        public boolean isResolved() {
            return status == ReferenceStatus.RESOLVED;
        }
    }

    public static final class SummarySection {

        private final SummaryCategory category;
        private final String text;

        private SummarySection(
                SummaryCategory category,
                String text
        ) {
            this.category = category;
            this.text = text;
        }

        public SummaryCategory getCategory() {
            return category;
        }

        public String getText() {
            return text;
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

    private static final class ReferenceMatch {

        private final ReferenceType type;
        private final String expression;

        private ReferenceMatch(
                ReferenceType type,
                String expression
        ) {
            this.type = type;
            this.expression = expression;
        }
    }

    private final int maxSentences;
    private final int maxInputLength;
    private final int maxOutputLength;
    private final int maxMessages;

    private final List<ConversationMessage> messages =
            new ArrayList<ConversationMessage>();

    private long nextMessageId = 1L;

    public JarvisConversationSummarizer() {
        this(
                DEFAULT_MAX_SENTENCES,
                DEFAULT_MAX_INPUT_LENGTH,
                DEFAULT_MAX_OUTPUT_LENGTH,
                DEFAULT_MAX_MESSAGES
        );
    }

    public JarvisConversationSummarizer(
            int maxSentences,
            int maxInputLength,
            int maxOutputLength
    ) {
        this(
                maxSentences,
                maxInputLength,
                maxOutputLength,
                DEFAULT_MAX_MESSAGES
        );
    }

    public JarvisConversationSummarizer(
            int maxSentences,
            int maxInputLength,
            int maxOutputLength,
            int maxMessages
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

        this.maxMessages = clamp(
                maxMessages, 1, MAX_MESSAGES_LIMIT
        );
    }

    /**
     * Resume um texto selecionando frases relevantes.
     */
    public SummaryResult summarize(String input) {

        if (input == null || input.trim().isEmpty()) {
            return createSummaryResult(
                    Status.EMPTY_INPUT, "", 0, 0, 0
            );
        }

        if (input.length() > maxInputLength) {
            return createSummaryResult(
                    Status.INPUT_TOO_LONG,
                    "",
                    input.length(),
                    0,
                    0
            );
        }

        List<String> sentences = splitSentences(input);

        if (sentences.isEmpty()) {
            return createSummaryResult(
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
                return createSummaryResult(
                        Status.NO_USABLE_SENTENCES,
                        "",
                        input.length(),
                        sentences.size(),
                        0
                );
            }

            return createSummaryResult(
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
                        int comparison = Double.compare(
                                b.score, a.score
                        );

                        if (comparison != 0) {
                            return comparison;
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

        for (SentenceScore sentence : scored) {
            if (chosen.size() >= maxSentences) {
                break;
            }

            chosen.add(sentence);
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

        List<String> selected =
                new ArrayList<String>();

        for (SentenceScore sentence : chosen) {
            selected.add(sentence.text);
        }

        String summary = joinWithinLimit(
                selected,
                maxOutputLength
        );

        if (summary.isEmpty()) {
            return createSummaryResult(
                    Status.NO_USABLE_SENTENCES,
                    "",
                    input.length(),
                    sentences.size(),
                    0
            );
        }

        return createSummaryResult(
                Status.SUCCESS,
                summary,
                input.length(),
                sentences.size(),
                countSentences(summary)
        );
    }

    /**
     * Divide o texto em frases usando pontuacao basica.
     */
    public List<String> splitSentences(String input) {

        if (input == null || input.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<String>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            current.append(c);

            if (c == '.' || c == '!' || c == '?'
                    || c == '\n') {
                addSentence(result, current);
            }
        }

        addSentence(result, current);

        return Collections.unmodifiableList(result);
    }

    /**
     * Classifica frases de um texto em categorias simples.
     */
    public List<SummarySection> categorizeSentences(
            String input
    ) {
        if (input == null || input.trim().isEmpty()
                || input.length() > maxInputLength) {
            return Collections.emptyList();
        }

        List<SummarySection> result =
                new ArrayList<SummarySection>();

        for (String sentence : splitSentences(input)) {
            result.add(
                    new SummarySection(
                            classifySentence(sentence),
                            sentence
                    )
            );
        }

        return Collections.unmodifiableList(result);
    }

    public SummaryCategory classifySentence(String sentence) {

        String text = normalize(sentence);

        if (text.isEmpty()) {
            return SummaryCategory.GENERAL;
        }

        if (text.contains("?")
                || startsWithAny(
                        text,
                        "quem ",
                        "quando ",
                        "onde ",
                        "como ",
                        "por que ",
                        "qual ",
                        "quais ",
                        "quanto ",
                        "quantos ",
                        "quantas "
                )) {
            return SummaryCategory.QUESTION;
        }

        if (containsAny(
                text,
                "decidimos",
                "ficou decidido",
                "a decisao",
                "vamos manter",
                "escolhemos"
        )) {
            return SummaryCategory.DECISION;
        }

        if (containsAny(
                text,
                "nosso objetivo",
                "meu objetivo",
                "o objetivo",
                "pretendo",
                "quero construir",
                "quero criar"
        )) {
            return SummaryCategory.GOAL;
        }

        if (containsAny(
                text,
                "erro",
                "problema",
                "falha",
                "bug",
                "nao funciona",
                "nao compilou",
                "incompatibilidade"
        )) {
            return SummaryCategory.PROBLEM;
        }

        if (containsAny(
                text,
                "precisamos",
                "proximo passo",
                "depois devemos",
                "falta fazer",
                "implementar",
                "corrigir",
                "verificar"
        )) {
            return SummaryCategory.TASK;
        }

        if (containsAny(
                text,
                "importante",
                "essencial",
                "obrigatorio",
                "nunca apagar",
                "preservar",
                "nao substituir"
        )) {
            return SummaryCategory.IMPORTANT;
        }

        return SummaryCategory.GENERAL;
    }

    /**
     * Guarda uma mensagem no historico temporario desta instancia.
     * Mensagens antigas sao removidas quando o limite e excedido.
     */
    public synchronized long addMessage(
            MessageRole role,
            String text
    ) {
        if (role == null || text == null) {
            return -1L;
        }

        String safeText = text.trim();

        if (safeText.isEmpty()
                || safeText.length() > MAX_MESSAGE_LENGTH) {
            return -1L;
        }

        long id = nextMessageId++;

        messages.add(
                new ConversationMessage(
                        id,
                        role,
                        safeText,
                        System.currentTimeMillis()
                )
        );

        while (messages.size() > maxMessages) {
            messages.remove(0);
        }

        return id;
    }

    public synchronized List<ConversationMessage> getMessages() {
        return Collections.unmodifiableList(
                new ArrayList<ConversationMessage>(messages)
        );
    }

    public synchronized ConversationMessage getMessageById(
            long id
    ) {
        for (ConversationMessage message : messages) {
            if (message.getId() == id) {
                return message;
            }
        }

        return null;
    }

    public synchronized ConversationMessage getPreviousMessage() {
        if (messages.size() < 2) {
            return null;
        }

        return messages.get(messages.size() - 2);
    }

    public synchronized ConversationMessage getLastMessage() {
        if (messages.isEmpty()) {
            return null;
        }

        return messages.get(messages.size() - 1);
    }

    public synchronized ConversationMessage getLastUserMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ConversationMessage message = messages.get(i);

            if (message.getRole() == MessageRole.USER) {
                return message;
            }
        }

        return null;
    }

    public synchronized ConversationMessage getLastAssistantMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ConversationMessage message = messages.get(i);

            if (message.getRole() == MessageRole.ASSISTANT) {
                return message;
            }
        }

        return null;
    }

    /**
     * Analisa uma expressao contextual e tenta encontrar
     * uma mensagem correspondente no historico temporario.
     */
    public synchronized ReferenceResult resolveReference(
            String expression
    ) {
        if (expression == null || expression.trim().isEmpty()) {
            return referenceResult(
                    ReferenceStatus.NOT_FOUND,
                    ReferenceType.UNKNOWN,
                    expression,
                    "",
                    -1L,
                    "A expressao esta vazia."
            );
        }

        if (messages.isEmpty()) {
            return referenceResult(
                    ReferenceStatus.NO_CONTEXT,
                    ReferenceType.UNKNOWN,
                    expression,
                    "",
                    -1L,
                    "Ainda nao existe historico nesta instancia."
            );
        }

        String normalized = normalize(expression);
        ReferenceMatch match = detectReference(normalized);

        if (match == null) {
            return referenceResult(
                    ReferenceStatus.NOT_FOUND,
                    ReferenceType.UNKNOWN,
                    expression,
                    "",
                    -1L,
                    "Nao foi identificada uma referencia conhecida."
            );
        }

        ConversationMessage target = null;

        switch (match.type) {
            case PREVIOUS_MESSAGE:
                target = getPreviousMessage();
                break;

            case LAST_ANSWER:
                target = getLastAssistantMessage();
                break;

            case LAST_QUESTION:
                target = getLastQuestionMessage();
                break;

            case LAST_TOPIC:
                target = getLastUserMessage();
                if (target == null) {
                    target = getLastMessage();
                }
                break;

            case EXPLICIT_REFERENCE:
                target = findExplicitReference(match.expression);
                break;

            case PRONOUN:
            case DEMONSTRATIVE:
                target = findRecentReferableMessage();

                if (target == null) {
                    return referenceResult(
                            ReferenceStatus.NOT_FOUND,
                            match.type,
                            expression,
                            "",
                            -1L,
                            "Nao foi encontrado um referente claro."
                    );
                }

                if (hasMultiplePossibleReferents(normalized)) {
                    return referenceResult(
                            ReferenceStatus.AMBIGUOUS,
                            match.type,
                            expression,
                            "",
                            -1L,
                            "Existem varias mensagens possiveis. "
                                    + "Informe o assunto ou a mensagem."
                    );
                }
                break;

            default:
                break;
        }

        if (target == null) {
            return referenceResult(
                    ReferenceStatus.NOT_FOUND,
                    match.type,
                    expression,
                    "",
                    -1L,
                    "Nao encontrei uma mensagem correspondente."
            );
        }

        return referenceResult(
                ReferenceStatus.RESOLVED,
                match.type,
                expression,
                target.getText(),
                target.getId(),
                "Referencia associada a uma mensagem existente "
                        + "no historico temporario."
        );
    }

    /**
     * Busca mensagens pelo texto ou por palavras do assunto.
     */
    public synchronized List<ConversationMessage> searchMessages(
            String query,
            int limit
    ) {
        if (query == null || query.trim().isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }

        String normalizedQuery = normalize(query);

        List<ConversationMessage> result =
                new ArrayList<ConversationMessage>();

        for (int i = messages.size() - 1; i >= 0; i--) {
            ConversationMessage message = messages.get(i);
            String normalizedMessage = normalize(message.getText());

            if (normalizedMessage.contains(normalizedQuery)
                    || sharesUsefulWord(
                            normalizedQuery,
                            normalizedMessage
                    )) {
                result.add(message);

                if (result.size() >= limit) {
                    break;
                }
            }
        }

        return Collections.unmodifiableList(result);
    }

    public synchronized void clearTemporaryHistory() {
        messages.clear();
    }

    public synchronized int getMessageCount() {
        return messages.size();
    }

    private List<SentenceScore> scoreSentences(
            List<String> sentences
    ) {
        List<SentenceScore> result =
                new ArrayList<SentenceScore>();

        Map<String, Integer> frequency =
                new LinkedHashMap<String, Integer>();

        for (String sentence : sentences) {
            for (String word : words(sentence)) {
                if (isUsefulWord(word)) {
                    Integer count = frequency.get(word);
                    frequency.put(
                            word,
                            count == null ? 1 : count + 1
                    );
                }
            }
        }

        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            List<String> sentenceWords = words(sentence);

            double score = 0.0;
            int usefulWords = 0;

            for (String word : sentenceWords) {
                if (isUsefulWord(word)) {
                    usefulWords++;

                    Integer count = frequency.get(word);

                    if (count != null) {
                        score += Math.min(count, 4);
                    }
                }
            }

            if (usefulWords > 0) {
                score = score / usefulWords;
            }

            SummaryCategory category =
                    classifySentence(sentence);

            switch (category) {
                case QUESTION:
                case DECISION:
                case GOAL:
                case PROBLEM:
                case TASK:
                case IMPORTANT:
                    score += 1.5;
                    break;

                default:
                    break;
            }

            if (containsReferenceLanguage(sentence)) {
                score += 0.4;
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

    private ReferenceMatch detectReference(String text) {

        if (containsAny(
                text,
                "ultima resposta",
                "ultima mensagem do jarvis",
                "resposta anterior",
                "o que voce respondeu",
                "o que vc respondeu"
        )) {
            return new ReferenceMatch(
                    ReferenceType.LAST_ANSWER,
                    text
            );
        }

        if (containsAny(
                text,
                "ultima pergunta",
                "pergunta anterior",
                "o que eu perguntei"
        )) {
            return new ReferenceMatch(
                    ReferenceType.LAST_QUESTION,
                    text
            );
        }

        if (containsAny(
                text,
                "mensagem anterior",
                "mensagem anterior a essa",
                "o que foi dito antes",
                "a conversa anterior",
                "o anterior",
                "a anterior",
                "a mensagem passada",
                "a mensagem anterior"
        )) {
            return new ReferenceMatch(
                    ReferenceType.PREVIOUS_MESSAGE,
                    text
            );
        }

        if (containsAny(
                text,
                "sobre o que estavamos falando",
                "qual era o assunto",
                "nosso ultimo assunto",
                "o ultimo assunto",
                "o tema anterior"
        )) {
            return new ReferenceMatch(
                    ReferenceType.LAST_TOPIC,
                    text
            );
        }

        if (text.startsWith("aquele ")
                || text.startsWith("aquela ")
                || text.startsWith("aqueles ")
                || text.startsWith("aquelas ")
                || text.startsWith("esse ")
                || text.startsWith("essa ")
                || text.startsWith("esses ")
                || text.startsWith("essas ")
                || text.startsWith("o que voce disse sobre ")
                || text.startsWith("o que voce falou sobre ")) {
            return new ReferenceMatch(
                    ReferenceType.EXPLICIT_REFERENCE,
                    extractReferenceTopic(text)
            );
        }

        if (containsAny(
                text,
                " isso ",
                " aquilo ",
                " ele ",
                " ela ",
                " eles ",
                " elas ",
                " esse ",
                " essa ",
                " aquele ",
                " aquela "
        )) {
            return new ReferenceMatch(
                    ReferenceType.PRONOUN,
                    text
            );
        }

        if (containsAny(
                text,
                "isso",
                "aquilo",
                "aquele",
                "aquela",
                "esses",
                "essas"
        )) {
            return new ReferenceMatch(
                    ReferenceType.DEMONSTRATIVE,
                    text
            );
        }

        return null;
    }

    private ConversationMessage findExplicitReference(
            String topic
    ) {
        if (topic == null || topic.trim().isEmpty()) {
            return null;
        }

        List<ConversationMessage> matches =
                searchMessages(topic, 2);

        if (matches.isEmpty()) {
            return null;
        }

        if (matches.size() > 1
                && !normalize(matches.get(0).getText()).contains(
                        normalize(topic))) {
            return null;
        }

        return matches.get(0);
    }

    private ConversationMessage findRecentReferableMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ConversationMessage message = messages.get(i);

            if (message.getRole() == MessageRole.SYSTEM) {
                continue;
            }

            if (!message.getText().trim().isEmpty()) {
                return message;
            }
        }

        return null;
    }

    private ConversationMessage getLastQuestionMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ConversationMessage message = messages.get(i);

            if (message.getText().contains("?")) {
                return message;
            }
        }

        return null;
    }

    private boolean hasMultiplePossibleReferents(String text) {
        int candidates = 0;

        for (ConversationMessage message : messages) {
            if (message.getRole() != MessageRole.SYSTEM
                    && sharesUsefulWord(
                            text,
                            normalize(message.getText())
                    )) {
                candidates++;

                if (candidates > 1) {
                    return true;
                }
            }
        }

        return false;
    }

    private static String extractReferenceTopic(String text) {
        String[] prefixes = {
                "o que voce disse sobre ",
                "o que voce falou sobre ",
                "aquele ",
                "aquela ",
                "aqueles ",
                "aquelas ",
                "esse ",
                "essa ",
                "esses ",
                "essas "
        };

        for (String prefix : prefixes) {
            if (text.startsWith(prefix)) {
                return text.substring(prefix.length()).trim();
            }
        }

        return text;
    }

    private static boolean containsReferenceLanguage(
            String sentence
    ) {
        String text = normalize(sentence);

        return containsAny(
                text,
                "aquele",
                "aquela",
                "aqueles",
                "aquelas",
                "isso",
                "isto",
                "aquilo",
                "esse",
                "essa",
                "ele",
                "ela",
                "anterior",
                "ultima resposta",
                "mensagem anterior"
        );
    }

    private static boolean sharesUsefulWord(
            String first,
            String second
    ) {
        List<String> firstWords = words(first);
        List<String> secondWords = words(second);

        for (String word : firstWords) {
            if (isUsefulWord(word) && secondWords.contains(word)) {
                return true;
            }
        }

        return false;
    }

    private static List<String> words(String text) {
        List<String> result = new ArrayList<String>();

        String normalized = normalize(text);
        String[] parts = normalized.split("[^a-z0-9]+");

        for (String part : parts) {
            if (!part.isEmpty()) {
                result.add(part);
            }
        }

        return result;
    }

    private static boolean isUsefulWord(String word) {
        return word.length() >= 3 && !isStopWord(word);
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
            case "esses":
            case "essas":
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
            case "aquela":
            case "aquele":
            case "aquelas":
            case "aqueles":
            case "aquilo":
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
            case "vos":
            case "lhe":
            case "lhes":
            case "ja":
            case "nao":
            case "sim":
            case "voce":
            case "voces":
            case "vc":
                return true;

            default:
                return false;
        }
    }

    private static boolean startsWithAny(
            String text,
            String... prefixes
    ) {
        for (String prefix : prefixes) {
            if (text.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsAny(
            String text,
            String... terms
    ) {
        String normalizedText = normalize(text);

        for (String term : terms) {
            if (normalizedText.contains(normalize(term))) {
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

    private SummaryResult createSummaryResult(
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

    private ReferenceResult referenceResult(
            ReferenceStatus status,
            ReferenceType type,
            String expression,
            String resolvedText,
            long messageId,
            String explanation
    ) {
        return new ReferenceResult(
                status,
                type,
                expression == null ? "" : expression,
                resolvedText,
                messageId,
                explanation
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

    public int getMaxMessages() {
        return maxMessages;
    }
}
