package dev.hephaestus.deckbuilder.cards;

import dev.hephaestus.deckbuilder.TextComponent;
import dev.hephaestus.deckbuilder.templates.Template;

import java.net.URL;
import java.util.List;

public class Creature extends Spell {
    private final String power, toughness;

    public Creature(Template template, String name, List<Color> colors, String type, TypeContainer types, URL image, String manaCost, String power, String toughness, List<List<TextComponent>> text) {
        super(template, name, colors, type, types, image, manaCost, text);
        this.power = power;
        this.toughness = toughness;
    }

    public final String power() {
        return power;
    }

    public final String toughness() {
        return toughness;
    }
}
