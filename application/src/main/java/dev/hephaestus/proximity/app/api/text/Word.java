package dev.hephaestus.proximity.app.api.text;

import dev.hephaestus.proximity.utils.UnmodifiableIterator;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class Word implements Iterable<TextComponent> {
    private final List<TextComponent> components;

    public Word(TextComponent... components) {
        this.components = Arrays.asList(components);
    }

    public Word(Collection<TextComponent> components) {
        this.components = new ArrayList<>(components);
    }

    public int length() {
        return this.components.size();
    }

    @NotNull
    @Override
    public Iterator<TextComponent> iterator() {
        return new UnmodifiableIterator<>(this.components);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Word word && word.components.equals(this.components);
    }
}
