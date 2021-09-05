package dev.hephaestus.proximity.templates;

import dev.hephaestus.proximity.cards.Card;

public interface LayerFactory {
    Layer create(Card card, int x, int y);
}
