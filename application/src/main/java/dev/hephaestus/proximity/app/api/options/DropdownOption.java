package dev.hephaestus.proximity.app.api.options;

import dev.hephaestus.proximity.app.api.Option;
import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.json.api.Json;
import dev.hephaestus.proximity.json.api.JsonElement;
import javafx.beans.property.Property;
import javafx.scene.Node;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class DropdownOption<T, D extends RenderJob> extends Option<T, DropdownOption<T, D>.Widget, D> {
    public DropdownOption(String id, T defaultValue) {
        super(id, defaultValue);
    }

    public DropdownOption(String id, Function<D, T> defaultValue) {
        super(id, defaultValue);
    }

    @Override
    public Widget createControl(D renderJob) {
        return new Widget();
    }

    public abstract String toString(T t);

    public class Widget extends Node implements Option.Widget<T> {
        @Override
        public Property<T> getValueProperty() {
            return null;
        }
    }

    public static <T, D extends RenderJob> Builder<T, D> builder(String id, Function<T, String> stringFunction, Function<T, JsonElement> toJson, Function<JsonElement, T> fromJson) {
        return new Builder<>(id, stringFunction, toJson, fromJson);
    }

    public static <D extends RenderJob> Builder<String, D> builder(String id) {
        return new Builder<>(id, s -> s, Json::create, JsonElement::asString);
    }

    public static final class Builder<T, D extends RenderJob> {
        private final String id;
        private final Function<T, String> stringFunction;
        private final Function<T, JsonElement> toJson;
        private final Function<JsonElement, T> fromJson;

        private final List<Pair<Function<D, T>, Predicate<D>>> values = new LinkedList<>();

        private Builder(String id, Function<T, String> stringFunction, Function<T, JsonElement> toJson, Function<JsonElement, T> fromJson) {
            this.id = id;

            this.stringFunction = stringFunction;
            this.toJson = toJson;
            this.fromJson = fromJson;
        }

        public Builder<T, D> add(Function<D, T> value, Predicate<D> predicate) {
            this.values.add(new Pair<>(value, predicate));

            return this;
        }

        public Builder<T, D> add(T value, Predicate<D> predicate) {
            return this.add(d -> value, predicate);
        }

        public Builder<T, D> add(T value) {
            return this.add(value, d -> false);
        }

        public DropdownOption<T, D> build() {
            List<Pair<Function<D, T>, Predicate<D>>> values = new ArrayList<>(this.values);

            Function<D, T> defaultFunction = data -> {
                for (var pair : values) {
                    if (pair.getValue().test(data)) {
                        return pair.getKey().apply(data);
                    }
                }

                return null;
            };

            return new DropdownOption<>(this.id, defaultFunction) {
                @Override
                public String toString(T t) {
                    return t == null ? "" : Builder.this.stringFunction.apply(t);
                }

                @Override
                public JsonElement toJson(T value) {
                    return Builder.this.toJson.apply(value);
                }

                @Override
                public T fromJson(JsonElement json) {
                    return Builder.this.fromJson.apply(json);
                }
            };
        }
    }
}
