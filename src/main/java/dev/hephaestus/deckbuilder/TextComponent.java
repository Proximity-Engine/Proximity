package dev.hephaestus.deckbuilder;

public record TextComponent(Integer color, String string) {
    public TextComponent(String string) {
        this(null, string);
    }
}
