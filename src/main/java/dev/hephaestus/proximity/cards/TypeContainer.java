package dev.hephaestus.proximity.cards;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;

public class TypeContainer {
    private final Collection<String> types = new HashSet<>();

    public TypeContainer(String... types) {
        this.types.addAll(Arrays.asList(types));
    }

    public TypeContainer(Collection<String> types) {
        this.types.addAll(types);
    }

    public boolean has(String type) {
        return this.types.contains(type.toLowerCase(Locale.ROOT));
    }
}
