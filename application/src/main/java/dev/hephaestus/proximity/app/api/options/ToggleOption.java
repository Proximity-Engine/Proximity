package dev.hephaestus.proximity.app.api.options;

import dev.hephaestus.proximity.app.api.Option;
import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.controls.SwitchButton;
import dev.hephaestus.proximity.app.impl.Appearance;
import dev.hephaestus.proximity.json.api.Json;
import dev.hephaestus.proximity.json.api.JsonElement;
import javafx.beans.property.Property;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

public class ToggleOption<D extends RenderJob<?>> extends Option<Boolean, ToggleOption<D>.Widget, D> {
    public ToggleOption(String id, Boolean defaultValue) {
        super(id, defaultValue);
    }

    @Override
    public JsonElement toJson(Boolean value) {
        return Json.create(value);
    }

    @Override
    public Boolean fromJson(JsonElement json) {
        return json.asBoolean();
    }

    @Override
    public Widget createControl(D renderJob) {
        return new Widget(renderJob);
    }

    public class Widget extends HBox implements Option.Widget<Boolean> {
        private final SwitchButton switchButton;

        private Widget(RenderJob<?> renderJob) {
            Label label = new Label(ToggleOption.this.getId());
            Pane spacer = new Pane();
            this.switchButton = new SwitchButton(8, 40, 20, Color.GREEN, Appearance.BASE.brighter().brighter().brighter(), renderJob.getOption(ToggleOption.this));

            this.getChildren().addAll(label, spacer, this.switchButton);

            HBox.setHgrow(spacer, Priority.ALWAYS);
            label.getStyleClass().add("sidebar-text");
            this.getStyleClass().add("sidebar-entry");
        }

        @Override
        public Property<Boolean> getValueProperty() {
            return this.switchButton.getState();
        }
    }
}
