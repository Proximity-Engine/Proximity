package dev.hephaestus.proximity.util;

public class Box<T> {
    private T value;

    public Box(T value) {
        this.value = value;
    }

    public Box<T> set(T value) {
        this.value = value;
        return this;
    }

    public T get() {
        return this.value;
    }
}
