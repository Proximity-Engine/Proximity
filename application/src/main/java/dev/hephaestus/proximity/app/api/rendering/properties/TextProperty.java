package dev.hephaestus.proximity.app.api.rendering.properties;

import dev.hephaestus.proximity.app.api.text.TextComponent;
import dev.hephaestus.proximity.app.api.rendering.util.Stateful;
import dev.hephaestus.proximity.app.api.text.Word;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface TextProperty<D, R extends Stateful> extends Iterable<Word> {
    /**
     * Adds a word consisting of a single {@link TextComponent}
     *
     * @param component a single component to add
     * @return some value
     */
    R add(TextComponent component);

    /**
     * Adds each string as a word consisting of a single {@link TextComponent} with the given string and no styling
     *
     * @param strings any number of strings to be added
     * @return some value
     */
    R add(String... strings);

    R add(Word word);

    R add(Function<D, String> wordGetter);

    /**
     * @param wordConsumer a {@link BiConsumer} that can add any number of {@link Word}s to add to this property via the passed {@link Consumer}
     * @return some value
     */
    R add(BiConsumer<D, Consumer<Word>> wordConsumer);

    int wordCount();
}
