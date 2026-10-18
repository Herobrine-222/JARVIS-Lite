
package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Gerenciador de atencao do JARVIS.
 *
 * Calcula prioridades internas para tarefas.
 * Nao executa comandos nem altera o dispositivo.
 */
public final class JarvisAttentionManager {

    public static final int PRIORITY_LOW = 1;
    public static final int PRIORITY_NORMAL = 2;
    public static final int PRIORITY_HIGH = 3;
    public static final int PRIORITY_CRITICAL = 4;

    public static final int MAX_TASKS = 200;
    public static final int MAX_DESCRIPTION_LENGTH = 1000;

    private final List<AttentionTask> tasks =
            new ArrayList<>();

    private long nextId = 1;
    private long revision = 0;

    public enum TaskStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }

    public static final class AttentionTask {
        private final long id;
        private final String title;
        private final String description;
        private final int priority;
        private final long createdAt;
        private final long deadlineAt;
        private final TaskStatus status;

        private AttentionTask(
                long id,
                String title,
                String description,
                int priority,
                long createdAt,
                long deadlineAt,
                TaskStatus status) {

            this.id = id;
            this.title = title;
            this.description = description;
            this.priority = priority;
            this.createdAt = createdAt;
            this.deadlineAt = deadlineAt;
            this.status = status;
        }

        public long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public int getPriority() {
            return priority;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public long getDeadlineAt() {
            return deadlineAt;
        }

        public TaskStatus getStatus() {
            return status;
        }
    }

    /**
     * Cria uma tarefa sem prazo definido.
     */
    public synchronized long addTask(
            String title,
            String description,
            int priority) {

        return addTask(
                title,
                description,
                priority,
                0L
        );
    }

    /**
     * deadlineAt usa milissegundos desde a epoca Unix.
     * Zero significa que nao ha prazo definido.
     */
    public synchronized long addTask(
            String title,
            String description,
            int priority,
            long deadlineAt) {

        if (title == null
                || description == null
                || priority < PRIORITY_LOW
                || priority > PRIORITY_CRITICAL
                || deadlineAt < 0
                || tasks.size() >= MAX_TASKS) {
            return -1L;
        }

        String cleanTitle = title.trim();
        String cleanDescription = description.trim();

        if (cleanTitle.isEmpty()
                || cleanTitle.length() > 200
                || cleanDescription.length()
                        > MAX_DESCRIPTION_LENGTH) {
            return -1L;
        }

        long id = nextId++;

        tasks.add(new AttentionTask(
                id,
                cleanTitle,
                cleanDescription,
                priority,
                System.currentTimeMillis(),
                deadlineAt,
                TaskStatus.PENDING
        ));

        revision++;
        return id;
    }

    public synchronized AttentionTask getTask(long id) {
        for (AttentionTask task : tasks) {
            if (task.id == id) {
                return task;
            }
        }

        return null;
    }

    /**
     * Altera o estado de uma tarefa.
     */
    public synchronized boolean setStatus(
            long id,
            TaskStatus newStatus) {

        if (newStatus == null) {
            return false;
        }

        for (int i = 0; i < tasks.size(); i++) {
            AttentionTask old = tasks.get(i);

            if (old.id == id) {
                tasks.set(i, new AttentionTask(
                        old.id,
                        old.title,
                        old.description,
                        old.priority,
                        old.createdAt,
                        old.deadlineAt,
                        newStatus
                ));

                revision++;
                return true;
            }
        }

        return false;
    }

    /**
     * Retorna tarefas abertas em ordem de atencao.
     */
    public synchronized List<AttentionTask> getPrioritizedTasks() {
        List<AttentionTask> result =
                new ArrayList<>();

        for (AttentionTask task : tasks) {
            if (task.status == TaskStatus.PENDING
                    || task.status == TaskStatus.IN_PROGRESS) {
                result.add(task);
            }
        }

        Collections.sort(result,
                new Comparator<AttentionTask>() {
                    @Override
                    public int compare(
                            AttentionTask a,
                            AttentionTask b) {

                        int scoreA = calculateAttentionScore(a);
                        int scoreB = calculateAttentionScore(b);

                        if (scoreA != scoreB) {
                            return scoreB - scoreA;
                        }

                        return Long.compare(
                                a.createdAt,
                                b.createdAt
                        );
                    }
                });

        return Collections.unmodifiableList(result);
    }

    /**
     * Pontuacao heuristica de 0 a 100.
     * Nao representa probabilidade estatistica.
     */
    public synchronized int calculateAttentionScore(
            AttentionTask task) {

        if (task == null) {
            return 0;
        }

        if (task.status == TaskStatus.COMPLETED
                || task.status == TaskStatus.CANCELLED) {
            return 0;
        }

        int score;

        switch (task.priority) {
            case PRIORITY_CRITICAL:
                score = 100;
                break;

            case PRIORITY_HIGH:
                score = 75;
                break;

            case PRIORITY_NORMAL:
                score = 50;
                break;

            case PRIORITY_LOW:
                score = 25;
                break;

            default:
                score = 0;
                break;
        }

        if (task.status == TaskStatus.IN_PROGRESS) {
            score = Math.min(100, score + 5);
        }

        if (task.deadlineAt > 0) {
            long remaining =
                    task.deadlineAt - System.currentTimeMillis();

            if (remaining <= 0) {
                score = Math.min(100, score + 20);
            } else if (remaining <= 15 * 60 * 1000L) {
                score = Math.min(100, score + 15);
            } else if (remaining <= 60 * 60 * 1000L) {
                score = Math.min(100, score + 10);
            }
        }

        return score;
    }

    public synchronized int getTaskCount() {
        return tasks.size();
    }

    public synchronized int getOpenTaskCount() {
        int count = 0;

        for (AttentionTask task : tasks) {
            if (task.status == TaskStatus.PENDING
                    || task.status == TaskStatus.IN_PROGRESS) {
                count++;
            }
        }

        return count;
    }

    public synchronized long getRevision() {
        return revision;
    }

    /**
     * Remove apenas tarefas concluidas ou canceladas.
     */
    public synchronized int removeFinishedTasks() {
        int before = tasks.size();

        for (int i = tasks.size() - 1; i >= 0; i--) {
            TaskStatus status = tasks.get(i).status;

            if (status == TaskStatus.COMPLETED
                    || status == TaskStatus.CANCELLED) {
                tasks.remove(i);
            }
        }

        int removed = before - tasks.size();

        if (removed > 0) {
            revision++;
        }

        return removed;
    }

    /**
     * Retorna uma copia de todas as tarefas.
     */
    public synchronized List<AttentionTask> getAllTasks() {
        return Collections.unmodifiableList(
                new ArrayList<>(tasks)
        );
    }
}
