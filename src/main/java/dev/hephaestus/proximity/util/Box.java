package dev.hephaestus.proximity.util;

public class Box<T> {
    private T value;

    public Box(T value) {
        this.value = value;
    }

    public Box() {
        this(null);
    }

    public void set(T value) {
        this.value = value;
    }

    public T get() {
        return this.value;
    }
}
