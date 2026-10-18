
package com.jarvis.lite;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Validador não destrutivo de estruturas JSON de memória.
 *
 * Não abre nem modifica o SQLite.
 * Não importa, apaga, repara ou migra dados.
 * Apenas verifica a estrutura do texto recebido.
 */
public final class JarvisMemoryIntegrity {

    public static final class Report {
        public final boolean valid;
        public final int itemCount;
        public final String code;
        public final String message;

        private Report(
                boolean valid,
                int itemCount,
                String code,
                String message) {

            this.valid = valid;
            this.itemCount = itemCount;
            this.code = code;
            this.message = message;
        }
    }

    private JarvisMemoryIntegrity() {
        // Classe utilitária.
    }

    /**
     * Aceita uma raiz JSON em forma de array ou objeto.
     *
     * Para objetos, procura um array com um dos nomes reconhecidos.
     * Cada item do array deve ser um objeto JSON.
     *
     * A validação estrutural não garante que os dados sejam
     * semanticamente corretos ou seguros para importação.
     */
    public static Report validateExport(String json) {

        if (json == null || json.trim().isEmpty()) {
            return report(
                    false,
                    0,
                    "EMPTY_INPUT",
                    "O conteúdo de memória está vazio."
            );
        }

        String trimmed = json.trim();

        try {
            if (trimmed.startsWith("[")) {
                JSONArray array = new JSONArray(trimmed);
                return validateArray(array);
            }

            if (trimmed.startsWith("{")) {
                JSONObject object = new JSONObject(trimmed);
                JSONArray items = findItemsArray(object);

                if (items == null) {
                    return report(
                            false,
                            0,
                            "ITEMS_ARRAY_MISSING",
                            "O objeto JSON não contém um array de "
                                    + "memória reconhecido."
                    );
                }

                return validateArray(items);
            }

            return report(
                    false,
                    0,
                    "INVALID_ROOT",
                    "Era esperado um array ou objeto JSON."
            );

        } catch (JSONException ex) {
            return report(
                    false,
                    0,
                    "INVALID_JSON",
                    "O conteúdo não é um JSON válido."
            );

        } catch (RuntimeException ex) {
            return report(
                    false,
                    0,
                    "VALIDATION_ERROR",
                    "Não foi possível validar o conteúdo com segurança."
            );
        }
    }

    private static Report validateArray(JSONArray items) {

        for (int i = 0; i < items.length(); i++) {
            Object item = items.opt(i);

            if (!(item instanceof JSONObject)) {
                return report(
                        false,
                        items.length(),
                        "INVALID_ITEM",
                        "O item no índice " + i
                                + " não é um objeto JSON."
                );
            }
        }

        return report(
                true,
                items.length(),
                "STRUCTURE_OK",
                "A estrutura JSON é válida. Isso não garante "
                        + "a correção dos dados nem a segurança "
                        + "de uma eventual importação."
        );
    }

    private static JSONArray findItemsArray(JSONObject object) {

        String[] keys = {
                "items",
                "memories",
                "memory",
                "data"
        };

        for (String key : keys) {
            Object value = object.opt(key);

            if (value instanceof JSONArray) {
                return (JSONArray) value;
            }
        }

        return null;
    }

    private static Report report(
            boolean valid,
            int itemCount,
            String code,
            String message) {

        return new Report(valid, itemCount, code, message);
    }
}
