package dev.hephaestus.proximity.api;

import dev.hephaestus.proximity.api.json.JsonArray;
import dev.hephaestus.proximity.api.json.JsonObject;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.function.BiFunction;
import java.util.function.Function;

public class Value<T> {
    private final String[] key;
    private final BiFunction<JsonObject, String[], T> getter;
    private final TriConsumer<JsonObject, String[], T> setter;

    private Value(String[] key, BiFunction<JsonObject, String[], T> getter, TriConsumer<JsonObject, String[], T> setter) {
        this.key = join("proximity", key);
        this.setter = setter;
        this.getter = getter;
    }

    public T get(JsonObject object) {
        return this.getter.apply(object, this.key);
    }

    public void set(JsonObject object, T value) {
        this.setter.accept(object, this.key, value);
    }

    public boolean exists(JsonObject object) {
        return object.has(this.key);
    }

    private static String[] join(String first, String... more) {
        String[] result = new String[more.length + 1];
        result[0] = first;

        System.arraycopy(more, 0, result, 1, more.length);

        return result;
    }

    public static Value<String> createString(String key0, String... keys) {
        return new Value<>(join(key0, keys), JsonObject::getAsString, JsonObject::add);
    }

    public static Value<Integer> createInteger(String key0, String... keys) {
        return new Value<>(join(key0, keys), JsonObject::getAsInt, JsonObject::add);
    }

    public static Value<Boolean> createBoolean(String key0, String... keys) {
        return new Value<>(join(key0, keys), JsonObject::getAsBoolean, JsonObject::add);
    }

    public static Value<JsonObject> createObject(String key0, String... keys) {
        return new Value<>(join(key0, keys), JsonObject::getAsJsonObject, JsonObject::add);
    }

    public static Value<JsonArray> createArray(String key0, String... keys) {
        return new Value<>(join(key0, keys), JsonObject::getAsJsonArray, JsonObject::add);
    }

    public static <E extends Enum<E>> Value<E> createEnum(Function<String, E> enumValueGetter, String key0, String... keys) {
        return new Value<>(join(key0, keys), (object, key) ->
                enumValueGetter.apply(object.getAsString(key)),
                (object, key, value) -> object.add(key, value.name())
        );
    }
}
