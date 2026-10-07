package com.jarvis.lite;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Memória persistente local do JARVIS.
 *
 * Mantém compatibilidade com a API existente,
 * acrescentando:
 *
 * - relevância por palavras;
 * - categoria;
 * - recência;
 * - importância;
 * - frequência de acesso;
 * - deduplicação;
 * - contexto ordenado por relevância;
 * - memória chave/valor;
 * - importação/exportação JSON;
 * - persistência segura do contador de acesso.
 *
 * Tudo funciona localmente, sem Internet.
 */
public class JarvisMemory {

    private static final String PREFS =
            "jarvis_memory";

    private static final String KEY_ITEMS =
            "items";

    private static final int MAX_ITEMS =
            150;

    private static final int MAX_TEXT =
            500;

    private static final int MAX_CATEGORY =
            80;

    private static final int MAX_TAGS =
            12;

    private static final int MAX_CONTEXT_SCAN =
            150;

    private static final long DAY_MS =
            24L * 60L * 60L * 1000L;

    private final SharedPreferences prefs;

    public JarvisMemory(Context context) {

        prefs =
                context
                        .getApplicationContext()
                        .getSharedPreferences(
                                PREFS,
                                Context.MODE_PRIVATE
                        );
    }

    // ============================================================
    // MEMÓRIA PRINCIPAL
    // ============================================================

    public synchronized boolean remember(
            String text) {

        return remember(
                text,
                "geral"
        );
    }

    public synchronized boolean remember(
            String text,
            String category) {

        return rememberInternal(
                text,
                category,
                50,
                null
        );
    }

    /**
     * Guarda uma memória com importância
     * entre 0 e 100.
     */
    public synchronized boolean rememberImportant(
            String text,
            String category,
            int importance) {

        return rememberInternal(
                text,
                category,
                clamp(
                        importance,
                        0,
                        100
                ),
                null
        );
    }

    private boolean rememberInternal(
            String text,
            String category,
            int importance,
            String[] tags) {

        if (text == null) {
            return false;
        }

        String value =
                text.trim();

        if (value.isEmpty()) {
            return false;
        }

        if (value.length() > MAX_TEXT) {
            value =
                    value.substring(
                            0,
                            MAX_TEXT
                    );
        }

        String categoria =
                sanitizeCategory(
                        category
                );

        JSONArray items =
                readItems();

        long now =
                System.currentTimeMillis();

        try {

            /*
             * Se a memória já existir,
             * atualiza em vez de criar duplicata.
             */
            for (
                    int i = 0;
                    i < items.length();
                    i++
            ) {

                JSONObject item =
                        items.optJSONObject(i);

                if (item == null) {
                    continue;
                }

                String existing =
                        item.optString(
                                "text",
                                ""
                        ).trim();

                String existingCategory =
                        item.optString(
                                "category",
                                "geral"
                        );

                if (
                        normalize(existing)
                                .equals(
                                        normalize(value)
                                )
                                &&
                        normalize(existingCategory)
                                .equals(
                                        normalize(categoria)
                                )
                ) {

                    int acessos =
                            item.optInt(
                                    "accessCount",
                                    0
                            ) + 1;

                    item.put(
                            "text",
                            value
                    );

                    item.put(
                            "category",
                            categoria
                    );

                    item.put(
                            "importance",
                            Math.max(
                                    item.optInt(
                                            "importance",
                                            50
                                    ),
                                    importance
                            )
                    );

                    item.put(
                            "updatedTime",
                            now
                    );

                    item.put(
                            "lastAccessTime",
                            now
                    );

                    item.put(
                            "accessCount",
                            acessos
                    );

                    item.put(
                            "tags",
                            tagsToJson(
                                    tags,
                                    value
                            )
                    );

                    saveItems(items);

                    return true;
                }
            }

            /*
             * Nova memória.
             */
            JSONObject novo =
                    new JSONObject();

            novo.put(
                    "text",
                    value
            );

            novo.put(
                    "category",
                    categoria
            );

            novo.put(
                    "time",
                    now
            );

            novo.put(
                    "updatedTime",
                    now
            );

            novo.put(
                    "lastAccessTime",
                    0L
            );

            novo.put(
                    "importance",
                    clamp(
                            importance,
                            0,
                            100
                    )
            );

            novo.put(
                    "accessCount",
                    0
            );

            novo.put(
                    "tags",
                    tagsToJson(
                            tags,
                            value
                    )
            );

            items.put(
                    novo
            );

            saveItems(
                    limitItems(
                            items
                    )
            );

            return true;

        } catch (Exception ignored) {

            return false;
        }
    }

    // ============================================================
    // MEMÓRIA CHAVE / VALOR
    // ============================================================

    public synchronized boolean rememberKeyValue(
            String key,
            String value) {

        return rememberKeyValue(
                key,
                value,
                "preferencias"
        );
    }

    public synchronized boolean rememberKeyValue(
            String key,
            String value,
            String category) {

        if (
                key == null
                        || value == null
        ) {
            return false;
        }

        String chave =
                key.trim();

        String valor =
                value.trim();

        if (
                chave.isEmpty()
                        || valor.isEmpty()
        ) {
            return false;
        }

        return rememberImportant(
                chave
                        + ": "
                        + valor,
                category,
                75
        );
    }

    // ============================================================
    // ATUALIZAR MEMÓRIA
    // ============================================================

    public synchronized boolean update(
            String query,
            String newText) {

        return update(
                query,
                newText,
                "geral"
        );
    }

    public synchronized boolean update(
            String query,
            String newText,
            String category) {

        if (
                query == null
                        || newText == null
        ) {
            return false;
        }

        String busca =
                query.trim();

        String novoTexto =
                newText.trim();

        if (
                busca.isEmpty()
                        || novoTexto.isEmpty()
        ) {
            return false;
        }

        if (novoTexto.length() > MAX_TEXT) {

            novoTexto =
                    novoTexto.substring(
                            0,
                            MAX_TEXT
                    );
        }

        JSONArray items =
                readItems();

        String normalizada =
                normalize(
                        busca
                );

        JSONObject melhor =
                null;

        int melhorScore =
                0;

        try {

            for (
                    int i = 0;
                    i < items.length();
                    i++
            ) {

                JSONObject item =
                        items.optJSONObject(i);

                if (item == null) {
                    continue;
                }

                String texto =
                        item.optString(
                                "text",
                                ""
                        );

                int score =
                        scoreMemory(
                                normalizada,
                                item
                        );

                if (score > melhorScore) {

                    melhorScore =
                            score;

                    melhor =
                            item;
                }

                if (
                        normalize(texto)
                                .equals(
                                        normalizada
                                )
                ) {

                    melhor =
                            item;

                    melhorScore =
                            10000;

                    break;
                }
            }

            if (
                    melhor == null
                            || melhorScore < 20
            ) {
                return false;
            }

            melhor.put(
                    "text",
                    novoTexto
            );

            melhor.put(
                    "category",
                    sanitizeCategory(
                            category
                    )
            );

            melhor.put(
                    "updatedTime",
                    System.currentTimeMillis()
            );

            melhor.put(
                    "tags",
                    tagsToJson(
                            null,
                            novoTexto
                    )
            );

            saveItems(
                    items
            );

            return true;

        } catch (Exception ignored) {

            return false;
        }
    }

    // ============================================================
    // TODAS AS MEMÓRIAS
    // ============================================================

    public synchronized List<String> getAll() {

        ArrayList<String> resultado =
                new ArrayList<>();

        JSONArray items =
                readItems();

        for (
                int i = items.length() - 1;
                i >= 0;
                i--
        ) {

            JSONObject item =
                    items.optJSONObject(i);

            if (item == null) {
                continue;
            }

            String texto =
                    item.optString(
                            "text",
                            ""
                    );

            if (!texto.isEmpty()) {
                resultado.add(
                        texto
                );
            }
        }

        return resultado;
    }

    // ============================================================
    // MEMÓRIA POR CATEGORIA
    // ============================================================

    public synchronized List<String> getByCategory(
            String category) {

        ArrayList<String> resultado =
                new ArrayList<>();

        if (
                category == null
                        || category.trim().isEmpty()
        ) {
            return resultado;
        }

        String categoria =
                normalize(
                        category
                );

        JSONArray items =
                readItems();

        for (
                int i = items.length() - 1;
                i >= 0;
                i--
        ) {

            JSONObject item =
                    items.optJSONObject(i);

            if (item == null) {
                continue;
            }

            String categoriaItem =
                    item.optString(
                            "category",
                            "geral"
                    );

            if (
                    !normalize(categoriaItem)
                            .equals(categoria)
            ) {
                continue;
            }

            String texto =
                    item.optString(
                            "text",
                            ""
                    );

            if (!texto.isEmpty()) {
                resultado.add(
                        texto
                );
            }
        }

        return resultado;
    }

    // ============================================================
    // FORMATAÇÃO
    // ============================================================

    public synchronized String formatForDisplay() {

        List<String> todas =
                getAll();

        if (todas.isEmpty()) {
            return "Nenhuma memória salva.";
        }

        StringBuilder resultado =
                new StringBuilder();

        for (
                int i = 0;
                i < todas.size();
                i++
        ) {

            resultado
                    .append(i + 1)
                    .append(". ")
                    .append(
                            todas.get(i)
                    );

            if (
                    i + 1
                            < todas.size()
            ) {
                resultado.append(
                        "\n"
                );
            }
        }

        return resultado.toString();
    }

    public synchronized String
    formatDetailedForDisplay() {

        JSONArray items =
                readItems();

        if (items.length() == 0) {
            return "Nenhuma memória salva.";
        }

        StringBuilder resultado =
                new StringBuilder();

        int numero =
                1;

        for (
                int i = items.length() - 1;
                i >= 0;
                i--
        ) {

            JSONObject item =
                    items.optJSONObject(i);

            if (item == null) {
                continue;
            }

            String texto =
                    item.optString(
                            "text",
                            ""
                    );

            if (texto.isEmpty()) {
                continue;
            }

            String categoria =
                    item.optString(
                            "category",
                            "geral"
                    );

            int importancia =
                    item.optInt(
                            "importance",
                            50
                    );

            int acessos =
                    item.optInt(
                            "accessCount",
                            0
                    );

            resultado
                    .append(numero++)
                    .append(". ")
                    .append(texto)
                    .append(" [")
                    .append(categoria)
                    .append("]")
                    .append(" {importancia=")
                    .append(importancia)
                    .append(", acessos=")
                    .append(acessos)
                    .append("}");

            if (i > 0) {
                resultado.append(
                        "\n"
                );
            }
        }

        return resultado.toString();
    }

    // ============================================================
    // ESQUECER
    // ============================================================

    public synchronized int forget(
            String query) {

        if (
                query == null
                        || query.trim().isEmpty()
        ) {
            return 0;
        }

        String busca =
                normalize(
                        query
                );

        JSONArray old =
                readItems();

        JSONArray next =
                new JSONArray();

        int removidas =
                0;

        try {

            for (
                    int i = 0;
                    i < old.length();
                    i++
            ) {

                JSONObject item =
                        old.optJSONObject(i);

                if (item == null) {
                    continue;
                }

                String texto =
                        item.optString(
                                "text",
                                ""
                        );

                if (
                        !texto.isEmpty()
                                &&
                        normalize(texto)
                                .contains(busca)
                ) {

                    removidas++;

                } else {

                    next.put(
                            item
                    );
                }
            }

            saveItems(
                    next
            );

        } catch (Exception ignored) {

            return 0;
        }

        return removidas;
    }

    public synchronized int forgetCategory(
            String category) {

        if (
                category == null
                        || category.trim().isEmpty()
        ) {
            return 0;
        }

        String busca =
                normalize(
                        category
                );

        JSONArray old =
                readItems();

        JSONArray next =
                new JSONArray();

        int removidas =
                0;

        try {

            for (
                    int i = 0;
                    i < old.length();
                    i++
            ) {

                JSONObject item =
                        old.optJSONObject(i);

                if (item == null) {
                    continue;
                }

                String categoria =
                        item.optString(
                                "category",
                                "geral"
                        );

                if (
                        normalize(categoria)
                                .equals(busca)
                ) {

                    removidas++;

                } else {

                    next.put(
                            item
                    );
                }
            }

            saveItems(
                    next
            );

        } catch (Exception ignored) {

            return 0;
        }

        return removidas;
    }

    public synchronized void clear() {

        prefs.edit()
                .remove(KEY_ITEMS)
                .apply();
    }

    // ============================================================
    // CONTEXTO RELEVANTE
    // ============================================================

    public synchronized String relevantContext(
            String query,
            int maxItems) {

        if (
                query == null
                        || maxItems <= 0
        ) {
            return "";
        }

        String busca =
                normalize(query);

        if (busca.isEmpty()) {
            return "";
        }

        List<ScoredMemory> ranking =
                rank(
                        query,
                        maxItems
                );

        StringBuilder resultado =
                new StringBuilder();

        int quantidade =
                0;

        for (
                ScoredMemory memoria
                        : ranking
        ) {

            if (memoria.score < 8) {
                continue;
            }

            if (quantidade > 0) {
                resultado.append(
                        "\n"
                );
            }

            resultado.append(
                    memoria.text
            );

            quantidade++;

            if (
                    quantidade
                            >= maxItems
            ) {
                break;
            }
        }

        return resultado.toString();
    }

    // ============================================================
    // BUSCA
    // ============================================================

    public synchronized List<String> search(
            String query,
            int maxItems) {

        ArrayList<String> resultado =
                new ArrayList<>();

        if (
                query == null
                        || query.trim().isEmpty()
                        || maxItems <= 0
        ) {
            return resultado;
        }

        List<ScoredMemory> ranking =
                rank(
                        query,
                        maxItems
                );

        for (
                ScoredMemory memoria
                        : ranking
        ) {

            if (memoria.score < 8) {
                continue;
            }

            resultado.add(
                    memoria.text
            );

            if (
                    resultado.size()
                            >= maxItems
            ) {
                break;
            }
        }

        return resultado;
    }

    public synchronized String getBestMatchJson(
            String query) {

        List<ScoredMemory> ranking =
                rank(
                        query,
                        1
                );

        if (ranking.isEmpty()) {
            return "";
        }

        return ranking
                .get(0)
                .json
                .toString();
    }

    /**
     * Aumenta o contador de uso da melhor
     * memória correspondente.
     */
    public synchronized boolean touchBestMatch(
            String query) {

        List<ScoredMemory> ranking =
                rank(
                        query,
                        1
                );

        if (
                ranking.isEmpty()
                        || ranking.get(0).score < 8
        ) {
            return false;
        }

        try {

            JSONObject item =
                    ranking
                            .get(0)
                            .json;

            item.put(
                    "accessCount",
                    item.optInt(
                            "accessCount",
                            0
                    ) + 1
            );

            item.put(
                    "lastAccessTime",
                    System.currentTimeMillis()
            );

            saveRankedBack(
                    ranking.get(0)
            );

            return true;

        } catch (Exception ignored) {

            return false;
        }
    }

    // ============================================================
    // QUANTIDADE / EXISTÊNCIA
    // ============================================================

    public synchronized int size() {

        return readItems()
                .length();
    }

    public synchronized boolean contains(
            String query) {

        if (
                query == null
                        || query.trim().isEmpty()
        ) {
            return false;
        }

        List<ScoredMemory> ranking =
                rank(
                        query,
                        1
                );

        return
                !ranking.isEmpty()
                        && ranking
                        .get(0)
                        .score >= 8;
    }

    // ============================================================
    // ÚLTIMA MEMÓRIA
    // ============================================================

    public synchronized String getLast() {

        JSONArray items =
                readItems();

        if (items.length() == 0) {
            return "";
        }

        JSONObject item =
                items.optJSONObject(
                        items.length() - 1
                );

        return item == null
                ? ""
                : item.optString(
                        "text",
                        ""
                );
    }

    public synchronized long getLastTime() {

        JSONArray items =
                readItems();

        if (items.length() == 0) {
            return 0L;
        }

        JSONObject item =
                items.optJSONObject(
                        items.length() - 1
                );

        return item == null
                ? 0L
                : item.optLong(
                        "time",
                        0L
                );
    }

    // ============================================================
    // EXPORTAR / IMPORTAR
    // ============================================================

    public synchronized String exportJson() {

        return readItems()
                .toString();
    }

    public synchronized boolean importJson(
            String json) {

        if (
                json == null
                        || json.trim().isEmpty()
        ) {
            return false;
        }

        try {

            JSONArray origem =
                    new JSONArray(
                            json
                    );

            JSONArray limpa =
                    new JSONArray();

            for (
                    int i = 0;
                    i < origem.length()
                            && limpa.length()
                            < MAX_ITEMS;
                    i++
            ) {

                JSONObject original =
                        origem.optJSONObject(i);

                if (original == null) {
                    continue;
                }

                String texto =
                        original
                                .optString(
                                        "text",
                                        ""
                                )
                                .trim();

                if (texto.isEmpty()) {
                    continue;
                }

                if (
                        texto.length()
                                > MAX_TEXT
                ) {

                    texto =
                            texto.substring(
                                    0,
                                    MAX_TEXT
                            );
                }

                JSONObject item =
                        new JSONObject();

                item.put(
                        "text",
                        texto
                );

                item.put(
                        "category",
                        sanitizeCategory(
                                original.optString(
                                        "category",
                                        "geral"
                                )
                        )
                );

                long time =
                        original.optLong(
                                "time",
                                System.currentTimeMillis()
                        );

                item.put(
                        "time",
                        time
                );

                item.put(
                        "updatedTime",
                        original.optLong(
                                "updatedTime",
                                time
                        )
                );

                item.put(
                        "lastAccessTime",
                        Math.max(
                                0L,
                                original.optLong(
                                        "lastAccessTime",
                                        0L
                                )
                        )
                );

                item.put(
                        "importance",
                        clamp(
                                original.optInt(
                                        "importance",
                                        50
                                ),
                                0,
                                100
                        )
                );

                item.put(
                        "accessCount",
                        Math.max(
                                0,
                                original.optInt(
                                        "accessCount",
                                        0
                                )
                        )
                );

                JSONArray tags =
                        original.optJSONArray(
                                "tags"
                        );

                item.put(
                        "tags",
                        tags == null
                                ? tagsToJson(
                                        null,
                                        texto
                                )
                                : tags
                );

                limpa.put(
                        item
                );
            }

            saveItems(
                    limpa
            );

            return true;

        } catch (Exception ignored) {

            return false;
        }
    }

    public synchronized String getMemoryJson(
            int index) {

        JSONArray items =
                readItems();

        if (
                index < 0
                        || index >= items.length()
        ) {
            return "";
        }

        JSONObject item =
                items.optJSONObject(
                        index
                );

        return item == null
                ? ""
                : item.toString();
    }

    // ============================================================
    // RANKING
    // ============================================================

    private List<ScoredMemory> rank(
            String query,
            int maxItems) {

        ArrayList<ScoredMemory> ranking =
                new ArrayList<>();

        if (
                query == null
                        || query.trim().isEmpty()
                        || maxItems <= 0
        ) {
            return ranking;
        }

        String normalizedQuery =
                normalize(
                        query
                );

        String[] queryTokens =
                tokenize(
                        normalizedQuery
                );

        if (queryTokens.length == 0) {
            return ranking;
        }

        JSONArray items =
                readItems();

        int scanned =
                Math.min(
                        items.length(),
                        MAX_CONTEXT_SCAN
                );

        for (
                int i = 0;
                i < scanned;
                i++
        ) {

            JSONObject item =
                    items.optJSONObject(i);

            if (item == null) {
                continue;
            }

            String text =
                    item.optString(
                            "text",
                            ""
                    ).trim();

            if (text.isEmpty()) {
                continue;
            }

            int score =
                    scoreMemory(
                            normalizedQuery,
                            item
                    );

            if (score <= 0) {
                continue;
            }

            ranking.add(
                    new ScoredMemory(
                            item,
                            text,
                            score
                    )
            );
        }

        Collections.sort(
                ranking,
                new Comparator<ScoredMemory>() {

                    @Override
                    public int compare(
                            ScoredMemory a,
                            ScoredMemory b) {

                        int byScore =
                                Integer.compare(
                                        b.score,
                                        a.score
                                );

                        if (byScore != 0) {
                            return byScore;
                        }

                        long timeA =
                                a.json.optLong(
                                        "updatedTime",
                                        a.json.optLong(
                                                "time",
                                                0L
                                        )
                                );

                        long timeB =
                                b.json.optLong(
                                        "updatedTime",
                                        b.json.optLong(
                                                "time",
                                                0L
                                        )
                                );

                        return Long.compare(
                                timeB,
                                timeA
                        );
                    }
                }
        );

        if (
                ranking.size()
                        > maxItems
        ) {

            return new ArrayList<>(
                    ranking.subList(
                            0,
                            maxItems
                    )
            );
        }

        return ranking;
    }

    // ============================================================
    // CÁLCULO DE RELEVÂNCIA
    // ============================================================

    private int scoreMemory(
            String normalizedQuery,
            JSONObject item) {

        String text =
                normalize(
                        item.optString(
                                "text",
                                ""
                        )
                );

        if (
                text.isEmpty()
                        || normalizedQuery.isEmpty()
        ) {
            return 0;
        }

        int score =
                0;

        /*
         * Correspondência exata.
         */
        if (
                text.equals(
                        normalizedQuery
                )
        ) {

            score += 100;

        } else if (
                text.contains(
                        normalizedQuery
                )
        ) {

            score += 55;

        } else if (
                normalizedQuery.contains(
                        text
                )
                        && text.length() >= 4
        ) {

            score += 40;
        }

        String[] queryTokens =
                tokenize(
                        normalizedQuery
                );

        String[] textTokens =
                tokenize(
                        text
                );

        Set<String> textSet =
                new HashSet<>();

        Collections.addAll(
                textSet,
                textTokens
        );

        int matched =
                0;

        int meaningful =
                0;

        for (
                String token
                        : queryTokens
        ) {

            if (
                    isStopWord(
                            token
                    )
            ) {
                continue;
            }

            meaningful++;

            if (
                    textSet.contains(
                            token
                    )
            ) {

                matched++;

            } else if (
                    token.length() >= 5
                            && text.contains(token)
            ) {

                matched++;
            }
        }

        if (meaningful > 0) {

            score +=
                    (matched * 45)
                            / meaningful;
        }

        String category =
                normalize(
                        item.optString(
                                "category",
                                "geral"
                        )
                );

        boolean categoriaCoincide =
                !category.isEmpty()
                        &&
                normalizedQuery.contains(
                        category
                );

        if (categoriaCoincide) {
            score += 18;
        }

        /*
         * IMPORTANTE:
         *
         * Importância e recência só entram
         * depois de existir alguma correspondência.
         *
         * Assim uma consulta genérica como
         * "você" não ganha uma memória
         * aleatoriamente só porque ela é recente.
         */
        if (score <= 0) {
            return 0;
        }

        int importance =
                clamp(
                        item.optInt(
                                "importance",
                                50
                        ),
                        0,
                        100
                );

        score +=
                importance / 10;

        long time =
                item.optLong(
                        "updatedTime",
                        item.optLong(
                                "time",
                                0L
                        )
                );

        long age =
                Math.max(
                        0L,
                        System.currentTimeMillis()
                                - time
                );

        if (
                age < DAY_MS
        ) {

            score += 12;

        } else if (
                age
                        < 7L * DAY_MS
        ) {

            score += 7;

        } else if (
                age
                        < 30L * DAY_MS
        ) {

            score += 3;
        }

        int accesses =
                Math.max(
                        0,
                        item.optInt(
                                "accessCount",
                                0
                        )
                );

        score +=
                Math.min(
                        10,
                        accesses
                );

        return score;
    }

    // ============================================================
    // PERSISTIR ACESSO DA MEMÓRIA
    // ============================================================

    private void saveRankedBack(
            ScoredMemory target) {

        if (
                target == null
                        || target.json == null
        ) {
            return;
        }

        JSONArray items =
                readItems();

        try {

            String targetText =
                    normalize(
                            target.text
                    );

            String targetCategory =
                    normalize(
                            target.json.optString(
                                    "category",
                                    "geral"
                            )
                    );

            for (
                    int i = 0;
                    i < items.length();
                    i++
            ) {

                JSONObject item =
                        items.optJSONObject(i);

                if (item == null) {
                    continue;
                }

                String itemText =
                        normalize(
                                item.optString(
                                        "text",
                                        ""
                                )
                        );

                String itemCategory =
                        normalize(
                                item.optString(
                                        "category",
                                        "geral"
                                )
                        );

                if (
                        !itemText.equals(
                                targetText
                        )
                        ||
                        !itemCategory.equals(
                                targetCategory
                        )
                ) {
                    continue;
                }

                /*
                 * O objeto de target pertence
                 * à árvore JSON usada pelo ranking.
                 *
                 * readItems() criou outra árvore.
                 *
                 * Portanto atualizamos apenas os
                 * campos necessários no objeto
                 * recém-lido e persistimos essa árvore.
                 */
                item.put(
                        "accessCount",
                        Math.max(
                                0,
                                target.json.optInt(
                                        "accessCount",
                                        0
                                )
                        )
                );

                item.put(
                        "lastAccessTime",
                        Math.max(
                                0L,
                                target.json.optLong(
                                        "lastAccessTime",
                                        0L
                                )
                        )
                );

                saveItems(
                        items
                );

                return;
            }

        } catch (Exception ignored) {

            /*
             * Uma falha ao registrar o acesso
             * não deve quebrar a memória inteira.
             */
        }
    }

    // ============================================================
    // LEITURA / GRAVAÇÃO
    // ============================================================

    private JSONArray readItems() {

        try {

            return new JSONArray(
                    prefs.getString(
                            KEY_ITEMS,
                            "[]"
                    )
            );

        } catch (Exception ignored) {

            return new JSONArray();
        }
    }

    private void saveItems(
            JSONArray items) {

        prefs.edit()
                .putString(
                        KEY_ITEMS,
                        items == null
                                ? "[]"
                                : items.toString()
                )
                .apply();
    }

    // ============================================================
    // LIMITE DE MEMÓRIAS
    // ============================================================

    private JSONArray limitItems(
            JSONArray items) {

        if (
                items == null
                        || items.length()
                        <= MAX_ITEMS
        ) {

            return items == null
                    ? new JSONArray()
                    : items;
        }

        /*
         * Primeiro preserva memórias mais
         * importantes, acessadas e recentes.
         */
        ArrayList<JSONObject> lista =
                new ArrayList<>();

        for (
                int i = 0;
                i < items.length();
                i++
        ) {

            JSONObject item =
                    items.optJSONObject(i);

            if (item != null) {
                lista.add(
                        item
                );
            }
        }

        Collections.sort(
                lista,
                new Comparator<JSONObject>() {

                    @Override
                    public int compare(
                            JSONObject a,
                            JSONObject b) {

                        int sa =
                                retentionScore(
                                        a
                                );

                        int sb =
                                retentionScore(
                                        b
                                );

                        return Integer.compare(
                                sb,
                                sa
                        );
                    }
                }
        );

        JSONArray reduzido =
                new JSONArray();

        int limite =
                Math.min(
                        MAX_ITEMS,
                        lista.size()
                );

        for (
                int i = 0;
                i < limite;
                i++
        ) {

            reduzido.put(
                    lista.get(i)
            );
        }

        /*
         * Reordena pela data original para
         * manter a memória legível.
         */
        ArrayList<JSONObject> finalList =
                new ArrayList<>();

        for (
                int i = 0;
                i < reduzido.length();
                i++
        ) {

            JSONObject item =
                    reduzido.optJSONObject(i);

            if (item != null) {
                finalList.add(
                        item
                );
            }
        }

        Collections.sort(
                finalList,
                new Comparator<JSONObject>() {

                    @Override
                    public int compare(
                            JSONObject a,
                            JSONObject b) {

                        return Long.compare(
                                a.optLong(
                                        "time",
                                        0L
                                ),
                                b.optLong(
                                        "time",
                                        0L
                                )
                        );
                    }
                }
        );

        JSONArray ordenado =
                new JSONArray();

        for (
                JSONObject item
                        : finalList
        ) {

            if (item != null) {
                ordenado.put(
                        item
                );
            }
        }

        return ordenado;
    }

    private int retentionScore(
            JSONObject item) {

        if (item == null) {
            return 0;
        }

        int importance =
                clamp(
                        item.optInt(
                                "importance",
                                50
                        ),
                        0,
                        100
                );

        int accesses =
                Math.min(
                        20,
                        Math.max(
                                0,
                                item.optInt(
                                        "accessCount",
                                        0
                                )
                        )
                );

        long time =
                item.optLong(
                        "updatedTime",
                        item.optLong(
                                "time",
                                0L
                        )
                );

        long age =
                Math.max(
                        0L,
                        System.currentTimeMillis()
                                - time
                );

        int recency;

        if (
                age
                        < DAY_MS
        ) {

            recency = 20;

        } else if (
                age
                        < 7L * DAY_MS
        ) {

            recency = 12;

        } else if (
                age
                        < 30L * DAY_MS
        ) {

            recency = 6;

        } else {

            recency = 0;
        }

        return
                importance
                        + accesses
                        + recency;
    }

    // ============================================================
    // TAGS
    // ============================================================

    private JSONArray tagsToJson(
            String[] supplied,
            String text) {

        JSONArray tags =
                new JSONArray();

        Set<String> unique =
                new HashSet<>();

        if (supplied != null) {

            for (
                    String tag
                            : supplied
            ) {

                String n =
                        normalize(
                                tag
                        );

                if (
                        !n.isEmpty()
                                && unique.add(n)
                                && tags.length()
                                < MAX_TAGS
                ) {

                    tags.put(
                            n
                    );
                }
            }
        }

        for (
                String token
                        : tokenize(
                                normalize(text)
                        )
        ) {

            if (
                    isStopWord(token)
                            || token.length() < 4
            ) {
                continue;
            }

            if (
                    unique.add(
                            token
                    )
            ) {

                tags.put(
                        token
                );
            }

            if (
                    tags.length()
                            >= MAX_TAGS
            ) {
                break;
            }
        }

        return tags;
    }

    // ============================================================
    // TOKENIZAÇÃO
    // ============================================================

    private String[] tokenize(
            String text) {

        if (
                text == null
                        || text.trim().isEmpty()
        ) {

            return new String[0];
        }

        return text.split(
                "[^a-z0-9]+",
                -1
        );
    }

    // ============================================================
    // PALAVRAS DE PARADA
    // ============================================================

    private boolean isStopWord(
            String word) {

        switch (word) {

            case "a":
            case "o":
            case "as":
            case "os":
            case "um":
            case "uma":
            case "uns":
            case "umas":

            case "de":
            case "do":
            case "da":
            case "dos":
            case "das":

            case "e":
            case "ou":

            case "em":
            case "no":
            case "na":
            case "nos":
            case "nas":

            case "por":
            case "para":
            case "com":
            case "sem":

            case "que":
            case "se":

            case "eu":
            case "voce":
            case "voces":
            case "me":
            case "te":
            case "ele":
            case "ela":

            case "isso":
            case "isto":
            case "esse":
            case "essa":

            case "como":
            case "quando":
            case "onde":
            case "qual":
            case "quem":
            case "porque":

            case "pra":
            case "pro":

                return true;

            default:

                return false;
        }
    }

    // ============================================================
    // CATEGORIA
    // ============================================================

    private String sanitizeCategory(
            String category) {

        if (category == null) {
            return "geral";
        }

        String value =
                category.trim();

        if (value.isEmpty()) {
            return "geral";
        }

        if (
                value.length()
                        > MAX_CATEGORY
        ) {

            value =
                    value.substring(
                            0,
                            MAX_CATEGORY
                    );
        }

        return value;
    }

    // ============================================================
    // NORMALIZAÇÃO
    // ============================================================

    private String normalize(
            String text) {

        String value =
                text == null
                        ? ""
                        : text.toLowerCase(
                                Locale.ROOT
                        );

        value =
                Normalizer.normalize(
                        value,
                        Normalizer.Form.NFD
                );

        return value
                .replaceAll(
                        "\\p{M}+",
                        ""
                )
                .trim();
    }

    // ============================================================
    // CLAMP
    // ============================================================

    private int clamp(
            int value,
            int min,
            int max) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }

    // ============================================================
    // OBJETO DE RANKING
    // ============================================================

    private static class ScoredMemory {

        final JSONObject json;
        final String text;
        final int score;

        ScoredMemory(
                JSONObject json,
                String text,
                int score) {

            this.json = json;
            this.text = text;
            this.score = score;
        }
    }
}
