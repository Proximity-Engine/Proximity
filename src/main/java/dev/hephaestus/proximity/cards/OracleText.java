package dev.hephaestus.proximity.cards;

import dev.hephaestus.proximity.TextComponent;
import dev.hephaestus.proximity.text.Alignment;

import java.util.List;

public record OracleText(Alignment alignment, List<List<TextComponent>> text) {

}
