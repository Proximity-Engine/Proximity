package dev.hephaestus.proximity.app.api.util;

import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Result<T> {
    private final boolean isError;
    private final T value;
    private final ImmutableList<Error> errors;

    protected Result(T value) {
        this.isError = false;
        this.value = value;
        this.errors = null;
    }

    protected Result(ImmutableList<Error> errors) {
        this.isError = true;
        this.value = null;
        this.errors = errors;
    }

    protected Result(Error error1, Error... errors) {
        this(ImmutableList.<Error>builder().add(error1).add(errors).build());
    }

    public boolean isError() {
        return this.isError;
    }

    public boolean isOk() {
        return !this.isError;
    }

    public T get() {
        if (!this.isOk()) {
            throw new UnsupportedOperationException("Cannot get value of Error Result");
        }

        return this.value;
    }

    public ImmutableList<Error> getErrors() {
        if (!this.isError()) {
            throw new UnsupportedOperationException("Cannot get error messages for Ok Result");
        }

        return this.errors;
    }

    @SuppressWarnings("unchecked")
    public <N> Result<N> then(Function<T, Result<N>> step) {
        return this.isError() ? (Result<N>) this : step.apply(this.value);
    }

    @SuppressWarnings("unchecked")
    public <N> Result<N> then(Supplier<Result<N>> step) {
        return this.isError() ? (Result<N>) this : step.get();
    }

    @SuppressWarnings("unchecked")
    public <N1, N2> Result<N2> then(Function<T, Result<N1>> step1, BiFunction<T, N1, Result<N2>> step2) {
        if (this.isError()) {
            return (Result<N2>) this;
        } else {
            Result<N1> result = step1.apply(this.value);

            return result.isError()
                    ? (Result<N2>) this
                    : step2.apply(this.value, result.value);
        }
    }

    public Result<T> ifPresent(Consumer<T> consumer) {
        if (!this.isError()) {
            consumer.accept(this.value);
        }

        return this;
    }

    public void ifError(Consumer<ImmutableList<Error>> errorConsumer) {
        if (this.isError()) {
            errorConsumer.accept(this.errors);
        }
    }

    @SuppressWarnings("unchecked")
    public <V> Result<V> unwrap() {
        if (this.value instanceof Result<?>) {
            return ((Result<?>) this.value).unwrap();
        } else {
            return (Result<V>) this;
        }
    }

    public T orElse(T value) {
        return this.isError ? value : this.value;
    }

    /**
     * Constructs a new error result with the given error message in addition to our existing errors
     */
    public <T2> Result<T2> error1(String message) {
        return new Result<>(ImmutableList.<Error>builder()
                .add(new Error(message, getStackTrace()))
                .addAll(this.errors).build()
        );
    }

    /**
     * Constructs a new error result with the given error message in addition to our existing errors
     */
    public <T2> Result<T2> error1(String message, Object... args) {
        return new Result<>(ImmutableList.<Error>builder()
                .add(new Error(String.format(message, args), getStackTrace()))
                .addAll(this.errors).build()
        );
    }

    protected static ImmutableList<StackTraceElement> getStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        ImmutableList.Builder<StackTraceElement> builder = ImmutableList.builder();

        boolean escaped = false;
        boolean encounteredResult = false;

        for (StackTraceElement stackTraceElement : stackTrace) {
            if (!stackTraceElement.getClassName().equals(Result.class.getName()) && encounteredResult) {
                escaped = true;
            } else if (stackTraceElement.getClassName().equals(Result.class.getName())) {
                encounteredResult = true;
            }

            if (escaped) {
                builder.add(stackTraceElement);
            }
        }

        return builder.build();
    }

    public static <T> Result<T> error(String message, Object... args) {
        Objects.requireNonNull(message);

        return new Result<T>(new Error(args.length == 0 ? message : String.format(message, args), getStackTrace()));
    }

    public static <T> Result<T> error(Iterable<Error> cause, String message, Object... args) {
        Objects.requireNonNull(message);

        return new Result<>(ImmutableList.<Error>builder()
                .add(new Error(args.length == 0 ? message : String.format(message, args), getStackTrace()))
                .addAll(() -> {
                    var itr = cause.iterator();

                    return new Iterator<>() {
                        @Override
                        public boolean hasNext() {
                            return itr.hasNext();
                        }

                        @Override
                        public Error next() {
                            return itr.next().indent();
                        }
                    };
                })
                .build());
    }

    public static <T> Result<T> of(T value) {
        return new Result<>(value);
    }

    public static Result<Void> ok() {
        return Result.of(null);
    }
}
