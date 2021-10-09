package dev.hephaestus.proximity.cards.layers;

import dev.hephaestus.proximity.util.Rectangles;
import dev.hephaestus.proximity.xml.RenderableCard;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.json.JsonElement;
import dev.hephaestus.proximity.templates.TextFunction;
import dev.hephaestus.proximity.templates.layers.TextLayer;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.text.TextAlignment;
import dev.hephaestus.proximity.text.TextComponent;
import dev.hephaestus.proximity.util.Keys;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.StatefulGraphics;
import dev.hephaestus.proximity.xml.LayerProperty;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;

public class TextLayerRenderer extends LayerRenderer {
    @Override
    public Result<Optional<Rectangles>> renderLayer(RenderableCard card, RenderableCard.XMLElement element, StatefulGraphics graphics, Rectangles wrap, boolean draw, float scale, Rectangle2D bounds) {
        if (element.hasAttribute("width") ^ element.hasAttribute("height")) {
            return Result.error("Text layer must have both 'width' and 'height' attributes or neither");
        }

        String value = element.getAttribute("value");

        TextAlignment alignment = element.hasAttribute("alignment") ? TextAlignment.valueOf(element.getAttribute("alignment").toUpperCase(Locale.ROOT)) : TextAlignment.LEFT;
        int x = (element.hasAttribute("x") ? Integer.decode(element.getAttribute("x")) : 0);
        int y = (element.hasAttribute("y") ? Integer.decode(element.getAttribute("y")) : 0);
        Integer width = element.hasAttribute("width") ? Integer.decode(element.getAttribute("width")) : null;
        Integer height = element.hasAttribute("height") ? Integer.decode(element.getAttribute("height")) : null;
        wrap = Rectangles.singleton(element.getProperty(LayerProperty.WRAP));
        Style style = element.getProperty(LayerProperty.STYLE, Style.EMPTY).merge(
                card.getStyle(element.getAttribute("style"))
        );

        List<List<TextComponent>> text = applyCapitalization(parseText(card, style, value), style.capitalization(), style.size());

        TextLayer layer = new TextLayer(
                element.getId(),
                x,
                y,
                card,
                style,
                text,
                alignment,
                width != null && height != null ? new Rectangle(x, y, width, height) : null
        );

        layer.setWrap(wrap);

        if (bounds == null) {
            bounds = element.getProperty(LayerProperty.BOUNDS);
        }

        if (bounds == null && width != null && height != null) {
            bounds = new Rectangle2D.Double(x, y, width, height);
        }

        layer.setBounds(bounds);

        return Result.of(Optional.ofNullable(layer.draw(graphics, wrap, draw, scale)));
    }

    @Override
    public boolean scales(RenderableCard card, RenderableCard.XMLElement element) {
        return true;
    }

    private List<List<TextComponent>> parseText(RenderableCard card, Style baseStyle, String string) {
        Matcher matcher = RenderableCard.SUBSTITUTE.matcher(string);

        List<List<TextComponent>> result = new ArrayList<>();

        int previousEnd = 0;

        while (matcher.find()) {
            String priors = string.substring(previousEnd, matcher.start());

            if (!priors.isEmpty()) {
                result.add(Collections.singletonList(new TextComponent(baseStyle, priors)));
            }

            String function = matcher.group(1);
            String replacement = "";

            if (matcher.groupCount() == 2) {
                String[] key = matcher.group(2).split("\\.");
                JsonElement element = card.get(key);

                if (element == null) {
                    replacement = "null";
                } else if (element.isJsonArray()) {
                    StringBuilder builder = new StringBuilder();

                    for (JsonElement e : element.getAsJsonArray()) {
                        builder.append(e.getAsString());
                    }

                    replacement = builder.toString();
                } else if (element.isJsonPrimitive()) {
                    replacement = element.getAsString();
                } else {
                    throw new UnsupportedOperationException();
                }
            }

            previousEnd = matcher.end();

            result.addAll(TextFunction.apply(card::getStyle, baseStyle, function, replacement, card.getAsJsonObject(Keys.OPTIONS)));
        }

        if (previousEnd != string.length()) {
            String priors = string.substring(previousEnd);
            result.add(Collections.singletonList(new TextComponent(baseStyle, priors)));
        }

        return result;
    }

    private static List<List<TextComponent>> applyCapitalization(List<List<TextComponent>> text, Style.Capitalization caps, Integer fontSize) {
        if (caps == null || fontSize == null) return text;

        List<List<TextComponent>> result = new ArrayList<>();

        for (List<TextComponent> list : text) {
            List<TextComponent> level = new ArrayList<>();

            for (TextComponent component : list) {
                switch (caps) {
                    case ALL_CAPS -> level.add(new TextComponent(component.string().toUpperCase(Locale.ROOT)));
                    case NO_CAPS -> level.add(new TextComponent(component.string().toLowerCase(Locale.ROOT)));
                    case SMALL_CAPS -> {
                        Style uppercase = component.style() == null
                                ? new Style.Builder().size(fontSize).build()
                                : component.style().size(fontSize);

                        Style lowercase = component.style() == null
                                ? new Style.Builder().size((int) (fontSize * 0.75F)).build()
                                : component.style().size((int) (fontSize * 0.75F));

                        for (char c : component.string().toCharArray()) {
                            boolean bl = Character.isUpperCase(c);

                            level.add(new TextComponent(bl ? uppercase : lowercase, Character.toUpperCase(c)));
                        }
                    }
                }
            }

            result.add(level);
        }

        return result;
    }
}
