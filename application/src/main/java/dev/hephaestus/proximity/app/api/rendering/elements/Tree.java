package dev.hephaestus.proximity.app.api.rendering.elements;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;

public interface Tree extends Parent, Element {
    record Level(Level.Branch... branches) {
        public Level(ReadOnlyBooleanProperty... branches) {
            this(copy(branches));
        }

        public Level(String defaultBranch, ReadOnlyBooleanProperty... branches) {
            this(insert(defaultBranch, branches));
        }

        public Level(String defaultBranch, Level.Branch... branches) {
            this(insert(defaultBranch, branches));
        }

        private static Level.Branch[] insert(String defaultBranch, ReadOnlyBooleanProperty... properties) {
            Level.Branch[] result = new Level.Branch[properties.length + 1];

            result[0] = new Level.Branch(defaultBranch, new SimpleBooleanProperty(true));

            for (int i = 1; i < result.length; ++i) {
                var property = properties[i - 1];
                result[i] = new Level.Branch(property.getName(), property);
            }

            return result;
        }

        private static Level.Branch[] insert(String defaultBranch, Level.Branch... branches) {
            Level.Branch[] result = new Level.Branch[branches.length + 1];

            result[0] = new Level.Branch(defaultBranch, new SimpleBooleanProperty(true));

            System.arraycopy(branches, 0, result, 1, result.length - 1);

            return result;
        }

        private static Level.Branch[] copy(ReadOnlyBooleanProperty... properties) {
            Level.Branch[] result = new Level.Branch[properties.length];

            for (int i = 0; i < result.length; ++i) {
                var property = properties[i];
                result[i] = new Level.Branch(property.getName(), property);
            }

            return result;
        }

        public record Branch(String name, ObservableBooleanValue value) {
        }
    }
}
