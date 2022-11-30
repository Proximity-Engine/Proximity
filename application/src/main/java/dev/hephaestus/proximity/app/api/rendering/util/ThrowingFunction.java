package dev.hephaestus.proximity.app.api.rendering.util;

public interface ThrowingFunction<T, R, E extends Exception> {
    R apply(T t) throws E;
}
