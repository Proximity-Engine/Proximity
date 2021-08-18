package dev.hephaestus.deckbuilder.cards;

import dev.hephaestus.deckbuilder.TextComponent;
import dev.hephaestus.deckbuilder.text.Alignment;
import dev.hephaestus.deckbuilder.text.Style;
import dev.hephaestus.deckbuilder.text.Text;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Spell extends Card {
    private static final Pattern COST_SYMBOLS = Pattern.compile("\\{([WUBGRxyz0-9])}");
    private final Text manaCost;

    public Spell(String name, List<Color> colors, String type, TypeContainer types, URL image, String manaCost) {
        super(name, type, colors, image, types);

        Matcher matcher = COST_SYMBOLS.matcher(manaCost);
        List<TextComponent> components = new ArrayList<>();

        Style style = new Style.Builder()
                .font("NDPMTG")
                .size(175F)
                .alignment(Alignment.RIGHT)
                .build();

        for (int i = 1; i < matcher.groupCount(); ++i) {
            String group = matcher.group(i);
            char c = group.charAt(0);
            Color color = Color.of(c);

            components.add(new TextComponent((color == null ? Color.BLACK : color).rgb(), "" + c));
        }

        this.manaCost = new Text(style, components.toArray(new TextComponent[0]));
    }

    public final Text manaCost() {
        return this.manaCost;
    }
}
