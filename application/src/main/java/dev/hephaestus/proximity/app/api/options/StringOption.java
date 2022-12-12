package dev.hephaestus.proximity.app.api.options;

import dev.hephaestus.proximity.app.api.Option;
import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.json.api.Json;
import dev.hephaestus.proximity.json.api.JsonElement;
import javafx.beans.property.Property;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

import java.util.function.Function;

public class StringOption<D extends RenderJob<?>> extends Option<String, StringOption<D>.Widget, D> {
    public StringOption(String id, String defaultValue) {
        super(id, defaultValue);
    }

    public StringOption(String id, Function<D, String> defaultValue) {
        super(id, defaultValue);
    }

    @Override
    public JsonElement toJson(String value) {
        return Json.create(value);
    }

    @Override
    public String fromJson(JsonElement json) {
        return json.asString();
    }

    @Override
    public Widget createControl(D renderJob) {
        return new Widget();
    }

    public class Widget extends HBox implements Option.Widget<String> {
        private final TextField field = new TextField();

        public Widget() {
            super();
            Label label = new Label(StringOption.this.getId());
            Pane spacer = new Pane();

            label.getStyleClass().add("sidebar-text");
            this.getStyleClass().add("sidebar-entry");

            this.getChildren().addAll(label, spacer, this.field);

            this.field.getStyleClass().add("string-entry");

            HBox.setHgrow(spacer, Priority.ALWAYS);
        }

        @Override
        public Property<String> getValueProperty() {
            return this.field.textProperty();
        }
    }
}
