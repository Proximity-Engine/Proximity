package dev.hephaestus.proximity.cards;

import dev.hephaestus.proximity.text.TextComponent;
import dev.hephaestus.proximity.text.TextAlignment;

import java.util.List;

public record TextBody(TextAlignment alignment, List<List<TextComponent>> text) {

}
