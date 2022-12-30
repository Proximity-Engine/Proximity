package dev.hephaestus.proximity.app.api.util;

import dev.hephaestus.proximity.app.api.logging.Log;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Task {
    private final List<Function<?, ?>> steps;

    protected final String name;
    protected final Log log;

    private Thread currentExecution;
    private int executions;

    public Task(String name, Log log) {
        this.name = name;
        this.log = log;

        this.steps = this.addSteps(builder()).build();
    }

    protected abstract Builder<?> addSteps(Builder<Void> builder);

    protected final <T> T interrupt() {
        if (this.currentExecution != null) {
            this.currentExecution.interrupt();

            this.doFinally();
        }

        return null;
    }

    protected final <T> T interrupt(String message, Object... args) {
        if (this.currentExecution != null) {
            this.currentExecution.interrupt();
            this.log.print(message, args);

            this.doFinally();
        }

        return null;
    }

    protected final <T> T interrupt(Throwable throwable) {
        if (this.currentExecution != null) {
            this.currentExecution.interrupt();

            if (!(throwable.getClass().equals(RuntimeException.class))) {
                this.log.print(throwable);
            }

            this.doFinally();
        }

        return null;
    }

    /**
     * Can be used by tasks to do any cleanup necessary.
     */
    protected void beforeInterrupt() {

    }

    protected void doFinally() {

    }

    public void run() {
        if (this.currentExecution != null) {
            this.currentExecution.interrupt();
        }

        this.currentExecution = new Thread(() -> {
            Thread thread = Thread.currentThread();
            Object value = null;
            Iterator<Function<?, ?>> itr = this.steps.iterator();

            while (itr.hasNext() && !thread.isInterrupted()) {
                value = run(itr.next(), value);
            }

            if (!thread.isInterrupted()) {
                this.doFinally();
            }
        });

        this.currentExecution.setName(this.name + "-thread-" + this.executions++);
        this.currentExecution.setDaemon(true);
        this.currentExecution.start();
    }

    private static <T, R> R run(Function<T, R> step, Object value) {
        //noinspection unchecked
        return step.apply((T) value);
    }

    private static Task.Builder<Void> builder() {
        return new Builder<>(Collections.emptyList());
    }

    public static final class Builder<T> {
        private final List<Function<?, ?>> steps;

        private Builder(List<Function<?, ?>> steps) {
            this.steps = steps;
        }

        public <R> Builder<R> then(Function<T, R> step) {
            Builder<R> builder = this.copy();

            builder.steps.add(step);

            return builder;
        }

        public Builder<Void> then(Consumer<T> step) {
            Builder<Void> builder = this.copy();

            builder.steps.add(t -> {
                //noinspection unchecked
                step.accept((T) t);

                return null;
            });

            return builder;
        }

        public Builder<Void> then(Runnable step) {
            Builder<Void> builder = this.copy();

            builder.steps.add(t -> {
                step.run();

                return null;
            });

            return builder;
        }

        public <R> Builder<R> then(Supplier<R> step) {
            Builder<R> builder = this.copy();

            builder.steps.add(t -> step.get());

            return builder;
        }

        public <R> Builder<R> branch(Function<T, Function<T, R>> selector) {
            Builder<R> builder = this.copy();

            builder.steps.add(t -> {
                //noinspection unchecked
                return selector.apply((T) t).apply((T) t);
            });

            return builder;
        }

        private <R> Builder<R> copy() {
            return new Builder<>(new LinkedList<>(this.steps));
        }

        public List<Function<?, ?>> build() {
            return new ArrayList<>(this.steps);
        }
    }
}
