package dev.hephaestus.deckbuilder.templates;

import dev.hephaestus.deckbuilder.cards.Card;

public interface LayerFactory {
    Layer create(Card card, int x, int y);
}
