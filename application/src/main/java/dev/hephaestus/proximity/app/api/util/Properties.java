package dev.hephaestus.proximity.app.api.util;

import javafx.beans.property.ListProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import java.util.List;

public final class Properties {
    private Properties() {
    }

    public static <T> Property<T> of(T value) {
        return new SimpleObjectProperty<>(value);
    }

    public static <T> Property<T> of(String name) {
        return new SimpleObjectProperty<>(null, name);
    }

    public static <T> Property<T> of(String name, T value) {
        return new SimpleObjectProperty<>(null, name, value);
    }

    public static <T> ListProperty<T> list() {
        return new SimpleListProperty<>(FXCollections.observableArrayList());
    }

    public static <T> ListProperty<T> list(String name) {
        return new SimpleListProperty<>(null, name, FXCollections.observableArrayList());
    }

    public static <T> ListProperty<T> list(String name, T... initialValues) {
        return new SimpleListProperty<>(null, name, FXCollections.observableArrayList(initialValues));
    }

    public static <T> ListProperty<T> list(String name, List<T> initialValues) {
        return new SimpleListProperty<>(null, name, FXCollections.observableArrayList(initialValues));
    }
}
