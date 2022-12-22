package dev.hephaestus.proximity.app.api.options;

import dev.hephaestus.proximity.app.api.Option;
import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.controls.ColorPicker;
import dev.hephaestus.proximity.json.api.Json;
import dev.hephaestus.proximity.json.api.JsonElement;
import dev.hephaestus.proximity.json.api.JsonString;
import javafx.beans.property.Property;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class ColorOption<D extends RenderJob<?>> extends Option<Color, ColorOption<D>.Widget, D> {
    public ColorOption(String id, Color defaultValue) {
        super(id, defaultValue);
    }

    @Override
    public JsonElement toJson(Color value) {
        return Json.create(String.format("#%02X%02X%02X",
                (int) (value.getRed() * 255),
                (int) (value.getGreen() * 255),
                (int) (value.getBlue() * 255)));
    }

    @Override
    public Color fromJson(JsonElement json) {
        return Color.web(((JsonString) json).get());
    }

    @Override
    public Widget createControl(D renderJob) {
        return new Widget(renderJob);
    }

    public class Widget extends VBox implements Option.Widget<Color> {
        private final ColorPicker picker;

        private Widget(RenderJob<?> renderJob) {
            Label label = new Label(ColorOption.this.getId());

            label.getStyleClass().add("sidebar-text");

            Color color = renderJob.getOption(ColorOption.this);

            this.picker = new ColorPicker(
                    color.getHue(),
                    color.getSaturation(),
                    color.getBrightness()
            );

            this.getChildren().addAll(label, picker);

            this.setSpacing(5);
            this.getStyleClass().add("sidebar-entry");
        }

        @Override
        public Property<Color> getValueProperty() {
            return this.picker.getColorProperty();
        }
    }
}
