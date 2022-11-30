package dev.hephaestus.proximity.app.impl.rendering.properties;

import dev.hephaestus.proximity.app.api.rendering.properties.TextProperty;
import dev.hephaestus.proximity.app.api.rendering.util.Stateful;
import dev.hephaestus.proximity.app.api.text.TextComponent;
import dev.hephaestus.proximity.app.api.text.Word;
import dev.hephaestus.proximity.utils.UnmodifiableIterator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class TextPropertyImpl<D, R extends Stateful> implements TextProperty<D, R>, Stateful {
    private final D data;
    private final R result;

    private final List<BiConsumer<D, Consumer<Word>>> wordGetters = new ArrayList<>();
    private final List<Word> words = new ArrayList<>();

    public TextPropertyImpl(R result, D data) {
        this.data = data;
        this.result = result;
    }

    @Override
    public R add(TextComponent component) {
        Word word = new Word(component);

        this.wordGetters.add((data, consumer) -> consumer.accept(word));

        this.invalidate();

        return this.result;
    }

    @Override
    public R add(String... strings) {
        this.wordGetters.add((data, consumer) -> {
            for (String string : strings) {
                consumer.accept(new Word(new TextComponent(string)));
            }
        });

        this.invalidate();

        return this.result;
    }

    @Override
    public R add(Word word) {
        this.wordGetters.add((data, consumer) -> consumer.accept(word));

        this.invalidate();

        return this.result;
    }

    @Override
    public R add(Function<D, String> wordGetter) {
        this.wordGetters.add((data, consumer) -> consumer.accept(new Word(new TextComponent(wordGetter.apply(data)))));

        this.invalidate();

        return this.result;
    }

    @Override
    public R add(BiConsumer<D, Consumer<Word>> wordConsumer) {
        this.wordGetters.add(wordConsumer);

        this.invalidate();

        return this.result;
    }

    @Override
    public int wordCount() {
        if (this.words.isEmpty() && !this.wordGetters.isEmpty()) {
            for (var getter : this.wordGetters) {
                getter.accept(this.data, this.words::add);
            }
        }

        return this.words.size();
    }

    @NotNull
    @Override
    public Iterator<Word> iterator() {
        if (this.words.isEmpty() && !this.wordGetters.isEmpty()) {
            for (var getter : this.wordGetters) {
                getter.accept(this.data, this.words::add);
            }
        }

        return new UnmodifiableIterator<>(this.words);
    }

    @Override
    public void invalidate() {
        this.words.clear();
        this.result.invalidate();
    }
}
