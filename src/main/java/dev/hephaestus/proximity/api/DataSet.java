package dev.hephaestus.proximity.api;

import dev.hephaestus.proximity.api.json.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class DataSet implements Iterable<JsonObject> {
    private final List<JsonObject> wrapped;

    public DataSet(List<JsonObject> wrapped) {
        this.wrapped = wrapped;
    }

    public DataSet(JsonObject... initialValues) {
        this.wrapped = new ArrayList<>(Arrays.asList(initialValues));
    }

    public void add(JsonObject... objects) {
        Collections.addAll(this.wrapped, objects);
    }

    public void remove(JsonObject... objects) {
        for (JsonObject object : objects) {
            this.wrapped.remove(object);
        }
    }

    public void replace(JsonObject oldObject, JsonObject newObject) {
        for (int i = 0; i < oldObject.size(); ++i) {
            if (this.wrapped.get(i) == oldObject) {
                this.wrapped.set(i, newObject);
            }
        }
    }

    @NotNull
    @Override
    public Iterator<JsonObject> iterator() {
        return this.wrapped.iterator();
    }
}
