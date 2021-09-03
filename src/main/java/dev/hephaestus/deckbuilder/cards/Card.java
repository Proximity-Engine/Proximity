package dev.hephaestus.deckbuilder.cards;

import dev.hephaestus.deckbuilder.TextComponent;
import dev.hephaestus.deckbuilder.templates.Template;

import java.net.URL;
import java.util.List;

public class Card {
    private final String name;
    private final List<Color> colors;
    private final String type;
    private final URL image;
    private final TypeContainer types;
    private final List<List<TextComponent>> oracle;

    public Card(Template template, String type, List<Color> colors, URL image, TypeContainer types, List<List<TextComponent>> oracle, String name) {
        this.name = name;
        this.type = type;

        this.colors = colors;
        this.types = types;
        this.image = image;
        this.oracle = oracle;

        colors.sort((c1, c2) -> switch (c1.symbol()) {
            case 'W' -> switch (c2.symbol()) {
                case 'U', 'B' -> -1;
                case 'R', 'G' -> 1;
                default -> 0;
            };
            case 'U' -> switch (c2.symbol()) {
                case 'B', 'R' -> -1;
                case 'W', 'G' -> 1;
                default -> 0;
            };
            case 'B' -> switch (c2.symbol()) {
                case 'R', 'G' -> -1;
                case 'W', 'U' -> 1;
                default -> 0;
            };
            case 'G' -> switch (c2.symbol()) {
                case 'W', 'U' -> -1;
                case 'B', 'R' -> 1;
                default -> 0;
            };
            case 'R' -> switch (c2.symbol()) {
                case 'G', 'W' -> -1;
                case 'B', 'U' -> 1;
                default -> 0;
            };
            default -> 0;
        });
    }

    public final String name() {
        return this.name;
    }

    public final List<Color> colors() {
        return this.colors;
    }

    public final String type() {
        return this.type;
    }

    public final URL image() {
        return this.image;
    }

    public TypeContainer getTypes() {
        return this.types;
    }

    public List<List<TextComponent>> getOracle() {
        return this.oracle;
    }
}
