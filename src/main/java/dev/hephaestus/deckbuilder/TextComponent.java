package dev.hephaestus.deckbuilder;

import dev.hephaestus.deckbuilder.text.Style;

public record TextComponent(Style style, String string) {
    public TextComponent(String string) {
        this(null, string);
    }
}
