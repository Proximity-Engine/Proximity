package dev.hephaestus.proximity.cards.layers;

import dev.hephaestus.proximity.json.JsonElement;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.json.JsonPrimitive;
import dev.hephaestus.proximity.scripting.Context;
import dev.hephaestus.proximity.scripting.ScriptingUtil;
import dev.hephaestus.proximity.templates.layers.TextLayer;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.text.Symbol;
import dev.hephaestus.proximity.text.TextAlignment;
import dev.hephaestus.proximity.text.TextComponent;
import dev.hephaestus.proximity.util.Box;
import dev.hephaestus.proximity.util.Rectangles;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.StatefulGraphics;
import dev.hephaestus.proximity.xml.LayerProperty;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableCard;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.regex.Matcher;

import static dev.hephaestus.proximity.xml.RenderableCard.KEY;
import static dev.hephaestus.proximity.xml.RenderableCard.XMLElement.handle;

public class TextLayerRenderer extends LayerRenderer {
    @Override
    public Result<Optional<Rectangles>> renderLayer(RenderableCard card, RenderableCard.XMLElement element, StatefulGraphics graphics, Rectangles wrap, boolean draw, Box<Float> scale, Rectangle2D bounds) {
        if (element.hasAttribute("width") ^ element.hasAttribute("height")) {
            return Result.error("Text layer must have both 'width' and 'height' attributes or neither");
        }

        String value = element.getAttributeRaw("value");

        TextAlignment alignment = element.hasAttribute("alignment") ? TextAlignment.valueOf(element.getAttribute("alignment").toUpperCase(Locale.ROOT)) : TextAlignment.LEFT;
        int x = (element.hasAttribute("x") ? Integer.decode(element.getAttribute("x")) : 0);
        int y = (element.hasAttribute("y") ? Integer.decode(element.getAttribute("y")) : 0);
        Integer width = element.hasAttribute("width") ? Integer.decode(element.getAttribute("width")) : null;
        Integer height = element.hasAttribute("height") ? Integer.decode(element.getAttribute("height")) : null;
        wrap = element.getProperty(LayerProperty.WRAP);
        Style style = element.getProperty(LayerProperty.STYLE, Style.EMPTY).merge(
                card.getStyle(element.getAttribute("style"))
        );

        List<List<TextComponent>> text = applyCapitalization(parseText(element, card, style, value), style.capitalization(), style.size());

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

    private List<List<TextComponent>> parseText(RenderableCard.XMLElement element, RenderableCard card, Style baseStyle, String string) {
        Matcher matcher = RenderableCard.SUBSTITUTE.matcher(string);

        List<List<TextComponent>> result = new ArrayList<>();

        int previousEnd = 0;

        while (matcher.find()) {
            String priors = string.substring(previousEnd, matcher.start());

            if (!priors.isEmpty()) {
                result.add(Collections.singletonList(new TextComponent.Literal(baseStyle, priors)));
            }

            String function = matcher.group("function");

            String[] key = matcher.group("value").split("\\.");
            JsonElement e = card;

            for (String k : key) {
                Matcher m = KEY.matcher(k);

                while(m.find()) {
                    if (e instanceof JsonObject object) {
                        e = object.get(m.group("key"));

                        if (m.group("range") != null) {
                            e = handle(e, Integer.decode(m.group("beginning")), Integer.decode(m.group("end")));
                        }
                    } else {
                        throw new UnsupportedOperationException();
                    }
                }
            }

            previousEnd = matcher.end();

            Map<String, String> namedContexts = new LinkedHashMap<>();
            List<String> looseContexts = new ArrayList<>();

            Context context = Context.create(element.getId(), namedContexts, looseContexts, (step, task) -> {});

            if (function == null) {
                result.add(Collections.singletonList(new TextComponent.Literal(baseStyle, e instanceof JsonPrimitive primitive && primitive.isString() ? e.getAsString() : e.toString())));
            } else {
                result.addAll(ScriptingUtil.applyTextFunction(context, function, e, card, card.getStyles(), baseStyle));
            }
        }

        if (previousEnd != string.length()) {
            String priors = string.substring(previousEnd);
            result.add(Collections.singletonList(new TextComponent.Literal(baseStyle, priors)));
        }

        if (!result.isEmpty()) {
            StringBuilder text = new StringBuilder();

            for (var list : result) {
                for (var components : list) {
                    text.append(components.string());
                }
            }

            List<Symbol> symbols = card.getApplicable(text.toString());

            if (symbols.size() > 0) {
                List<List<TextComponent>> preSymbolResult = result;
                result = new ArrayList<>(preSymbolResult.size());

                for (List<TextComponent> components : preSymbolResult) {
                    List<TextComponent> list = new ArrayList<>(components.size());
                    result.add(list);

                    for (TextComponent component : components) {
                        string = component.string();
                        int anchor = 0;

                        for (int i = 0; i < string.length(); ++i) {
                            String s = string.substring(i);

                            for (Symbol symbol : symbols) {
                                if (s.startsWith(symbol.getRepresentation())) {
                                    if (i > anchor) {
                                        list.add(new TextComponent.Literal(component.style(), string.substring(anchor, i)));
                                    }

                                    list.addAll(symbol.getGlyphs(component.style()));

                                    i += symbol.getRepresentation().length() - 1;
                                    anchor = i + 1;
                                }
                            }
                        }

                        if (anchor < string.length()) {
                            list.add(new TextComponent.Literal(component.style(), string.substring(anchor)));
                        }
                    }
                }
            }
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
                    case ALL_CAPS -> level.add(new TextComponent.Literal(component.style(), component.string().toUpperCase(Locale.ROOT)));
                    case NO_CAPS -> level.add(new TextComponent.Literal(component.style(), component.string().toLowerCase(Locale.ROOT)));
                    case SMALL_CAPS -> {
                        Style uppercase = component.style() == null
                                ? new Style.Builder().size(fontSize).build()
                                : component.style().size(fontSize);

                        Style lowercase = component.style() == null
                                ? new Style.Builder().size((int) (fontSize * 0.75F)).build()
                                : component.style().size((int) (fontSize * 0.75F));

                        for (char c : component.string().toCharArray()) {
                            boolean bl = Character.isUpperCase(c);

                            level.add(new TextComponent.Literal(bl ? uppercase : lowercase, Character.toUpperCase(c)));
                        }
                    }
                }
            }

            result.add(level);
        }

        return result;
    }
}
