
package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Organiza tarefas, dependências e prioridades do JARVIS Lite.
 *
 * Este módulo apenas planeja. Ele não executa tarefas,
 * comandos externos ou operações no dispositivo.
 */
public final class JarvisPlanningEngine {

    public static final int DEFAULT_MAX_TASKS = 200;
    public static final int MAX_TEXT_LENGTH = 500;

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }

    public enum Status {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        BLOCKED,
        CANCELLED
    }

    public enum Result {
        SUCCESS,
        INVALID_INPUT,
        TASK_NOT_FOUND,
        TASK_LIMIT_REACHED,
        DEPENDENCY_NOT_FOUND,
        DUPLICATE_DEPENDENCY,
        CYCLE_DETECTED,
        INVALID_TRANSITION,
        DEPENDENCIES_INCOMPLETE
    }

    public static final class Task {

        private final long id;
        private final String title;
        private final String description;
        private final Priority priority;
        private final long createdAt;

        private final Set<Long> dependencies =
                new LinkedHashSet<Long>();

        private Status status = Status.PENDING;

        private Task(
                long id,
                String title,
                String description,
                Priority priority
        ) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.priority = priority;
            this.createdAt = System.currentTimeMillis();
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

        public Priority getPriority() {
            return priority;
        }

        public Status getStatus() {
            return status;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public Set<Long> getDependencies() {
            return Collections.unmodifiableSet(
                    new LinkedHashSet<Long>(dependencies)
            );
        }
    }

    private final int maxTasks;

    private final Map<Long, Task> tasks =
            new LinkedHashMap<Long, Task>();

    private long nextId = 1L;

    public JarvisPlanningEngine() {
        this(DEFAULT_MAX_TASKS);
    }

    public JarvisPlanningEngine(int maxTasks) {
        this.maxTasks = Math.max(
                1,
                Math.min(maxTasks, 2000)
        );
    }

    /**
     * Cria uma tarefa sem executá-la.
     */
    public synchronized long addTask(
            String title,
            String description,
            Priority priority
    ) {
        String safeTitle = normalize(title);
        String safeDescription = normalize(description);

        if (!isValidText(safeTitle)
                || safeDescription.length() > MAX_TEXT_LENGTH
                || priority == null) {
            return -1L;
        }

        if (tasks.size() >= maxTasks) {
            return -1L;
        }

        long id = nextId++;

        Task task = new Task(
                id,
                safeTitle,
                safeDescription,
                priority
        );

        tasks.put(id, task);

        return id;
    }

    /**
     * Define que uma tarefa depende da conclusão de outra.
     */
    public synchronized Result addDependency(
            long taskId,
            long dependencyId
    ) {
        Task task = tasks.get(taskId);
        Task dependency = tasks.get(dependencyId);

        if (task == null || dependency == null) {
            return Result.TASK_NOT_FOUND;
        }

        if (taskId == dependencyId) {
            return Result.CYCLE_DETECTED;
        }

        if (task.status != Status.PENDING) {
            return Result.INVALID_TRANSITION;
        }

        if (task.dependencies.contains(dependencyId)) {
            return Result.DUPLICATE_DEPENDENCY;
        }

        if (dependency.status == Status.CANCELLED
                || dependency.status == Status.BLOCKED) {
            return Result.DEPENDENCIES_INCOMPLETE;
        }

        if (createsCycle(taskId, dependencyId)) {
            return Result.CYCLE_DETECTED;
        }

        task.dependencies.add(dependencyId);

        return Result.SUCCESS;
    }

    private boolean createsCycle(
            long taskId,
            long dependencyId
    ) {
        return canReach(
                dependencyId,
                taskId,
                new LinkedHashSet<Long>()
        );
    }

    private boolean canReach(
            long currentId,
            long targetId,
            Set<Long> visited
    ) {
        if (currentId == targetId) {
            return true;
        }

        if (!visited.add(currentId)) {
            return false;
        }

        Task current = tasks.get(currentId);

        if (current == null) {
            return false;
        }

        for (Long dependencyId : current.dependencies) {
            if (canReach(dependencyId, targetId, visited)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Atualiza o estado de uma tarefa.
     * Não executa a tarefa na prática.
     */
    public synchronized Result updateStatus(
            long taskId,
            Status newStatus
    ) {
        Task task = tasks.get(taskId);

        if (task == null) {
            return Result.TASK_NOT_FOUND;
        }

        if (newStatus == null
                || !isAllowedTransition(task.status, newStatus)) {
            return Result.INVALID_TRANSITION;
        }

        if (newStatus == Status.IN_PROGRESS
                || newStatus == Status.COMPLETED) {
            if (!areDependenciesCompleted(task)) {
                return Result.DEPENDENCIES_INCOMPLETE;
            }
        }

        task.status = newStatus;

        return Result.SUCCESS;
    }

    private boolean areDependenciesCompleted(Task task) {
        for (Long dependencyId : task.dependencies) {
            Task dependency = tasks.get(dependencyId);

            if (dependency == null
                    || dependency.status != Status.COMPLETED) {
                return false;
            }
        }

        return true;
    }

    private boolean isAllowedTransition(
            Status current,
            Status next
    ) {
        if (current == next) {
            return true;
        }

        switch (current) {
            case PENDING:
                return next == Status.IN_PROGRESS
                        || next == Status.BLOCKED
                        || next == Status.CANCELLED;

            case IN_PROGRESS:
                return next == Status.COMPLETED
                        || next == Status.BLOCKED
                        || next == Status.CANCELLED;

            case BLOCKED:
                return next == Status.PENDING
                        || next == Status.CANCELLED;

            case COMPLETED:
            case CANCELLED:
            default:
                return false;
        }
    }

    /**
     * Lista tarefas prontas para começar,
     * ordenadas por prioridade.
     */
    public synchronized List<Task> getReadyTasks() {
        List<Task> result = new ArrayList<Task>();

        for (Task task : tasks.values()) {
            if (task.status == Status.PENDING
                    && areDependenciesCompleted(task)) {
                result.add(task);
            }
        }

        Collections.sort(result, (first, second) -> {
            int priorityComparison = Integer.compare(
                    second.priority.ordinal(),
                    first.priority.ordinal()
            );

            if (priorityComparison != 0) {
                return priorityComparison;
            }

            return Long.compare(first.id, second.id);
        });

        return Collections.unmodifiableList(result);
    }

    public synchronized Task getTask(long taskId) {
        return tasks.get(taskId);
    }

    public synchronized List<Task> getAllTasks() {
        return Collections.unmodifiableList(
                new ArrayList<Task>(tasks.values())
        );
    }

    public synchronized List<Task> getTasksByStatus(
            Status status
    ) {
        List<Task> result = new ArrayList<Task>();

        if (status == null) {
            return Collections.unmodifiableList(result);
        }

        for (Task task : tasks.values()) {
            if (task.status == status) {
                result.add(task);
            }
        }

        return Collections.unmodifiableList(result);
    }

    public synchronized boolean removePendingTask(long taskId) {
        Task task = tasks.get(taskId);

        if (task == null || task.status != Status.PENDING) {
            return false;
        }

        for (Task other : tasks.values()) {
            if (other.dependencies.contains(taskId)) {
                return false;
            }
        }

        tasks.remove(taskId);

        return true;
    }

    public synchronized int getTaskCount() {
        return tasks.size();
    }

    public int getMaxTasks() {
        return maxTasks;
    }

    public synchronized void clear() {
        tasks.clear();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isValidText(String value) {
        return value != null
                && !value.trim().isEmpty()
                && value.length() <= MAX_TEXT_LENGTH;
    }
}
