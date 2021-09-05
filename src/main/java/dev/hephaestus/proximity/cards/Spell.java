package dev.hephaestus.proximity.cards;

import dev.hephaestus.proximity.TextComponent;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.text.Symbol;
import dev.hephaestus.proximity.util.OptionContainer;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Spell extends Card {
    private static final Pattern COST_SYMBOLS = Pattern.compile("\\G\\{([^}]*)}");
    private final List<TextComponent> manaCost = new ArrayList<>();

    public Spell(int number, String name, Template template, Collection<Symbol> colors, String type, TypeContainer types, URL image, String manaCost, OracleText text, OptionContainer options, Set<String> frameEffects) {
        super(number, name, colors, image, types, text, type, options, frameEffects);

        Matcher matcher = COST_SYMBOLS.matcher(manaCost);

        Style style = template.getStyle("mana_cost");

        if (style == Style.EMPTY) {
            style = new Style.Builder()
                    .font("NDPMTG")
                    .size(175F)
                    .shadow(new Style.Shadow(0, -4, 11))
                    .build();
        }

        while (matcher.find()) {
            String symbol = matcher.group(1);
            this.manaCost.addAll(Symbol.symbol(symbol, template, style, new Symbol.Factory.Context("cost")));
        }
    }

    public final List<TextComponent> manaCost() {
        return this.manaCost;
    }
}
