package dev.hephaestus.proximity.cards.predicates;

import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.util.Result;

public record Range(String[] key, int min, int max) implements CardPredicate {
    @Override
    public Result<Boolean> test(JsonObject card) {
        int i = Integer.decode(card.getFromFullPath(String.join(".", this.key)).getAsString());

        return Result.of(i >= this.min && i <= this.max);
    }

    public static Result<CardPredicate> of(String[] key, int min, int max) {
        if (min > max) {
            return Result.error("min must be less than or equal to max");
        }

        return Result.of(new Range(key, min, max));
    }
}
