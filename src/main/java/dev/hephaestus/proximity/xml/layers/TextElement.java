package dev.hephaestus.proximity.xml.layers;

import dev.hephaestus.proximity.cards.TextBody;
import dev.hephaestus.proximity.cards.TextParser;
import dev.hephaestus.proximity.json.JsonElement;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.TextFunction;
import dev.hephaestus.proximity.templates.layers.TextLayer;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.text.TextAlignment;
import dev.hephaestus.proximity.text.TextComponent;
import dev.hephaestus.proximity.util.Keys;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.xml.LayerProperty;
import dev.hephaestus.proximity.xml.Properties;
import org.w3c.dom.Element;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;

public class TextElement extends LayerElement<TextLayer> {
    private TextAlignment alignment;
    private Integer width, height;
    private String value, styleName;
    private Function<JsonObject, Style> style;
    private Function<JsonObject, Rectangle> wrap;
    private Template template;

    public TextElement(Element element) {
        super(element);
    }

    @Override
    protected Result<LayerElement<TextLayer>> parseLayer(Context context, Properties properties) {
        if (element.hasAttribute("width") ^ element.hasAttribute("height")) {
            return Result.error("Text layer must have both 'width' and 'height' attributes or neither");
        }

        this.alignment = element.hasAttribute("alignment") ? TextAlignment.valueOf(element.getAttribute("alignment").toUpperCase(Locale.ROOT)) : TextAlignment.LEFT;
        this.width = element.hasAttribute("width") ? Integer.decode(element.getAttribute("width")) : null;
        this.height = element.hasAttribute("height") ? Integer.decode(element.getAttribute("height")) : null;
        this.value = element.getAttribute("value");
        this.styleName = element.hasAttribute("style") ? element.getAttribute("style") : null;
        this.style = properties.get(LayerProperty.STYLE);
        this.wrap = properties.get(LayerProperty.WRAP);

        return Result.of(this);
    }

    @Override
    public Result<LayerElement<TextLayer>> createFactory(Template template) {
        if (template.getStyle(this.styleName) != null) {
            Style style = template.getStyle(this.styleName);
            Function<JsonObject, Style> s = this.style;
            this.style = object -> style.merge(s.apply(object));
        }

        this.template = template;

        return Result.of(this);
    }

    @Override
    public Result<TextLayer> createLayer(String parentId, JsonObject card) {
        List<List<dev.hephaestus.proximity.text.TextComponent>> text;
        TextAlignment alignment = this.alignment;

        int x = this.getX();

        Style style = this.style == null ? Style.EMPTY : this.style.apply(card);
        Rectangle wrap = this.wrap == null ? null : this.wrap.apply(card);

        if (this.value.equals("$oracle_and_flavor_text{}")) {
            TextBody oracle = new TextParser(card.getAsString("oracle_text"), this.template::getStyle, style, "\n\n", card.getAsJsonObject(Keys.OPTIONS)).parseOracle();

            if (card.has("flavor_text")) {
                text = oracle.text();

                text.add(Collections.singletonList(new TextComponent(style, "\n\n")));
                text.addAll(TextFunction.flavorText(this.template::getStyle, this.template.getStyle("flavor"), card.getAsString("flavor_text"), card.getAsJsonObject(Keys.OPTIONS)));
            } else {
                if (oracle.alignment() == TextAlignment.CENTER && this.width != null) {
                    alignment = TextAlignment.CENTER;
                    x += this.width / 2;
                }

                text = oracle.text();
            }
        } else {
            text = parseText(this.template::getStyle, style, this.value, card);
        }

        text = applyCapitalization(text, style.capitalization(), style.size());

        TextLayer layer = new TextLayer(
                parentId,
                this.getId(),
                x,
                this.getY(),
                this.template,
                style,
                text,
                alignment,
                this.width != null && this.height != null ? new Rectangle(x, this.getY(), this.width, this.height) : null
        );

        layer.setWrap(wrap);

        return Result.of(layer);
    }

    private List<List<TextComponent>> parseText(Function<String, Style> styleGetter, Style baseStyle, String string, JsonObject card) {
        Matcher matcher = SUBSTITUTE.matcher(string);

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

            result.addAll(TextFunction.apply(styleGetter, baseStyle, function, replacement, card.getAsJsonObject(Keys.OPTIONS)));
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
