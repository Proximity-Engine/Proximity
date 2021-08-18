package dev.hephaestus.deckbuilder.cards;

import dev.hephaestus.deckbuilder.TextComponent;
import dev.hephaestus.deckbuilder.text.Alignment;
import dev.hephaestus.deckbuilder.text.Style;
import dev.hephaestus.deckbuilder.text.Text;

import java.net.URL;
import java.util.List;

public class Creature extends Spell {
    private final String power, toughness;
    private final Text powerToughnessText;

    public Creature(String name, List<Color> colors, String type, TypeContainer types, URL image, String manaCost, String power, String toughness) {
        super(name, colors, type, types, image, manaCost);
        this.power = power;
        this.toughness = toughness;
        this.powerToughnessText = new Text(new Style.Builder()
                .font("Beleren2016-Bold")
                .size(155F)
                .alignment(Alignment.CENTER)
                .build(), new TextComponent(0, String.format("%s/%s", this.power, this.toughness)));
    }

    public final String power() {
        return power;
    }

    public final String toughness() {
        return toughness;
    }

    public final Text powerAndToughness() {
        return this.powerToughnessText;
    }
}
