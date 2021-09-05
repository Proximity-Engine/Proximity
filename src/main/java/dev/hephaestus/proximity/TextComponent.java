package dev.hephaestus.proximity;

import dev.hephaestus.proximity.text.Style;

public record TextComponent(Style style, String string) {
    public TextComponent(String string) {
        this(null, string);
    }

    public TextComponent(Style style, char c) {
        this(style, "" + c);
    }
}
