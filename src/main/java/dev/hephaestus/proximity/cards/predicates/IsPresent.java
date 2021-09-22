package dev.hephaestus.proximity.cards.predicates;

import dev.hephaestus.proximity.json.JsonElement;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.util.Result;

import java.util.Arrays;
import java.util.Optional;

public class IsPresent implements CardPredicate {
    private final String[] key;
    private final String value;
    private final boolean present;

    public IsPresent(String[] key, boolean present) {
        this.key = Arrays.copyOfRange(key, 0, key.length - 1);
        this.value = key[key.length - 1];
        this.present = present;
    }

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

                if (element.isJsonObject()) {
                    return Result.of(element.getAsJsonObject().has(this.value) == this.present);
                } else if (element.isJsonArray()) {
                    return Result.of(element.getAsJsonArray().contains(this.value) == this.present);
                } else {
                    return Result.error("Expected Object or Array at '%s', found '%'", this.key, element);
                }
            }
        }
    }
}
