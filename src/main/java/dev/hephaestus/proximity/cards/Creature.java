package dev.hephaestus.proximity.cards;

import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.text.Symbol;
import dev.hephaestus.proximity.util.OptionContainer;

import java.net.URL;
import java.util.Collection;
import java.util.Set;

public class Creature extends Spell {
    private final String power, toughness;

    public Creature(int number, String name, Template template, Collection<Symbol> colors, String type, TypeContainer types, URL image, String manaCost, String power, String toughness, TextBody text, OptionContainer options, Set<String> frameEffects, boolean isFrontFace, Card backSide, Set<String> keywords, String layout, TextBody flavorText) {
        super(number, name, template, colors, type, types, image, manaCost, text, options, frameEffects, isFrontFace, backSide, keywords, layout, flavorText);
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
