package dev.hephaestus.deckbuilder.cards;

import java.net.URL;
import java.util.List;

public class Creature extends Spell {
    private final String power, toughness;

    public Creature(String name, List<Color> colors, String type, TypeContainer types, URL image, String manaCost, String power, String toughness) {
        super(name, colors, type, types, image, manaCost);
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
