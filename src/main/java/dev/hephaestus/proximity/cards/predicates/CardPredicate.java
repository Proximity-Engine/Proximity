package dev.hephaestus.proximity.cards.predicates;

import dev.hephaestus.proximity.json.JsonElement;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.util.Result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface CardPredicate {
    Result<Boolean> test(JsonObject card);

    static Optional<Result<JsonElement>> traverse(JsonObject card, String[] key) {
        JsonElement element = card;

        for (int i = 0, stringsLength = key.length; i < stringsLength; i++) {
            String k = key[i];

            if (element.isJsonObject() && element.getAsJsonObject().has(k)) {
                element = element.getAsJsonObject().get(k);
            } else if (element.isJsonArray()) {
                if (i < key.length - 1) {
                    return Optional.of(Result.error("Cannot test condition with non-terminating Array element %s.", k));
                }
            } else if (element.isJsonPrimitive()) {
                if (i < key.length - 1) {
                    return Optional.of(Result.error("Cannot test condition with non-terminating Primitive element %s.", k));
                }
            } else {
                return Optional.empty();
            }
        }

        return Optional.of(Result.of(element));
    }

    final class And implements CardPredicate {
        private final List<CardPredicate> predicates;

        public And(List<CardPredicate> predicates) {
            this.predicates = new ArrayList<>(predicates);
        }

        public And(CardPredicate... predicates) {
            this.predicates = Arrays.asList(predicates);
        }

        @Override
        public Result<Boolean> test(JsonObject card) {
            Result<Boolean> result = Result.of(true);
            List<String> errors = new ArrayList<>();

            for (CardPredicate predicate : this.predicates) {
                result = predicate.test(card);

                if (result.isError()) {
                    errors.add(result.getError());
                } else if (!result.get()) {
                    break;
                }
            }

            if (errors.size() > 0) {
                result = Result.error(String.format("Encountered %d errors:\n\t%s", errors.size(), String.join("\n\t%s", errors)));
            }

            return result;
        }
    }

    final class Or implements CardPredicate {
        private final List<CardPredicate> predicates;

        public Or(List<CardPredicate> predicates) {
            this.predicates = new ArrayList<>(predicates);
        }

        @Override
        public Result<Boolean> test(JsonObject card) {
            Result<Boolean> result = Result.of(true);
            List<String> errors = new ArrayList<>();

            for (CardPredicate predicate : this.predicates) {
                result = predicate.test(card);

                if (result.isError()) {
                    errors.add(result.getError());
                } else if (result.get()) {
                    break;
                }
            }

            if (errors.size() > 0) {
                result = Result.error(String.format("Encountered %d errors:\n\t%s", errors.size(), String.join("\n\t", errors)));
            }

            return result;
        }
    }

    final class Not implements CardPredicate {
        private final CardPredicate wrapped;

        public Not(CardPredicate wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public Result<Boolean> test(JsonObject card) {
            return wrapped.test(card).then(b -> Result.of(!b));
        }
    }
}
