package dev.hephaestus.proximity.cards.predicates;

import dev.hephaestus.proximity.api.json.JsonElement;
import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.api.json.JsonPrimitive;
import dev.hephaestus.proximity.util.Result;

import java.util.Optional;

public record IsEquals(String key, String value) implements CardPredicate {
    @Override
    public Result<Boolean> test(JsonObject card) {
        Optional<Result<JsonElement>> optional = CardPredicate.traverse(card, key);

        if (optional.isEmpty()) {
            return Result.of(false);
        } else {
            Result<JsonElement> result = optional.get();

            if (result.isError()) {
                return Result.error(result.getError());
            } else {
                JsonElement element = result.get();

                if (element.isJsonPrimitive()) {
                    JsonPrimitive primitive = element.getAsJsonPrimitive();

                    if (primitive.isBoolean() && (this.value.equalsIgnoreCase("true") || this.value.equalsIgnoreCase("false"))) {
                        return Result.of(primitive.getAsBoolean() == Boolean.parseBoolean(this.value));
                    } else if (primitive.isNumber() && canParseInteger(this.value)) {
                        return Result.of(primitive.getAsInt() == Integer.decode(this.value));
                    } else if (primitive.isString()) {
                        return Result.of(primitive.getAsString().equals(this.value));
                    } else {
                        return Result.error("Expected String, Boolean, or Integer at '%s', found '%s'", String.join(".", this.key), element);
                    }
                } else {
                    return Result.error("Expected String, Boolean, or Integer at '%s', found '%s'", String.join(".", this.key), element);
                }
            }
        }
    }

    private static boolean canParseInteger(String string) {
        try {
            //noinspection ResultOfMethodCallIgnored
            Integer.decode(string);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
