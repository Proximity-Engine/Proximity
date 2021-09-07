package dev.hephaestus.proximity.cards;

import dev.hephaestus.proximity.text.Symbol;
import dev.hephaestus.proximity.util.OptionContainer;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class Card implements OptionContainer {
    private static final String[] MAIN_TYPES = new String[] {
            "enchantment",
            "artifact",
            "land",
            "creature",
            "conspiracy",
            "instant",
            "phenomenon",
            "plane",
            "planeswalker",
            "scheme",
            "sorcery",
            "tribal",
            "vanguard"
    };

    private final int number;
    private final String name;
    private final List<Symbol> colors;
    private final String type;
    private final URL image;
    private final TypeContainer types;
    private final TextBody oracle;
    private final OptionContainer wrappedOptions;
    private final Set<String> frameEffects;
    private final boolean isFrontFace;
    private final Set<String> keywords;
    private final String layout;
    private final String mainTypes;
    private final TextBody flavorText;
    private Card otherSide;

    public Card(int number, String name, Collection<Symbol> colors, URL image, TypeContainer types, TextBody oracle, String type, OptionContainer options, Set<String> frameEffects, boolean isFrontFace, Card otherSide, Set<String> keywords, String layout, TextBody flavorText) {
        this.number = number;
        this.name = name;
        this.type = type;

        this.colors = new ArrayList<>(colors);
        this.types = types;
        this.image = image;
        this.oracle = oracle;
        this.wrappedOptions = options;
        this.frameEffects = frameEffects;
        this.isFrontFace = isFrontFace;
        this.otherSide = otherSide;
        this.keywords = keywords;
        this.layout = layout;
        this.flavorText = flavorText;

        this.mainTypes = this.type.contains(Character.toString(8212))
                ? this.type.substring(this.type.indexOf(Character.toString(8212)) + 2)
                : Arrays.stream(this.type.split(" ")).filter(s -> Arrays.stream(MAIN_TYPES).anyMatch(s::equalsIgnoreCase)).collect(Collectors.joining(" "));

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

    public void setOtherSide(Card card) {
        this.otherSide = card;
    }

    public final int number() {
        return this.number;
    }

    public final boolean isFrontFace() {
        return this.isFrontFace;
    }

    public final boolean isSingleSided() {
        return this.otherSide == null;
    }

    public final Card getOtherSide() {
        if (this.isSingleSided()) {
            throw new RuntimeException("Cannot get back side of single sided card");
        }

        return this.otherSide;
    }

    public final String getLayout() {
        return this.layout;
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

    public TextBody getOracle() {
        return this.oracle;
    }

    public TextBody getFlavor() {
        return this.flavorText;
    }

    public boolean hasFrameEffect(String effect) {
        return this.frameEffects.contains(effect);
    }

    public boolean hasKeyword(String keyword) {
        return this.keywords.contains(keyword);
    }

    @Override
    public <T> T getOption(String name) {
        return this.wrappedOptions.getOption(name);
    }

    @Override
    public Map<String, Object> getMap() {
        return this.wrappedOptions.getMap();
    }

    public String getMainTypes() {
        return this.mainTypes;
    }

    public record Prototype(String scryfallName, String name, int number, OptionContainer options) implements OptionContainer {
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
