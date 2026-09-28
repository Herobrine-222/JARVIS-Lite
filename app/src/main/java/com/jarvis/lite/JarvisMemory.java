package com.jarvis.lite;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class JarvisMemory {

    private static final String PREFS = "jarvis_memory";
    private static final String KEY_ITEMS = "items";

    private static final int MAX_ITEMS = 100;
    private static final int MAX_TEXT = 500;

    private final SharedPreferences prefs;

    public JarvisMemory(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                );
    }

    public synchronized boolean remember(String text) {

        if (text == null) {
            return false;
        }

        String value = text.trim();

        if (value.isEmpty()) {
            return false;
        }

        if (value.length() > MAX_TEXT) {
            value = value.substring(0, MAX_TEXT);
        }

        JSONArray old = readItems();
        JSONArray next = new JSONArray();

        try {

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

                if (!existing.equalsIgnoreCase(value)) {
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
                    "time",
                    System.currentTimeMillis()
            );

            next.put(novo);

            while (next.length() > MAX_ITEMS) {

                JSONArray reduzido =
                        new JSONArray();

                for (
                        int i = 1;
                        i < next.length();
                        i++
                ) {
                    reduzido.put(
                            next.get(i)
                    );
                }

                next = reduzido;
            }

            prefs.edit()
                    .putString(
                            KEY_ITEMS,
                            next.toString()
                    )
                    .apply();

            return true;

        } catch (Exception ignored) {

            return false;
        }
    }

    public boolean rememberKeyValue(
            String key,
            String value
    ) {

        if (key == null || value == null) {
            return false;
        }

        return remember(
                key.trim()
                        + ": "
                        + value.trim()
        );
    }

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

            prefs.edit()
                    .putString(
                            KEY_ITEMS,
                            next.toString()
                    )
                    .apply();

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

    public synchronized String exportJson() {

        return readItems().toString();
    }

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

                JSONObject item =
                        new JSONObject();

                item.put(
                        "text",
                        texto
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

            prefs.edit()
                    .putString(
                            KEY_ITEMS,
                            limpa.toString()
                    )
                    .apply();

            return true;

        } catch (Exception ignored) {

            return false;
        }
    }

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
                java.text.Normalizer.normalize(
                        value,
                        java.text.Normalizer.Form.NFD
                );

        return value
                .replaceAll("\\p{M}+", "")
                .trim();
    }
 }
