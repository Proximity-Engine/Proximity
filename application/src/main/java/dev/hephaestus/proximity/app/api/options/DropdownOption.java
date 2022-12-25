package dev.hephaestus.proximity.app.api.options;

import dev.hephaestus.proximity.app.api.Option;
import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.json.api.Json;
import dev.hephaestus.proximity.json.api.JsonElement;
import dev.hephaestus.proximity.json.api.JsonString;
import javafx.beans.property.Property;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class DropdownOption<T, D extends RenderJob<?>> extends Option<T, DropdownOption<T, D>.Widget, D> {
    private final List<Entry<T, D>> entries;
    private final Map<Entry<T, D>, T> producedValues;
    private final Map<T, Entry<T, D>> producedValuesInverse;
    private final Map<String, T> producedValuesByName;
    private final Map<T, D> valuesToData;

    private DropdownOption(String id, Function<D, T> defaultValue, List<Entry<T, D>> entries) {
        super(id, defaultValue);
        this.entries = entries;
        this.producedValues = new HashMap<>(entries.size());
        this.producedValuesInverse = new HashMap<>(entries.size());
        this.producedValuesByName = new HashMap<>(entries.size());
        this.valuesToData = new HashMap<>(entries.size());
    }

    @Override
    public Widget createControl(D renderJob) {
        Widget widget = new Widget();

        T defaultValue = this.getDefaultValue(renderJob);

        for (var entry : this.entries) {
            T value = entry.value.apply(renderJob);

            this.producedValues.put(entry, value);
            this.producedValuesInverse.put(value, entry);
            this.producedValuesByName.put(entry.getName(renderJob), value);
            this.valuesToData.put(value, renderJob);

            widget.getItems().add(value);

            if (value == null && defaultValue == null || value != null && value.equals(defaultValue)) {
                widget.setValue(value);
            }
        }

        widget.setConverter(new StringConverter<>() {
            @Override
            public String toString(T t) {
                return DropdownOption.this.producedValuesInverse.get(t).getName(
                        DropdownOption.this.valuesToData.get(t)
                );
            }

            @Override
            public T fromString(String string) {
                return DropdownOption.this.producedValuesByName.get(string);
            }
        });

        return widget;
    }

    public abstract String toString(T t);

    public class Widget extends ComboBox<T> implements Option.Widget<T> {
        @Override
        public Property<T> getValueProperty() {
            return this.valueProperty();
        }
    }

    public static <T, D extends RenderJob<?>> Builder<T, D> builder(String id, Function<T, String> stringFunction, Function<T, JsonElement> toJson, Function<JsonElement, T> fromJson) {
        return new Builder<>(id, stringFunction, toJson, fromJson);
    }

    public static <D extends RenderJob<?>> Builder<String, D> builder(String id) {
        return new Builder<>(id, s -> s, Json::create, JsonString::get);
    }

    public static final class Builder<T, D extends RenderJob<?>> {
        private final String id;
        private final Function<T, String> stringFunction;
        private final Function<T, JsonElement> toJson;
        private final Function<JsonElement, T> fromJson;

        private final List<Entry<T, D>> values = new LinkedList<>();

        private Builder(String id, Function<T, String> stringFunction, Function<T, JsonElement> toJson, Function<JsonElement, T> fromJson) {
            this.id = id;

            this.stringFunction = stringFunction;
            this.toJson = toJson;
            this.fromJson = fromJson;
        }

        public Builder<T, D> add(Function<D, T> value, Predicate<D> predicate) {
            this.values.add(new Entry<>(value, predicate, data -> this.stringFunction.apply(value.apply(data))));

            return this;
        }

        public Builder<T, D> add(T value, Predicate<D> predicate) {
            return this.add(d -> value, predicate);
        }

        public Builder<T, D> add(T value) {
            return this.add(value, d -> false);
        }

        public Builder<T, D> add(String name, T value) {
            this.values.add(new Entry<>(name, d -> value, d -> false));

            return this;
        }

        public Builder<T, D> add(String name, T value, Predicate<D> predicate) {
            this.values.add(new Entry<>(name, d -> value, predicate));

            return this;
        }

        public DropdownOption<T, D> build() {
            List<Entry<T, D>> entries = new ArrayList<>(this.values);

            Function<D, T> defaultFunction = data -> {
                for (var entry : entries) {
                    if (entry.predicate.test(data)) {
                        return entry.value.apply(data);
                    }
                }

                return null;
            };


            return new DropdownOption<>(this.id, defaultFunction, entries) {
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

    private record Entry<T, D extends RenderJob<?>>(String name, Function<D, T> value, Predicate<D> predicate, Function<D, String> stringFunction) {
        public Entry(Function<D, T> value, Predicate<D> predicate, Function<D, String> stringFunction) {
            this(null, value, predicate, stringFunction);
        }

        public Entry(String name, Function<D, T> value, Predicate<D> predicate) {
            this(name, value, predicate, null);
        }

        public String getName(D data) {
            return this.name == null ? this.stringFunction.apply(data) : this.name;
        }
    }
}
