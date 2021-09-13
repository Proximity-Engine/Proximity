package dev.hephaestus.proximity.cards;

import dev.hephaestus.proximity.text.TextComponent;
import dev.hephaestus.proximity.text.Alignment;

import java.util.List;

public record TextBody(Alignment alignment, List<List<TextComponent>> text) {

}
