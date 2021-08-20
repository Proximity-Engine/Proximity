package dev.hephaestus.deckbuilder.cards;

import dev.hephaestus.deckbuilder.TextComponent;
import dev.hephaestus.deckbuilder.text.Alignment;
import dev.hephaestus.deckbuilder.text.Style;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Spell extends Card {
    private static final Pattern COST_SYMBOLS = Pattern.compile("\\G\\{([WUBGRxyz0-9])}");
    private final List<TextComponent> manaCost;

    public Spell(String name, List<Color> colors, String type, TypeContainer types, URL image, String manaCost) {
        super(name, type, colors, image, types);

        Matcher matcher = COST_SYMBOLS.matcher(manaCost);
        List<TextComponent> components = new ArrayList<>();

        while (matcher.find()) {
            String group = matcher.group(1);
            char c = group.charAt(0);
            Color color = Color.of(c);

            Style style = new Style.Builder()
                    .font("NDPMTG")
                    .size(175F)
                    .shadow(new Style.Shadow(0, -4, 11))
                    .color(color == null ? Color.BLACK.rgb() : color.rgb())
                    .build();

            components.add(new TextComponent(style, "o"));

            style = new Style.Builder()
                    .font("NDPMTG")
                    .size(175F)
                    .color(0)
                    .build();

            components.add(new TextComponent(style, "" + Character.toLowerCase(c)));
        }

        this.manaCost = components;
    }

    public final List<TextComponent> manaCost() {
        return this.manaCost;
    }
}
