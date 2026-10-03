package com.jarvis.lite;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class JarvisMemory {

    private static final String PREFS = "jarvis_memory";
    private static final String KEY_ITEMS = "items";

    private static final int MAX_ITEMS = 100;
    private static final int MAX_TEXT = 500;
    private static final int MAX_CATEGORY = 80;

    private final SharedPreferences prefs;

    public JarvisMemory(Context context) {

        prefs = context.getApplicationContext()
                .getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                );
    }

    // ============================================================
    // MEMÓRIA PRINCIPAL
    // ============================================================

    public synchronized boolean remember(String text) {

        return remember(
                text,
                "geral"
        );
    }

    public synchronized boolean remember(
            String text,
            String category
    ) {

        if (text == null) {
            return false;
        }

        String value = text.trim();

        if (value.isEmpty()) {
            return false;
        }

        if (value.length() > MAX_TEXT) {
            value = value.substring(
                    0,
                    MAX_TEXT
            );
        }

        String categoria =
                sanitizeCategory(category);

        JSONArray old = readItems();
        JSONArray next = new JSONArray();

        try {

            // Remove memória exatamente igual.
            for (int i = 0; i < old.length(); i++) {

                JSONObject item =
                        old.optJSONObject(i);

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

                if (!existing.equalsIgnoreCase(value)
                        || !existingCategory.equalsIgnoreCase(categoria)) {

                    next.put(item);
                }
            }

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
                    System.currentTimeMillis()
            );

            next.put(novo);

            next = limitItems(next);

            saveItems(next);

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
            String value
    ) {

        return rememberKeyValue(
                key,
                value,
                "preferencias"
        );
    }

    public synchronized boolean rememberKeyValue(
            String key,
            String value,
            String category
    ) {

        if (key == null || value == null) {
            return false;
        }

        String chave =
                key.trim();

        String valor =
                value.trim();

        if (chave.isEmpty() || valor.isEmpty()) {
            return false;
        }

        return remember(
                chave + ": " + valor,
                category
        );
    }

    // ============================================================
    // ATUALIZAR UMA MEMÓRIA
    // ============================================================

    public synchronized boolean update(
            String query,
            String newText
    ) {

        return update(
                query,
                newText,
                "geral"
        );
    }

    public synchronized boolean update(
            String query,
            String newText,
            String category
    ) {

        if (query == null ||
                newText == null) {

            return false;
        }

        String busca =
                query.trim();

        String novoTexto =
                newText.trim();

        if (busca.isEmpty() ||
                novoTexto.isEmpty()) {

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

        String buscaNormalizada =
                normalize(busca);

        boolean atualizada = false;

        try {

            for (int i = 0; i < items.length(); i++) {

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

                if (normalize(texto)
                        .contains(buscaNormalizada)) {

                    item.put(
                            "text",
                            novoTexto
                    );

                    item.put(
                            "category",
                            sanitizeCategory(category)
                    );

                    item.put(
                            "time",
                            System.currentTimeMillis()
                    );

                    atualizada = true;

                    break;
                }
            }

            if (atualizada) {
                saveItems(items);
            }

        } catch (Exception ignored) {

            return false;
        }

        return atualizada;
    }

    // ============================================================
    // OBTER TODAS AS MEMÓRIAS
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
                resultado.add(texto);
            }
        }

        return resultado;
    }

    // ============================================================
    // OBTER MEMÓRIAS POR CATEGORIA
    // ============================================================

    public synchronized List<String> getByCategory(
            String category
    ) {

        ArrayList<String> resultado =
                new ArrayList<>();

        if (category == null ||
                category.trim().isEmpty()) {

            return resultado;
        }

        String categoria =
                normalize(category);

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

            if (!normalize(categoriaItem)
                    .equals(categoria)) {

                continue;
            }

            String texto =
                    item.optString(
                            "text",
                            ""
                    );

            if (!texto.isEmpty()) {
                resultado.add(texto);
            }
        }

        return resultado;
    }

    // ============================================================
    // FORMATAR PARA EXIBIÇÃO
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
                    .append(todas.get(i));

            if (i + 1 < todas.size()) {
                resultado.append("\n");
            }
        }

        return resultado.toString();
    }

    // ============================================================
    // FORMATAR COM CATEGORIA
    // ============================================================

    public synchronized String formatDetailedForDisplay() {

        JSONArray items =
                readItems();

        if (items.length() == 0) {
            return "Nenhuma memória salva.";
        }

        StringBuilder resultado =
                new StringBuilder();

        int numero = 1;

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

            resultado
                    .append(numero++)
                    .append(". ")
                    .append(texto)
                    .append(" [")
                    .append(categoria)
                    .append("]");

            if (i > 0) {
                resultado.append("\n");
            }
        }

        return resultado.toString();
    }

    // ============================================================
    // ESQUECER MEMÓRIA
    // ============================================================

    public synchronized int forget(
            String query
    ) {

        if (
                query == null ||
                query.trim().isEmpty()
        ) {
            return 0;
        }

        String busca =
                normalize(query);

        JSONArray old =
                readItems();

        JSONArray next =
                new JSONArray();

        int removidas = 0;

        try {

            for (int i = 0; i < old.length(); i++) {

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
                        !texto.isEmpty() &&
                        normalize(texto)
                                .contains(busca)
                ) {

                    removidas++;

                } else {

                    next.put(item);
                }
            }

            saveItems(next);

        } catch (Exception ignored) {

            return 0;
        }

        return removidas;
    }

    // ============================================================
    // ESQUECER UMA CATEGORIA
    // ============================================================

    public synchronized int forgetCategory(
            String category
    ) {

        if (category == null ||
                category.trim().isEmpty()) {

            return 0;
        }

        String busca =
                normalize(category);

        JSONArray old =
                readItems();

        JSONArray next =
                new JSONArray();

        int removidas = 0;

        try {

            for (int i = 0; i < old.length(); i++) {

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

                if (normalize(categoria)
                        .equals(busca)) {

                    removidas++;

                } else {

                    next.put(item);
                }
            }

            saveItems(next);

        } catch (Exception ignored) {

            return 0;
        }

        return removidas;
    }

    // ============================================================
    // LIMPAR TUDO
    // ============================================================

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
            int maxItems
    ) {

        if (
                query == null ||
                maxItems <= 0
        ) {
            return "";
        }

        String busca =
                normalize(query);

        if (busca.isEmpty()) {
            return "";
        }

        StringBuilder resultado =
                new StringBuilder();

        int quantidade = 0;

        for (String item : getAll()) {

            String normalizado =
                    normalize(item);

            if (
                    normalizado.contains(busca) ||
                    busca.contains(normalizado)
            ) {

                if (quantidade > 0) {
                    resultado.append("\n");
                }

                resultado.append(item);

                quantidade++;

                if (quantidade >= maxItems) {
                    break;
                }
            }
        }

        return resultado.toString();
    }

    // ============================================================
    // BUSCA POR PALAVRAS
    // ============================================================

    public synchronized List<String> search(
            String query,
            int maxItems
    ) {

        ArrayList<String> resultado =
                new ArrayList<>();

        if (
                query == null ||
                query.trim().isEmpty() ||
                maxItems <= 0
        ) {
            return resultado;
        }

        String busca =
                normalize(query);

        String[] palavras =
                busca.split("\\s+");

        for (String item : getAll()) {

            String normalizado =
                    normalize(item);

            boolean corresponde = true;

            for (String palavra : palavras) {

                if (palavra.isEmpty()) {
                    continue;
                }

                if (!normalizado.contains(palavra)) {
                    corresponde = false;
                    break;
                }
            }

            if (corresponde) {

                resultado.add(item);

                if (resultado.size() >= maxItems) {
                    break;
                }
            }
        }

        return resultado;
    }

    // ============================================================
    // QUANTIDADE DE MEMÓRIAS
    // ============================================================

    public synchronized int size() {

        return readItems().length();
    }

    // ============================================================
    // VERIFICAR SE EXISTE
    // ============================================================

    public synchronized boolean contains(
            String query
    ) {

        if (query == null ||
                query.trim().isEmpty()) {

            return false;
        }

        String busca =
                normalize(query);

        JSONArray items =
                readItems();

        for (int i = 0; i < items.length(); i++) {

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

            if (normalize(texto)
                    .contains(busca)) {

                return true;
            }
        }

        return false;
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

        if (item == null) {
            return "";
        }

        return item.optString(
                "text",
                ""
        );
    }

    // ============================================================
    // DATA DA ÚLTIMA MEMÓRIA
    // ============================================================

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

        if (item == null) {
            return 0L;
        }

        return item.optLong(
                "time",
                0L
        );
    }

    // ============================================================
    // EXPORTAR JSON
    // ============================================================

    public synchronized String exportJson() {

        return readItems().toString();
    }

    // ============================================================
    // IMPORTAR JSON
    // ============================================================

    public synchronized boolean importJson(
            String json
    ) {

        if (
                json == null ||
                json.trim().isEmpty()
        ) {
            return false;
        }

        try {

            JSONArray origem =
                    new JSONArray(json);

            JSONArray limpa =
                    new JSONArray();

            for (
                    int i = 0;
                    i < origem.length() &&
                    limpa.length() < MAX_ITEMS;
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

                if (texto.length() > MAX_TEXT) {
                    texto =
                            texto.substring(
                                    0,
                                    MAX_TEXT
                            );
                }

                String categoria =
                        sanitizeCategory(
                                original.optString(
                                        "category",
                                        "geral"
                                )
                        );

                JSONObject item =
                        new JSONObject();

                item.put(
                        "text",
                        texto
                );

                item.put(
                        "category",
                        categoria
                );

                item.put(
                        "time",
                        original.optLong(
                                "time",
                                System.currentTimeMillis()
                        )
                );

                limpa.put(item);
            }

            saveItems(limpa);

            return true;

        } catch (Exception ignored) {

            return false;
        }
    }

    // ============================================================
    // OBTER JSON DE UMA MEMÓRIA
    // ============================================================

    public synchronized String getMemoryJson(
            int index
    ) {

        JSONArray items =
                readItems();

        if (index < 0 ||
                index >= items.length()) {

            return "";
        }

        JSONObject item =
                items.optJSONObject(index);

        if (item == null) {
            return "";
        }

        return item.toString();
    }

    // ============================================================
    // LEITURA INTERNA
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

    // ============================================================
    // SALVAR INTERNAMENTE
    // ============================================================

    private void saveItems(
            JSONArray items
    ) {

        prefs.edit()
                .putString(
                        KEY_ITEMS,
                        items.toString()
                )
                .apply();
    }

    // ============================================================
    // LIMITAR QUANTIDADE
    // ============================================================

    private JSONArray limitItems(
            JSONArray items
    ) {

        if (items.length() <= MAX_ITEMS) {
            return items;
        }

        JSONArray reduzido =
                new JSONArray();

        int inicio =
                items.length() - MAX_ITEMS;

        for (
                int i = inicio;
                i < items.length();
                i++
        ) {

            reduzido.put(
                    items.opt(i)
            );
        }

        return reduzido;
    }

    // ============================================================
    // LIMPAR CATEGORIA
    // ============================================================

    private String sanitizeCategory(
            String category
    ) {

        if (category == null) {
            return "geral";
        }

        String value =
                category.trim();

        if (value.isEmpty()) {
            return "geral";
        }

        if (value.length() > MAX_CATEGORY) {
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
            String text
    ) {

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
}
