package dev.hephaestus.proximity.templates.layers.factories;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.cards.TextBody;
import dev.hephaestus.proximity.cards.TextParser;
import dev.hephaestus.proximity.json.JsonElement;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.LayerFactory;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.TextFunction;
import dev.hephaestus.proximity.templates.layers.TextLayer;
import dev.hephaestus.proximity.text.Alignment;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.text.TextComponent;
import dev.hephaestus.proximity.util.Keys;
import dev.hephaestus.proximity.util.Result;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextFactory extends LayerFactory<TextLayer> {
    private static final Pattern SUBSTITUTE = Pattern.compile("\\$(\\w*)\\{(\\w+(?:\\.\\w+)*)}");

    private final Alignment alignment;
    private final Integer width, height;
    private final Style style;
    private final Rectangle wrap;
    private final String value;
    private final Template template;

    public TextFactory(String id, int x, int y, List<CardPredicate> predicates, Alignment alignment, Integer width, Integer height, Style style, Rectangle wrap, String value, Template template) {
        super(id, x, y, predicates);
        this.alignment = alignment;
        this.width = width;
        this.height = height;
        this.style = style;
        this.wrap = wrap;
        this.value = value;
        this.template = template;
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
            String[] key = matcher.group(2).split("\\.");

            JsonElement element = card.get(key);

            String replacement;

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

            previousEnd = matcher.end();

            result.addAll(TextFunction.apply(styleGetter, baseStyle, function, replacement, card.getAsJsonObject(Keys.OPTIONS)));
        }

        if (previousEnd != string.length()) {
            String priors = string.substring(previousEnd);
            result.add(Collections.singletonList(new TextComponent(baseStyle, priors)));
        }

        return result;
    }


    @Override
    public Result<TextLayer> createLayer(String parentId, JsonObject card) {
        List<List<dev.hephaestus.proximity.text.TextComponent>> text;
        Alignment alignment = this.alignment;

        int x = this.x;

        if (this.value.equals("$oracle_and_flavor_text{}")) {
            TextBody oracle = new TextParser(card.getAsString("oracle_text"), this.template::getStyle, this.template.getStyle("oracle"), "\n\n", card.getAsJsonObject(Keys.OPTIONS)).parseOracle();

            if (card.has("flavor_text")) {
                text = oracle.text();

                text.add(Collections.singletonList(new TextComponent(this.style, "\n\n")));
                text.addAll(TextFunction.flavorText(this.template::getStyle, this.template.getStyle("flavor"), card.getAsString("flavor_text"), card.getAsJsonObject(Keys.OPTIONS)));
            } else {
                if (oracle.alignment() == Alignment.CENTER && this.width != null) {
                    alignment = Alignment.CENTER;
                    x += this.width / 2;
                }

                text = oracle.text();
            }
        } else {
            text = parseText(this.template::getStyle, this.style, this.value, card);
        }

        text = applyCapitalization(text, this.style.capitalization(), this.style.size());

        return Result.of(new TextLayer(
                parentId,
                this.id,
                x,
                this.y,
                this.template,
                this.style,
                text,
                alignment,
                this.width != null && this.height != null ? new Rectangle(x, this.y, this.width, this.height) : null,
                this.wrap
        ));
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
