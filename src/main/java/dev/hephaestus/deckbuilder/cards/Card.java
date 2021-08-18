package dev.hephaestus.deckbuilder.cards;

import dev.hephaestus.deckbuilder.TextComponent;
import dev.hephaestus.deckbuilder.text.Style;
import dev.hephaestus.deckbuilder.text.Text;

import java.net.URL;
import java.util.List;

public class Card {
    private final Text name;
    private final List<Color> colors;
    private final Text type;
    private final URL image;
    private final TypeContainer types;

    public Card(String name, String type, List<Color> colors, URL image, TypeContainer types) {
        this.name = new Text(new Style.Builder()
                .size(160F)
                .color(0x000000)
                .build(), new TextComponent(name));

        this.type = new Text(new Style.Builder()
                .size(130F)
                .color(0x000000)
                .build(), new TextComponent(type));

        this.colors = colors;
        this.types = types;
        this.image = image;

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

    public final Text name() {
        return this.name;
    }

    public final List<Color> colors() {
        return this.colors;
    }

    public final Text type() {
        return this.type;
    }

    public final URL image() {
        return this.image;
    }

    public TypeContainer getTypes() {
        return this.types;
    }
}
