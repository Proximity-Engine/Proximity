package dev.hephaestus.proximity.cards;

import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.util.Result;

import java.util.ArrayList;
import java.util.List;

public interface Predicate {
    Result<Boolean> test(JsonObject card);

    final class Or implements Predicate {
        private final List<Predicate> predicates;

        public Or(List<Predicate> predicates) {
            this.predicates = new ArrayList<>(predicates);
        }

        @Override
        public Result<Boolean> test(JsonObject card) {
            Result<Boolean> result = Result.of(true);
            List<String> errors = new ArrayList<>();

            for (Predicate predicate : this.predicates) {
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
}
