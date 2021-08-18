package dev.hephaestus.deckbuilder.templates;

import dev.hephaestus.deckbuilder.cards.Card;

import java.awt.*;
import java.util.function.Function;

public interface Layer {
    Layer EMPTY = new Empty();

    void draw(Graphics2D out);

    interface Factory {
        Function<Card, Layer> create(Template template);
    }

    class Empty implements Layer {
        @Override
        public void draw(Graphics2D out) {

        }
    }
}