package dev.hephaestus.proximity.xml;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;

public record AttributeModifier(String attributeName, String value, CardPredicate predicate) {
}
