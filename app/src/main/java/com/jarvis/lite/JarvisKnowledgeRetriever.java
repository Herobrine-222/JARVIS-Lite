
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

/**
 * Recupera conhecimentos relevantes para uma consulta.
 *
 * Funciona localmente, sem internet e sem executar comandos.
 * Nesta primeira versão, os registros ficam apenas em memória.
 *
 * A integração com a base de conhecimento persistente do
 * JARVIS Lite será feita durante a etapa de integração.
 */
public final class JarvisKnowledgeRetriever {

    public static final int DEFAULT_MAX_DOCUMENTS = 500;
    public static final int MAX_DOCUMENTS_LIMIT = 5000;
    public static final int MAX_TEXT_LENGTH = 4000;
    public static final int MAX_QUERY_LENGTH = 1000;
    public static final int MAX_RESULTS_LIMIT = 50;

    public enum ResultCode {
        SUCCESS,
        INVALID_INPUT,
        DOCUMENT_NOT_FOUND,
        DOCUMENT_LIMIT_REACHED,
        DUPLICATE_ID
    }

    public static final class KnowledgeDocument {

        private final String id;
        private final String title;
        private final String content;
        private final Set<String> tags;
        private final long createdAt;

        private KnowledgeDocument(
                String id,
                String title,
                String content,
                Set<String> tags
        ) {
            this.id = id;
            this.title = title;
            this.content = content;

            this.tags = Collections.unmodifiableSet(
                    new LinkedHashSet<String>(tags)
            );

            this.createdAt = System.currentTimeMillis();
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }

        public Set<String> getTags() {
            return tags;
        }

        public long getCreatedAt() {
            return createdAt;
        }
    }

    public static final class SearchResult {

        private final KnowledgeDocument document;
        private final double score;
        private final List<String> matchedTerms;

        private SearchResult(
                KnowledgeDocument document,
                double score,
                List<String> matchedTerms
        ) {
            this.document = document;
            this.score = score;

            this.matchedTerms = Collections.unmodifiableList(
                    new ArrayList<String>(matchedTerms)
            );
        }

        public KnowledgeDocument getDocument() {
            return document;
        }

        public double getScore() {
            return score;
        }

        public List<String> getMatchedTerms() {
            return matchedTerms;
        }
    }

    private final int maxDocuments;

    private final Map<String, KnowledgeDocument> documents =
            new LinkedHashMap<String, KnowledgeDocument>();

    public JarvisKnowledgeRetriever() {
        this(DEFAULT_MAX_DOCUMENTS);
    }

    public JarvisKnowledgeRetriever(int maxDocuments) {
        this.maxDocuments = Math.max(
                1,
                Math.min(maxDocuments, MAX_DOCUMENTS_LIMIT)
        );
    }

    /**
     * Registra um documento na base temporária local.
     *
     * O identificador deve ser único.
     */
    public synchronized ResultCode addDocument(
            String id,
            String title,
            String content,
            Set<String> tags
    ) {
        String safeId = normalize(id);
        String safeTitle = normalize(title);
        String safeContent = normalize(content);

        if (!isValidId(safeId)
                || !isValidTitle(safeTitle)
                || safeContent.isEmpty()
                || safeContent.length() > MAX_TEXT_LENGTH) {
            return ResultCode.INVALID_INPUT;
        }

        if (documents.containsKey(safeId)) {
            return ResultCode.DUPLICATE_ID;
        }

        if (documents.size() >= maxDocuments) {
            return ResultCode.DOCUMENT_LIMIT_REACHED;
        }

        Set<String> safeTags = normalizeTags(tags);

        KnowledgeDocument document = new KnowledgeDocument(
                safeId,
                safeTitle,
                safeContent,
                safeTags
        );

        documents.put(safeId, document);

        return ResultCode.SUCCESS;
    }

    /**
     * Pesquisa documentos usando correspondência textual.
     *
     * O resultado é uma estimativa de relevância, não uma
     * garantia de que o conteúdo esteja correto.
     */
    public synchronized List<SearchResult> search(
            String query,
            int maxResults
    ) {
        String safeQuery = normalize(query);

        if (safeQuery.isEmpty()
                || safeQuery.length() > MAX_QUERY_LENGTH
                || maxResults <= 0) {
            return Collections.emptyList();
        }

        int resultLimit = Math.min(
                maxResults,
                MAX_RESULTS_LIMIT
        );

        Set<String> queryTerms = tokenize(safeQuery);

        if (queryTerms.isEmpty()) {
            return Collections.emptyList();
        }

        List<SearchResult> results =
                new ArrayList<SearchResult>();

        for (KnowledgeDocument document : documents.values()) {
            SearchResult result = scoreDocument(
                    document,
                    queryTerms
            );

            if (result != null) {
                results.add(result);
            }
        }

        Collections.sort(
                results,
                new Comparator<SearchResult>() {
                    @Override
                    public int compare(
                            SearchResult first,
                            SearchResult second
                    ) {
                        int scoreComparison = Double.compare(
                                second.getScore(),
                                first.getScore()
                        );

                        if (scoreComparison != 0) {
                            return scoreComparison;
                        }

                        return first.getDocument()
                                .getId()
                                .compareTo(
                                        second.getDocument().getId()
                                );
                    }
                }
        );

        if (results.size() > resultLimit) {
            results = new ArrayList<SearchResult>(
                    results.subList(0, resultLimit)
            );
        }

        return Collections.unmodifiableList(results);
    }

    /**
     * Pesquisa com um limite padrão de cinco resultados.
     */
    public synchronized List<SearchResult> search(String query) {
        return search(query, 5);
    }

    private SearchResult scoreDocument(
            KnowledgeDocument document,
            Set<String> queryTerms
    ) {
        Set<String> titleTerms = tokenize(document.getTitle());
        Set<String> contentTerms = tokenize(document.getContent());
        Set<String> tagTerms = tokenize(
                joinTags(document.getTags())
        );

        List<String> matchedTerms = new ArrayList<String>();

        double score = 0.0;

        for (String term : queryTerms) {
            boolean matched = false;

            if (titleTerms.contains(term)) {
                score += 3.0;
                matched = true;
            }

            if (tagTerms.contains(term)) {
                score += 2.0;
                matched = true;
            }

            if (contentTerms.contains(term)) {
                score += 1.0;
                matched = true;
            }

            if (matched) {
                matchedTerms.add(term);
            }
        }

        if (matchedTerms.isEmpty()) {
            return null;
        }

        double coverage = (double) matchedTerms.size()
                / (double) queryTerms.size();

        score = score * (0.5 + coverage * 0.5);

        return new SearchResult(
                document,
                score,
                matchedTerms
        );
    }

    /**
     * Busca um documento pelo identificador exato.
     */
    public synchronized KnowledgeDocument getDocument(String id) {
        if (id == null) {
            return null;
        }

        return documents.get(id.trim());
    }

    /**
     * Remove um documento da base temporária.
     */
    public synchronized ResultCode removeDocument(String id) {
        if (id == null || id.trim().isEmpty()) {
            return ResultCode.INVALID_INPUT;
        }

        KnowledgeDocument removed = documents.remove(id.trim());

        return removed == null
                ? ResultCode.DOCUMENT_NOT_FOUND
                : ResultCode.SUCCESS;
    }

    public synchronized List<KnowledgeDocument> getAllDocuments() {
        return Collections.unmodifiableList(
                new ArrayList<KnowledgeDocument>(
                        documents.values()
                )
        );
    }

    public synchronized int getDocumentCount() {
        return documents.size();
    }

    public int getMaxDocuments() {
        return maxDocuments;
    }

    /**
     * Limpa somente os registros desta instância.
     * Não apaga arquivos nem a memória persistente do app.
     */
    public synchronized void clear() {
        documents.clear();
    }

    private static Set<String> normalizeTags(Set<String> tags) {
        Set<String> result = new LinkedHashSet<String>();

        if (tags == null) {
            return result;
        }

        for (String tag : tags) {
            String safeTag = normalize(tag);

            if (!safeTag.isEmpty()
                    && safeTag.length() <= 100) {
                result.add(safeTag);
            }

            if (result.size() >= 50) {
                break;
            }
        }

        return result;
    }

    private static Set<String> tokenize(String text) {
        Set<String> terms = new LinkedHashSet<String>();

        String normalized = normalizeForSearch(text);

        if (normalized.isEmpty()) {
            return terms;
        }

        String[] parts = normalized.split("[^\\p{L}\\p{N}]+");

        for (String part : parts) {
            if (part.length() >= 2) {
                terms.add(part);
            }
        }

        return terms;
    }

    private static String normalizeForSearch(String text) {
        if (text == null) {
            return "";
        }

        String normalized = Normalizer.normalize(
                text.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );

        return normalized.replaceAll("\\p{M}+", "").trim();
    }

    private static String joinTags(Set<String> tags) {
        StringBuilder builder = new StringBuilder();

        for (String tag : tags) {
            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(tag);
        }

        return builder.toString();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isValidId(String value) {
        return value != null
                && !value.isEmpty()
                && value.length() <= 100;
    }

    private static boolean isValidTitle(String value) {
        return value != null
                && !value.isEmpty()
                && value.length() <= 300;
    }
}
