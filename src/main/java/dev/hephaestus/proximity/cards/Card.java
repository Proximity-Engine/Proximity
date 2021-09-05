package dev.hephaestus.proximity.cards;

import dev.hephaestus.proximity.text.Symbol;
import dev.hephaestus.proximity.util.OptionContainer;

import java.net.URL;
import java.util.*;

public class Card implements OptionContainer {
    private final int number;
    private final String name;
    private final List<Symbol> colors;
    private final String type;
    private final URL image;
    private final TypeContainer types;
    private final OracleText oracle;
    private final OptionContainer wrappedOptions;
    private final Set<String> frameEffects;

    public Card(int number, String name, Collection<Symbol> colors, URL image, TypeContainer types, OracleText oracle, String type, OptionContainer options, Set<String> frameEffects) {
        this.number = number;
        this.name = name;
        this.type = type;

        this.colors = new ArrayList<>(colors);
        this.types = types;
        this.image = image;
        this.oracle = oracle;
        this.wrappedOptions = options;
        this.frameEffects = frameEffects;

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

    public final int number() {
        return this.number;
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

    public boolean hasFrameEffect(String effect) {
        return this.frameEffects.contains(effect);
    }

    @Override
    public <T> T getOption(String name) {
        return this.wrappedOptions.getOption(name);
    }

    @Override
    public Map<String, Object> getMap() {
        return this.wrappedOptions.getMap();
    }

    public record Prototype(String name, int number, OptionContainer options) implements OptionContainer {
        @Override
        public <T> T getOption(String name) {
            return this.options.getOption(name);
        }

        @Override
        public Map<String, Object> getMap() {
            return this.options.getMap();
        }
    }
}
