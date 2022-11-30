package dev.hephaestus.proximity.utils;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class AppUtils {
    private AppUtils() {
    }

    public static Background background(int color, boolean hasAlpha) {
        double a = hasAlpha ? ((color >> 24) & 0xFF) / 255D : 1.0;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        return new Background(new BackgroundFill(Color.rgb(r, g, b, a), CornerRadii.EMPTY, Insets.EMPTY));
    }

    public static Background background(int color) {
        return background(color, false);
    }

    public static Background background(Color color) {
        return new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY));
    }

    public static <T extends Pane> T create(Supplier<T> constructor, Consumer<T> childCreator) {
        T pane = constructor.get();

        childCreator.accept(pane);

        return pane;
    }
}
