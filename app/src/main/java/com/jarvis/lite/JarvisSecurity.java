package com.jarvis.lite;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Regras simples de defesa para entradas controladas pelo usuário.
 * Não executa comandos, não concede permissões e não acessa a rede.
 */
public final class JarvisSecurity {

    public static final int MAX_COMMAND_LENGTH = 2000;
    public static final int MAX_HISTORY_TEXT = 2000;
    public static final int MAX_HISTORY_AUTHOR = 32;

    private JarvisSecurity() {
    }

    public static String limitText(String value, int maxLength) {
        if (value == null || maxLength <= 0) {
            return "";
        }

        String cleaned = value
                .replace("\u0000", "")
                .replace("\u2028", " ")
                .replace("\u2029", " ")
                .trim();

        StringBuilder safe =
                new StringBuilder(Math.min(cleaned.length(), maxLength));

        for (int i = 0;
             i < cleaned.length() && safe.length() < maxLength;
             i++) {

            char c = cleaned.charAt(i);

            if (Character.isISOControl(c)
                    && c != '\n'
                    && c != '\t') {
                continue;
            }

            safe.append(c);
        }

        return safe.toString().trim();
    }

    public static String normalizeCommand(String value) {
        String limited = limitText(value, MAX_COMMAND_LENGTH);

        String base = Normalizer.normalize(
                limited.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );

        return base
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
