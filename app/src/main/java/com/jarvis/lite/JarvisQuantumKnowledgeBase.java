package com.jarvis.lite;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Recuperador local de passagens do Caderno Unificado de Ciência Quântica.
 *
 * O conteúdo integral fica em app/src/main/assets/quantum/ e é lido
 * somente do armazenamento interno do APK. Esta classe não usa internet,
 * não altera a memória SQLite e não executa comandos do dispositivo.
 */
public final class JarvisQuantumKnowledgeBase {

    private static final String ASSET_PATH =
            "quantum/caderno_unificado_ciencia_quantica_volume1.md";

    private static final int MAX_RESPONSE_CHARS = 2200;
    private static final int MAX_PASSAGES = 3;

    private static final Set<String> STOP_WORDS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    "a", "ao", "aos", "as", "o", "os",
                    "de", "do", "dos", "da", "das",
                    "em", "no", "nos", "na", "nas",
                    "um", "uma", "uns", "umas",
                    "e", "ou", "por", "para", "com", "sem",
                    "como", "qual", "quais", "quando", "onde",
                    "quem", "que", "porque", "por que",
                    "explique", "explicar", "explicacao", "explicacao",
                    "fale", "falar", "diga", "mostrar", "mostre",
                    "me", "voce", "seu", "sua", "seus", "suas",
                    "eu", "quero", "gostaria", "poderia", "pode",
                    "saber", "sobre", "isso", "isto", "aqui",
                    "ciencia", "quantica", "quantico", "caderno",
                    "volume", "tema", "assunto", "conceito"
            )));

    private static volatile List<Passage> cachedPassages;
    private static volatile boolean loadAttempted;
    private static volatile Context applicationContext;

    /**
     * Inicializa o recurso com o contexto da aplicação sem reter a Activity.
     * É usado porque o MainActivity também mantém um JarvisBrain de
     * compatibilidade construído sem parâmetros.
     */
    public static void initialize(Context context) {
        if (context == null) {
            return;
        }

        Context app = context.getApplicationContext();
        applicationContext = app != null ? app : context;
    }

    private JarvisQuantumKnowledgeBase() {
    }

    /**
     * Retorna as passagens mais relevantes ou null se o tema não estiver
     * suficientemente representado no caderno.
     */
    public static String answer(Context context, String question) {
        Context resolvedContext = context;
        if (resolvedContext != null) {
            Context app = resolvedContext.getApplicationContext();
            if (app != null) {
                resolvedContext = app;
            }
        } else {
            resolvedContext = applicationContext;
        }

        if (resolvedContext == null || question == null
                || question.trim().isEmpty()) {
            return null;
        }

        List<String> terms = keywords(question);
        if (terms.isEmpty()) {
            return null;
        }

        List<Passage> passages = getPassages(resolvedContext);
        if (passages == null || passages.isEmpty()) {
            return null;
        }

        List<ScoredPassage> matches = new ArrayList<>();

        for (Passage passage : passages) {
            int score = 0;
            int matchedTerms = 0;

            for (String term : terms) {
                boolean inHeading = containsWord(
                        passage.headingTokens, term
                );
                boolean inBody = containsWord(
                        passage.bodyTokens, term
                );

                if (inHeading || inBody) {
                    matchedTerms++;
                }

                if (inHeading) {
                    score += 4;
                }

                if (inBody) {
                    score += 2;
                }

                // Consultas de uma única palavra útil ainda podem recuperar
                // um parágrafo que não tenha esse termo no título.
                if (terms.size() == 1 && inBody) {
                    score += 2;
                }
            }

            if (matchedTerms > 0 && score >= 4) {
                matches.add(new ScoredPassage(passage, score));
            }
        }

        if (matches.isEmpty()) {
            return null;
        }

        Collections.sort(matches, new Comparator<ScoredPassage>() {
            @Override
            public int compare(ScoredPassage left, ScoredPassage right) {
                return Integer.compare(right.score, left.score);
            }
        });

        int bestScore = matches.get(0).score;
        StringBuilder response = new StringBuilder(
                "Encontrei estas passagens no caderno offline de ciência "
                        + "quântica (Volume I):\n\n"
        );

        int included = 0;
        for (ScoredPassage match : matches) {
            if (included >= MAX_PASSAGES
                    || match.score < bestScore - 2) {
                break;
            }

            String heading = match.passage.heading.trim();
            String body = match.passage.body.trim();
            if (body.isEmpty()) {
                continue;
            }

            String block = heading + "\n" + body + "\n\n";
            int available = MAX_RESPONSE_CHARS - response.length();

            if (block.length() > available) {
                if (available > heading.length() + 8) {
                    int bodyRoom = available - heading.length() - 5;
                    String shortened = body.substring(
                            0, Math.min(body.length(), bodyRoom)
                    );
                    int lastSpace = shortened.lastIndexOf(' ');
                    if (lastSpace > shortened.length() / 2) {
                        shortened = shortened.substring(0, lastSpace);
                    }
                    response.append(heading)
                            .append('\n')
                            .append(shortened)
                            .append("…");
                }
                break;
            }

            response.append(block);
            included++;
        }

        if (included == 0) {
            return null;
        }

        return response.toString().trim();
    }

    private static List<Passage> getPassages(Context context) {
        List<Passage> current = cachedPassages;
        if (current != null || loadAttempted) {
            return current;
        }

        synchronized (JarvisQuantumKnowledgeBase.class) {
            current = cachedPassages;
            if (current != null || loadAttempted) {
                return current;
            }

            loadAttempted = true;
            try (InputStream input = context.getAssets().open(ASSET_PATH);
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(input, StandardCharsets.UTF_8))) {

                StringBuilder document = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    document.append(line).append('\n');
                }

                cachedPassages = Collections.unmodifiableList(
                        parsePassages(document.toString())
                );
                return cachedPassages;

            } catch (IOException | RuntimeException ignored) {
                // Falha de leitura não deve derrubar a conversa principal.
                cachedPassages = null;
                return null;
            }
        }
    }

    private static List<Passage> parsePassages(String document) {
        List<Passage> output = new ArrayList<>();
        String[] headings = new String[6];
        StringBuilder paragraph = new StringBuilder();

        String[] lines = document.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("#")) {
                flushParagraph(output, paragraph, headings);

                int level = 0;
                while (level < trimmed.length()
                        && trimmed.charAt(level) == '#') {
                    level++;
                }

                if (level > 0 && level <= headings.length
                        && (trimmed.length() == level
                        || Character.isWhitespace(trimmed.charAt(level)))) {
                    for (int i = level - 1; i < headings.length; i++) {
                        headings[i] = null;
                    }

                    headings[level - 1] = trimmed
                            .substring(level)
                            .trim();
                }
                continue;
            }

            if (trimmed.isEmpty()) {
                flushParagraph(output, paragraph, headings);
                continue;
            }

            if (paragraph.length() > 0) {
                paragraph.append('\n');
            }
            paragraph.append(trimmed);
        }

        flushParagraph(output, paragraph, headings);
        return output;
    }

    private static void flushParagraph(
            List<Passage> output,
            StringBuilder paragraph,
            String[] headings
    ) {
        String body = paragraph.toString().trim();
        paragraph.setLength(0);

        if (body.isEmpty()) {
            return;
        }

        StringBuilder title = new StringBuilder();
        for (String heading : headings) {
            if (heading == null || heading.trim().isEmpty()) {
                continue;
            }

            if (title.length() > 0) {
                title.append(" > ");
            }
            title.append(heading.trim());
        }

        String headingText = title.length() == 0
                ? "Caderno de ciência quântica"
                : title.toString();

        output.add(new Passage(headingText, body));
    }

    private static List<String> keywords(String text) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> unique = new HashSet<>();
        for (String token : normalized.split("\\s+")) {
            if (token.length() >= 3 && !STOP_WORDS.contains(token)) {
                unique.add(token);
            }
        }

        return new ArrayList<>(unique);
    }

    private static boolean containsWord(Set<String> words, String queryWord) {
        if (words.contains(queryWord)) {
            return true;
        }

        // Normaliza diferenças simples entre singular e plural em português.
        if (queryWord.length() > 4 && queryWord.endsWith("s")
                && words.contains(queryWord.substring(0, queryWord.length() - 1))) {
            return true;
        }

        return queryWord.length() > 4 && words.contains(queryWord + "s");
    }

    private static Set<String> tokensOf(String text) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> tokens = new HashSet<>();
        tokens.addAll(Arrays.asList(normalized.split("\\s+")));
        return tokens;
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }

        String value = text.toLowerCase(Locale.ROOT);
        value = Normalizer.normalize(value, Normalizer.Form.NFD);

        return value
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static final class Passage {
        final String heading;
        final String body;
        final Set<String> headingTokens;
        final Set<String> bodyTokens;

        Passage(String heading, String body) {
            this.heading = heading;
            this.body = body;
            this.headingTokens = tokensOf(heading);
            this.bodyTokens = tokensOf(body);
        }
    }

    private static final class ScoredPassage {
        final Passage passage;
        final int score;

        ScoredPassage(Passage passage, int score) {
            this.passage = passage;
            this.score = score;
        }
    }
}
