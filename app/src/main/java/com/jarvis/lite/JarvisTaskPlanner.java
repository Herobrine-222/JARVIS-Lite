
package com.jarvis.lite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Planejador de tarefas do JARVIS.
 * Organiza etapas sem executa-las automaticamente.
 */
public final class JarvisTaskPlanner {

    public static final int MAX_PLANS = 50;
    public static final int MAX_STEPS = 100;
    public static final int MAX_TEXT_LENGTH = 1000;

    public enum StepStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        BLOCKED,
        CANCELLED
    }

    public static final class PlanStep {
        private final long id;
        private final String description;
        private final int priority;
        private final List<Long> dependencies;
        private StepStatus status;

        private PlanStep(
                long id,
                String description,
                int priority,
                List<Long> dependencies) {
            this.id = id;
            this.description = description;
            this.priority = priority;
            this.dependencies =
                    Collections.unmodifiableList(
                            new ArrayList<>(dependencies));
            this.status = StepStatus.PENDING;
        }

        public long getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public int getPriority() {
            return priority;
        }

        public List<Long> getDependencies() {
            return dependencies;
        }

        public StepStatus getStatus() {
            return status;
        }
    }

    public static final class Plan {
        private final long id;
        private final String objective;
        private final List<PlanStep> steps =
                new ArrayList<>();
        private boolean cancelled;

        private Plan(long id, String objective) {
            this.id = id;
            this.objective = objective;
        }

        public long getId() {
            return id;
        }

        public String getObjective() {
            return objective;
        }

        public synchronized List<PlanStep> getSteps() {
            return Collections.unmodifiableList(
                    new ArrayList<>(steps));
        }

        public synchronized boolean isCancelled() {
            return cancelled;
        }

        public synchronized int getCompletedCount() {
            int count = 0;

            for (PlanStep step : steps) {
                if (step.status == StepStatus.COMPLETED) {
                    count++;
                }
            }

            return count;
        }
    }

    private final List<Plan> plans = new ArrayList<>();
    private long nextPlanId = 1;
    private long nextStepId = 1;

    public synchronized Plan createPlan(String objective) {
        if (objective == null
                || objective.trim().isEmpty()
                || objective.trim().length() > MAX_TEXT_LENGTH
                || plans.size() >= MAX_PLANS) {
            return null;
        }

        Plan plan = new Plan(
                nextPlanId++,
                objective.trim());

        plans.add(plan);
        return plan;
    }

    public synchronized Plan getPlan(long planId) {
        for (Plan plan : plans) {
            if (plan.id == planId) {
                return plan;
            }
        }

        return null;
    }

    public synchronized PlanStep addStep(
            long planId,
            String description,
            int priority,
            List<Long> dependencies) {

        Plan plan = getPlan(planId);

        if (plan == null
                || description == null
                || description.trim().isEmpty()
                || description.trim().length() > MAX_TEXT_LENGTH
                || priority < 1
                || priority > 4
                || dependencies == null) {
            return null;
        }

        synchronized (plan) {
            if (plan.cancelled
                    || plan.steps.size() >= MAX_STEPS) {
                return null;
            }

            for (Long dependencyId : dependencies) {
                if (dependencyId == null
                        || findStep(plan, dependencyId) == null) {
                    return null;
                }
            }

            long id = nextStepId++;

            // Uma etapa nao pode depender dela mesma.
            for (Long dependencyId : dependencies) {
                if (dependencyId == id) {
                    return null;
                }
            }

            PlanStep step = new PlanStep(
                    id,
                    description.trim(),
                    priority,
                    dependencies);

            plan.steps.add(step);
            return step;
        }
    }

    public synchronized PlanStep addStep(
            long planId,
            String description,
            int priority) {

        return addStep(
                planId,
                description,
                priority,
                Collections.<Long>emptyList());
    }

    public synchronized boolean setStepStatus(
            long planId,
            long stepId,
            StepStatus newStatus) {

        Plan plan = getPlan(planId);

        if (plan == null || newStatus == null) {
            return false;
        }

        synchronized (plan) {
            if (plan.cancelled) {
                return false;
            }

            PlanStep step = findStep(plan, stepId);

            if (step == null) {
                return false;
            }

            if (newStatus == StepStatus.IN_PROGRESS
                    && !dependenciesCompleted(plan, step)) {
                return false;
            }

            if (newStatus == StepStatus.COMPLETED
                    && !dependenciesCompleted(plan, step)) {
                return false;
            }

            step.status = newStatus;
            return true;
        }
    }

    public synchronized List<PlanStep> getReadySteps(
            long planId) {

        Plan plan = getPlan(planId);

        if (plan == null) {
            return Collections.emptyList();
        }

        synchronized (plan) {
            List<PlanStep> ready = new ArrayList<>();

            if (plan.cancelled) {
                return Collections.unmodifiableList(ready);
            }

            for (PlanStep step : plan.steps) {
                if (step.status == StepStatus.PENDING
                        && dependenciesCompleted(plan, step)) {
                    ready.add(step);
                }
            }

            Collections.sort(ready,
                    (a, b) -> Integer.compare(
                            b.priority, a.priority));

            return Collections.unmodifiableList(ready);
        }
    }

    public synchronized boolean cancelPlan(long planId) {
        Plan plan = getPlan(planId);

        if (plan == null) {
            return false;
        }

        synchronized (plan) {
            plan.cancelled = true;

            for (PlanStep step : plan.steps) {
                if (step.status == StepStatus.PENDING
                        || step.status == StepStatus.BLOCKED) {
                    step.status = StepStatus.CANCELLED;
                }
            }

            return true;
        }
    }

    private PlanStep findStep(Plan plan, long stepId) {
        for (PlanStep step : plan.steps) {
            if (step.id == stepId) {
                return step;
            }
        }

        return null;
    }

    private boolean dependenciesCompleted(
            Plan plan,
            PlanStep step) {

        for (Long dependencyId : step.dependencies) {
            PlanStep dependency =
                    findStep(plan, dependencyId);

            if (dependency == null
                    || dependency.status != StepStatus.COMPLETED) {
                return false;
            }
        }

        return true;
    }

    public synchronized int getPlanCount() {
        return plans.size();
    }
}
