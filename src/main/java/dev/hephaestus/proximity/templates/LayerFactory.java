package dev.hephaestus.proximity.templates;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonElement;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.layers.Layer;
import dev.hephaestus.proximity.util.Result;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class LayerFactory<T extends Layer> {
    private static final Pattern SUBSTITUTE = Pattern.compile("\\$(\\w*)\\{(\\w+(?:\\.\\w+)*)}");

    protected final String id;
    protected final int x, y;
    private final List<CardPredicate> predicates;

    protected LayerFactory(String id, int x, int y, List<CardPredicate> predicates) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.predicates = predicates;
    }

    public abstract Result<T> createLayer(String parentId, JsonObject card);

    public Result<? extends Layer> create(String parentId, JsonObject card) {
        for (CardPredicate predicate : this.predicates) {
            Result<Boolean> r = predicate.test(card);

            if (r.isError()) return Result.error(r.getError());
            if (!r.get()) return Result.of(Layer.EMPTY);
        }

        return this.createLayer(parentId, card);
    }

    protected static String substitute(String string, JsonObject card) {
        Matcher matcher = SUBSTITUTE.matcher(string);

        String s = string;

        while (matcher.find()) {
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

            s = s.replace("${" + matcher.group(2) + "}", replacement);
        }

        return s;
    }
}
