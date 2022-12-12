package dev.hephaestus.proximity.app.api.rendering.elements;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.rendering.properties.Property;
import dev.hephaestus.proximity.app.api.rendering.properties.TextProperty;
import dev.hephaestus.proximity.app.api.rendering.properties.VisibilityProperty;
import dev.hephaestus.proximity.app.api.rendering.util.Alignment;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBox;
import dev.hephaestus.proximity.app.api.text.TextComponent;
import dev.hephaestus.proximity.app.api.text.TextStyle;
import dev.hephaestus.proximity.app.api.text.Word;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface Text<D extends RenderJob<?>> extends Child<D>, Iterable<Word> {
    Property<D, Integer, Text<D>> x();
    Property<D, Integer, Text<D>> y();
    Property<D, TextStyle, Text<D>> style();
    Property<D, Alignment, Text<D>> alignment();
    TextProperty<D, Text<D>> text();

    VisibilityProperty<D, Text<D>> visibility();

    void layout(Consumer consumer);

    interface Consumer {
        void render(TextComponent component, BoundingBox bounds, int x, int y);
    }
}
