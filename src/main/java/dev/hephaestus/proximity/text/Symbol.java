package dev.hephaestus.proximity.text;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.xml.RenderableCard;

import java.util.ArrayList;
import java.util.List;

public final class Symbol {
    private final String representation;
    private final List<TextComponent> glyphs;
    private final CardPredicate predicate;

    public Symbol(String representation, List<TextComponent> glyphs, List<CardPredicate> predicates) {
        this.representation = representation;
        this.glyphs = new ArrayList<>(glyphs);
        this.predicate = new CardPredicate.And(predicates);
    }

    public boolean anyMatches(RenderableCard card, String text) {
        return this.predicate.test(card).orElse(false) && text.contains(this.representation);
    }

    public String getRepresentation() {
        return this.representation;
    }

    public List<TextComponent> getGlyphs(Style baseStyle) {
        List<TextComponent> result = new ArrayList<>(this.glyphs.size());

        for (TextComponent component : this.glyphs) {
            result.add(new TextComponent.Literal(
                    baseStyle.merge(component.style()),
                    component.string()
            ));
        }

        return result;
    }
}
