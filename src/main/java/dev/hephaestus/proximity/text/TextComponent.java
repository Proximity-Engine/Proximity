package dev.hephaestus.proximity.text;

public record TextComponent(Style style, String string) {
    public TextComponent(String string) {
        this(null, string);
    }

    public TextComponent(Style style, char c) {
        this(style, "" + c);
    }
}
