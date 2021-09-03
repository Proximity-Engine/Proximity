package dev.hephaestus.deckbuilder.cards;

import dev.hephaestus.deckbuilder.TextComponent;
import dev.hephaestus.deckbuilder.templates.Template;
import dev.hephaestus.deckbuilder.text.Style;
import dev.hephaestus.deckbuilder.text.Symbols;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Spell extends Card {
    private static final Pattern COST_SYMBOLS = Pattern.compile("\\G\\{([^}]*)}");
    private final List<TextComponent> manaCost = new ArrayList<>();

    public Spell(Template template, String name, List<Color> colors, String type, TypeContainer types, URL image, String manaCost, List<List<TextComponent>> text) {
        super(template, type, colors, image, types, text, name);

        Matcher matcher = COST_SYMBOLS.matcher(manaCost);

        Style style = new Style.Builder()
                .font("NDPMTG")
                .size(175F)
                .shadow(new Style.Shadow(0, -4, 11))
                .build();

        while (matcher.find()) {
            String symbol = matcher.group(1);
            this.manaCost.addAll(Symbols.symbol(symbol, template, style, new Symbols.Factory.Context("cost")));
        }
    }

    public final List<TextComponent> manaCost() {
        return this.manaCost;
    }
}
