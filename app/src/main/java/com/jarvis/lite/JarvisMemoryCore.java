
package com.jarvis.lite;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Coordenador leve da memoria existente do JARVIS Lite.
 *
 * Reutiliza JarvisMemory e JarvisContextManager.
 * Nao cria outro banco, nao apaga dados e nao acessa a Internet.
 * Nenhuma memoria e gravada automaticamente.
 */
public final class JarvisMemoryCore {

    private static final int MAX_QUERY_LENGTH = 1000;
    private static final int MAX_RESULTS = 5;

    private final JarvisContextManager contextManager;
    private final JarvisMemory memory;

    public JarvisMemoryCore(Context context) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "O contexto Android nao pode ser nulo."
            );
        }

        contextManager = new JarvisContextManager(
                context.getApplicationContext()
        );

        memory = contextManager.getMemory();

        if (memory == null) {
            throw new IllegalStateException(
                    "A memoria existente nao esta disponivel."
            );
        }
    }

    public JarvisMemoryCore(
            JarvisContextManager contextManager
    ) {
        if (contextManager == null
                || contextManager.getMemory() == null) {
            throw new IllegalArgumentException(
                    "O ContextManager precisa fornecer a memoria."
            );
        }

        this.contextManager = contextManager;
        this.memory = contextManager.getMemory();
    }

    public synchronized String buildContext(String query) {
        if (!isValidQuery(query)) {
            return "";
        }

        try {
            String result =
                    contextManager.getRelevantMemory(query.trim());

            return result == null ? "" : result;
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    public synchronized String buildConversationContext(
            String query
    ) {
        if (!isValidQuery(query)) {
            return "";
        }

        try {
            String result =
                    contextManager.buildContext(query.trim());

            return result == null ? "" : result;
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    public synchronized List<String> search(String query) {
        if (!isValidQuery(query)) {
            return Collections.emptyList();
        }

        try {
            List<String> found =
                    memory.search(query.trim(), MAX_RESULTS);

            if (found == null || found.isEmpty()) {
                return Collections.emptyList();
            }

            return Collections.unmodifiableList(
                    new ArrayList<>(found)
            );
        } catch (RuntimeException ignored) {
            return Collections.emptyList();
        }
    }

    /**
     * So grava quando outro componente chama este metodo.
     * Nao deve ser chamado automaticamente para cada mensagem.
     */
    public synchronized boolean remember(
            String text,
            String category
    ) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        try {
            return memory.remember(text.trim(), category);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public synchronized boolean rememberImportant(
            String text,
            String category,
            int importance
    ) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        try {
            int safeImportance =
                    Math.max(0, Math.min(100, importance));

            return memory.rememberImportant(
                    text.trim(),
                    category,
                    safeImportance
            );
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public JarvisMemory getMemory() {
        return memory;
    }

    public JarvisContextManager getContextManager() {
        return contextManager;
    }

    private boolean isValidQuery(String query) {
        return query != null
                && !query.trim().isEmpty()
                && query.length() <= MAX_QUERY_LENGTH;
    }
}
