package dev.hephaestus.proximity.util;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Result<T> {
    private final boolean isError;
    private final T value;
    private final String error;

    private Result(boolean isError, T value, String error) {
        this.isError = isError;
        this.value = value;
        this.error = error;
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

    public String getError() {
        if (!this.isError()) {
            throw new UnsupportedOperationException("Cannot get error message for Ok Result");
        }

        return this.error;
    }

    public <N> Result<N> then(Function<T, Result<N>> step) {
        return this.isError() ? Result.error(this.error) : step.apply(this.value);
    }

    public <N> Result<N> then(Supplier<Result<N>> step) {
        return this.isError() ? Result.error(this.error) : step.get();
    }

    public <N1, N2> Result<N2> then(Function<T, Result<N1>> step1, BiFunction<T, N1, Result<N2>> step2) {
        if (this.isError()) {
            return Result.error(this.error);
        } else {
            Result<N1> result = step1.apply(this.value);

            return result.isError()
                    ? Result.error(result.error)
                    : step2.apply(this.value, result.value);
        }
    }

    public Result<T> ifPresent(Consumer<T> consumer) {
        if (!this.isError()) {
            consumer.accept(this.value);
        }

        return this;
    }

    public Result<T> ifError(Consumer<String> errorConsumer) {
        if (this.isError()) {
            errorConsumer.accept(this.error);
        }

        return this;
    }

    @SuppressWarnings("unchecked")
    public <V> Result<V> unwrap() {
        if (this.isError()) {
            return Result.error(this.error);
        } else if (this.value instanceof Result<?> result) {
            return result.unwrap();
        } else {
            return (Result<V>) this;
        }
    }

    public static <T> Result<T> error(String message, Object... args) {
        Objects.requireNonNull(message);

        return new Result<>(true, null, String.format(message, args));
    }

    public static <T> Result<T> of(T value) {
        return new Result<>(false, value, null);
    }
}
