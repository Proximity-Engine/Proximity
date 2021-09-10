package dev.hephaestus.proximity.cards;

import dev.hephaestus.proximity.json.JsonElement;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.util.Result;
import org.apache.logging.log4j.Logger;

public class ElementPredicate implements Predicate {
    private final Logger log;
    private final String[] key;
    private final JsonElement p;

    private ElementPredicate(Logger log, String[] key, JsonElement p) {
        this.log = log;
        this.key = key;
        this.p = p;
    }

    public static Result<ElementPredicate> of(Logger log, String[] key, JsonElement p) {
        if (key.length == 0) {
            return Result.error("Cannot create condition with key of length 0.");
        }

        if (p.isJsonArray()) {
            return Result.error("Cannot create condition with p of an array.");
        }

        return Result.of(new ElementPredicate(log, key, p));
    }

    @Override
    public Result<Boolean> test(JsonObject card) {
        JsonElement element = card;

        Result<Boolean> result = Result.of(false);

        for (int i = 0, stringsLength = this.key.length; i < stringsLength; i++) {
            String k = this.key[i];

            if (element.isJsonObject() && element.getAsJsonObject().has(k)) {
                element = element.getAsJsonObject().get(k);
            } else if (element.isJsonArray() && i < this.key.length - 1) {
                result = Result.error("Cannot test condition with non-terminating Array element %s.", k);
                break;
            } else if (element.isJsonPrimitive() && i < this.key.length - 1) {
                result = Result.error("Cannot test condition with non-terminating Primitive element %s.", k);
                break;
            }
        }

        if (!result.isError()) {
            if (element.isJsonArray()) {
                result = Result.of(!this.p.getAsBoolean());

                for (JsonElement e : element.getAsJsonArray()) {
                    if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() && e.getAsString().equals(this.key[this.key.length - 1])) {
                        result = Result.of(this.p.getAsBoolean());
                        break;
                    } else if (!e.isJsonPrimitive()) {
                        result = Result.error(String.format("Cannot test equality of element %s", element));
                        break;
                    }
                }
            } else if (element.isJsonObject()) {
                result = Result.error("Cannot test equality of Object.");
            } else if (element.isJsonPrimitive()) {
                result = Result.of(element.equals(this.p));
            } else {
                result = Result.error(String.format("Unexpected type for element %s", element));
            }
        }

        log.debug("{} == {}: {}", this.toString(), this.p, result.isError() ? result.getError() : result.get() ? "PASS" : "FAIL");

        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < this.key.length; i++) {
            builder.append(this.key[i]);

            if (i < this.key.length - 1) {
                builder.append(".");
            }
        }

        builder.append(": ").append(this.p.toString());

        return builder.toString();
    }
}
