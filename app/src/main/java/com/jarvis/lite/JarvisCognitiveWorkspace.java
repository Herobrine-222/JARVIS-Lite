
package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Area de trabalho cognitiva temporaria do JARVIS.
 *
 * Guarda dados de tarefas em andamento na memoria do processo.
 * Nao substitui JarvisMemory nem grava automaticamente no SQLite.
 */
public final class JarvisCognitiveWorkspace {

    public static final int PRIORITY_LOW = 1;
    public static final int PRIORITY_NORMAL = 2;
    public static final int PRIORITY_HIGH = 3;
    public static final int PRIORITY_CRITICAL = 4;

    public static final int DEFAULT_MAX_ITEMS = 100;
    public static final int MAX_KEY_LENGTH = 120;
    public static final int MAX_VALUE_LENGTH = 4000;

    private final int maxItems;
    private final List<WorkspaceItem> items =
            new ArrayList<>();

    private long nextId = 1;
    private long revision = 0;
    private String activeTask = "";
    private boolean closed = false;

    public JarvisCognitiveWorkspace() {
        this(DEFAULT_MAX_ITEMS);
    }

    public JarvisCognitiveWorkspace(int maxItems) {
        if (maxItems < 1) {
            throw new IllegalArgumentException(
                    "maxItems deve ser maior que zero.");
        }

        this.maxItems = maxItems;
    }

    /**
     * Representa um dado temporario da tarefa.
     */
    public static final class WorkspaceItem {
        private final long id;
        private final String key;
        private final String value;
        private final int priority;
        private final long createdAt;
        private final long updatedAt;

        private WorkspaceItem(
                long id,
                String key,
                String value,
                int priority,
                long createdAt,
                long updatedAt) {

            this.id = id;
            this.key = key;
            this.value = value;
            this.priority = priority;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public long getId() {
            return id;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public int getPriority() {
            return priority;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public long getUpdatedAt() {
            return updatedAt;
        }
    }

    /**
     * Define a tarefa que esta sendo trabalhada.
     */
    public synchronized boolean setActiveTask(String task) {
        if (closed || task == null) {
            return false;
        }

        String clean = task.trim();

        if (clean.length() > MAX_VALUE_LENGTH) {
            return false;
        }

        activeTask = clean;
        revision++;
        return true;
    }

    public synchronized String getActiveTask() {
        return activeTask;
    }

    /**
     * Adiciona um dado temporario.
     * Se a chave ja existir, atualiza seu valor.
     */
    public synchronized boolean put(
            String key,
            String value,
            int priority) {

        if (closed
                || key == null
                || value == null
                || priority < PRIORITY_LOW
                || priority > PRIORITY_CRITICAL) {
            return false;
        }

        String cleanKey = key.trim();
        String cleanValue = value.trim();

        if (cleanKey.isEmpty()
                || cleanKey.length() > MAX_KEY_LENGTH
                || cleanValue.length() > MAX_VALUE_LENGTH) {
            return false;
        }

        long now = System.currentTimeMillis();

        for (int i = 0; i < items.size(); i++) {
            WorkspaceItem old = items.get(i);

            if (old.key.equalsIgnoreCase(cleanKey)) {
                items.set(i, new WorkspaceItem(
                        old.id,
                        old.key,
                        cleanValue,
                        priority,
                        old.createdAt,
                        now
                ));

                revision++;
                return true;
            }
        }

        if (items.size() >= maxItems) {
            int removableIndex = findLowestPriorityIndex();

            if (removableIndex < 0) {
                return false;
            }

            WorkspaceItem lowest = items.get(removableIndex);

            // Nao remove informacao de prioridade maior
            // para inserir outra de prioridade inferior.
            if (lowest.priority > priority) {
                return false;
            }

            items.remove(removableIndex);
        }

        items.add(new WorkspaceItem(
                nextId++,
                cleanKey,
                cleanValue,
                priority,
                now,
                now
        ));

        revision++;
        return true;
    }

    public synchronized boolean put(
            String key,
            String value) {

        return put(key, value, PRIORITY_NORMAL);
    }

    public synchronized WorkspaceItem get(String key) {
        if (closed || key == null) {
            return null;
        }

        for (WorkspaceItem item : items) {
            if (item.key.equalsIgnoreCase(key.trim())) {
                return item;
            }
        }

        return null;
    }

    public synchronized String getValue(String key) {
        WorkspaceItem item = get(key);
        return item == null ? null : item.getValue();
    }

    public synchronized boolean contains(String key) {
        return get(key) != null;
    }

    /**
     * Remove somente a entrada temporaria indicada.
     */
    public synchronized boolean remove(String key) {
        if (closed || key == null) {
            return false;
        }

        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).key.equalsIgnoreCase(key.trim())) {
                items.remove(i);
                revision++;
                return true;
            }
        }

        return false;
    }

    /**
     * Retorna uma copia ordenada por prioridade decrescente.
     */
    public synchronized List<WorkspaceItem> getItems() {
        List<WorkspaceItem> result =
                new ArrayList<>(items);

        Collections.sort(result,
                new Comparator<WorkspaceItem>() {
                    @Override
                    public int compare(
                            WorkspaceItem a,
                            WorkspaceItem b) {

                        if (a.priority != b.priority) {
                            return b.priority - a.priority;
                        }

                        return Long.compare(
                                b.updatedAt,
                                a.updatedAt
                        );
                    }
                });

        return Collections.unmodifiableList(result);
    }

    public synchronized int getItemCount() {
        return items.size();
    }

    public synchronized int getMaxItems() {
        return maxItems;
    }

    public synchronized long getRevision() {
        return revision;
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    /**
     * Limpa somente os dados desta area temporaria.
     */
    public synchronized void clearWorkspace() {
        if (closed) {
            return;
        }

        items.clear();
        activeTask = "";
        revision++;
    }

    private int findLowestPriorityIndex() {
        if (items.isEmpty()) {
            return -1;
        }

        int lowestIndex = 0;

        for (int i = 1; i < items.size(); i++) {
            if (items.get(i).priority
                    < items.get(lowestIndex).priority) {
                lowestIndex = i;
            }
        }

        return lowestIndex;
    }

    /**
     * Encerra esta instancia e libera os dados temporarios.
     * Nao afeta a memoria SQLite nem outros modulos.
     */
    public synchronized void close() {
        items.clear();
        activeTask = "";
        closed = true;
        revision++;
    }
}
