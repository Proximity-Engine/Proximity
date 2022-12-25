package dev.hephaestus.proximity.app.api.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Result<T> {
    private final boolean isError;
    private final T value;
    private final List<Error> errors;

    protected Result(T value) {
        this.isError = false;
        this.value = value;
        this.errors = null;
    }

    protected Result(List<Error> errors) {
        this.isError = true;
        this.value = null;
        this.errors = errors;
    }

    protected Result(Error error1, Error... errors) {
        this.isError = true;
        this.value = null;
        this.errors = new ArrayList<>(errors.length + 1);

        this.errors.add(error1);
        this.errors.addAll(Arrays.asList(errors));
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

    public List<Error> getErrors() {
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

    public void ifError(Consumer<List<Error>> errorConsumer) {
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
        List<Error> errors = new ArrayList<>(this.errors);

        errors.add(0, new Error(message, getStackTrace()));

        return new Result<>(errors);
    }

    /**
     * Constructs a new error result with the given error message in addition to our existing errors
     */
    public <T2> Result<T2> error1(String message, Object... args) {
        List<Error> errors = new ArrayList<>(this.errors);

        errors.add(0, new Error(String.format(message, args), getStackTrace()));

        return new Result<>(errors);
    }

    protected static List<StackTraceElement> getStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        List<StackTraceElement> elements = new ArrayList<>();

        boolean escaped = false;
        boolean encounteredResult = false;

        for (StackTraceElement stackTraceElement : stackTrace) {
            if (!stackTraceElement.getClassName().equals(Result.class.getName()) && encounteredResult) {
                escaped = true;
            } else if (stackTraceElement.getClassName().equals(Result.class.getName())) {
                encounteredResult = true;
            }

            if (escaped) {
                elements.add(stackTraceElement);
            }
        }

        return elements;
    }

    public static <T> Result<T> error(String message, Object... args) {
        Objects.requireNonNull(message);

        return new Result<T>(new Error(args.length == 0 ? message : String.format(message, args), getStackTrace()));
    }

    public static <T> Result<T> error(Iterable<Error> cause, String message, Object... args) {
        Objects.requireNonNull(message);

        List<Error> errors = new ArrayList<>();

        errors.add(new Error(args.length == 0 ? message : String.format(message, args), getStackTrace()));

        for (Error error : cause) {
            errors.add(error.indent());
        }

        return new Result<>(errors);
    }

    public static <T> Result<T> of(T value) {
        return new Result<>(value);
    }

    public static Result<Void> ok() {
        return Result.of(null);
    }
}
