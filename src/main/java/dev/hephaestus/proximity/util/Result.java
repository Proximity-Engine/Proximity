package dev.hephaestus.proximity.util;

import dev.hephaestus.proximity.cards.Predicate;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class Result<T> {
    private final T value;
    private final String error;

    private Result(T value, String error) {
        this.value = value;
        this.error = error;
    }

    public boolean isError() {
        return this.value == null;
    }

    public T get() {
        if (this.value == null) {
            throw new UnsupportedOperationException();
        }

        return this.value;
    }

    public String getError() {
        if (this.value != null) {
            throw new UnsupportedOperationException();
        }

        return this.error;
    }

    public <N> Result<N> then(Function<T, Result<N>> step) {
        return this.isError() ? Result.error(this.error) : step.apply(this.value);
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

    public static <T> Result<T> error(String message, Object... args) {
        Objects.requireNonNull(message);

        return new Result<>(null, String.format(message, args));
    }

    public static <T> Result<T> of(T value) {
        Objects.requireNonNull(value);

        return new Result<>(value, null);
    }
}
