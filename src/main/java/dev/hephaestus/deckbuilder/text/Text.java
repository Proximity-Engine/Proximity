package dev.hephaestus.deckbuilder.text;

import dev.hephaestus.deckbuilder.TextComponent;

public record Text(Style style, TextComponent... components) {
}
