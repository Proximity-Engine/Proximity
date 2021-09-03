package dev.hephaestus.deckbuilder.cards;

import dev.hephaestus.deckbuilder.templates.Template;
import dev.hephaestus.deckbuilder.text.Symbol;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Card {
    private final String name;
    private final List<Symbol> colors;
    private final String type;
    private final URL image;
    private final TypeContainer types;
    private final OracleText oracle;

    public Card(Template template, String type, Collection<Symbol> colors, URL image, TypeContainer types, OracleText oracle, String name) {
        this.name = name;
        this.type = type;

        this.colors = new ArrayList<>(colors);
        this.types = types;
        this.image = image;
        this.oracle = oracle;

        this.colors.sort((c1, c2) -> switch (c1.glyphs()) {
            case "W" -> switch (c2.glyphs()) {
                case "U", "B" -> -1;
                case "R", "G" -> 1;
                default -> 0;
            };
            case "U" -> switch (c2.glyphs()) {
                case "B", "R" -> -1;
                case "W", "G" -> 1;
                default -> 0;
            };
            case "B" -> switch (c2.glyphs()) {
                case "R", "G" -> -1;
                case "W", "U" -> 1;
                default -> 0;
            };
            case "G" -> switch (c2.glyphs()) {
                case "W", "U" -> -1;
                case "B", "R" -> 1;
                default -> 0;
            };
            case "R" -> switch (c2.glyphs()) {
                case "G", "W" -> -1;
                case "B", "U" -> 1;
                default -> 0;
            };
            default -> 0;
        });
    }

    public final String name() {
        return this.name;
    }

    public final List<Symbol> colors() {
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

    public OracleText getOracle() {
        return this.oracle;
    }
}
