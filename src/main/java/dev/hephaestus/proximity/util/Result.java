package dev.hephaestus.proximity.util;

import java.util.Objects;

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

    public static <T> Result<T> error(String message) {
        Objects.requireNonNull(message);

        return new Result<>(null, message);
    }

    public static <T> Result<T> of(T value) {
        Objects.requireNonNull(value);

        return new Result<>(value, null);
    }
}
