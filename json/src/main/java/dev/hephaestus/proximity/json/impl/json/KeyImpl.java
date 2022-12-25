package dev.hephaestus.proximity.json.impl.json;

import dev.hephaestus.proximity.json.api.JsonKey;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class KeyImpl implements JsonKey, Iterable<Object> {
    private final Object[] parts;

    public KeyImpl(Object[] parts) {
        this.parts = parts;
    }

    @NotNull
    @Override
    public Iterator<Object> iterator() {
        return new Iterator<>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < parts.length;
            }

            @Override
            public Object next() {
                return parts[i++];
            }
        };
    }
}
