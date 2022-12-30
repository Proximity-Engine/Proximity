package dev.hephaestus.proximity.app.api.rendering;

import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;

import java.util.function.Supplier;

public interface Attribute<T> extends ObservableValue<T> {
    void set(T value);

    T get();
    void bind(ObservableValue<? extends T> observableValue);
    void bind(Supplier<T> valueSupplier, Observable... dependencies);
}
