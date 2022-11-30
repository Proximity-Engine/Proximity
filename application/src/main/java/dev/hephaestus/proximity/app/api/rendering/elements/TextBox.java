package dev.hephaestus.proximity.app.api.rendering.elements;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.text.TextStyle;
import dev.hephaestus.proximity.app.api.rendering.properties.ListProperty;
import dev.hephaestus.proximity.app.api.rendering.properties.Property;
import dev.hephaestus.proximity.app.api.rendering.properties.TextProperty;
import dev.hephaestus.proximity.app.api.rendering.properties.VisibilityProperty;
import dev.hephaestus.proximity.app.api.rendering.util.Padding;

import java.awt.Shape;

public interface TextBox<D extends RenderJob> extends Child<D> {
    Property<D, Integer, TextBox<D>> x();
    Property<D, Integer, TextBox<D>> y();
    Property<D, Integer, TextBox<D>> width();
    Property<D, Integer, TextBox<D>> height();
    Property<D, TextStyle, TextBox<D>> style();
    TextProperty<D, TextBox<D>> text();
    ListProperty<D, Shape, TextBox<D>> wraps();
    Property<D, Padding, TextBox<D>> padding();
    Property<D, Float, TextBox<D>> lineSpacing();
    Property<D, Float, TextBox<D>> spaceAfterParagraph();

    VisibilityProperty<D, TextBox<D>> visibility();

    boolean isOutOfBounds();

    void layout(Text.Consumer consumer);
}
