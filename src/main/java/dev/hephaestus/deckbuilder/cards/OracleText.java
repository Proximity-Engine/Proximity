package dev.hephaestus.deckbuilder.cards;

import dev.hephaestus.deckbuilder.TextComponent;
import dev.hephaestus.deckbuilder.text.Alignment;

import java.util.List;

public record OracleText(Alignment alignment, List<List<TextComponent>> text) {

}
