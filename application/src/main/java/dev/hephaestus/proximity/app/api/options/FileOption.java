package dev.hephaestus.proximity.app.api.options;

import dev.hephaestus.proximity.app.api.Option;
import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.impl.Proximity;
import dev.hephaestus.proximity.json.api.Json;
import dev.hephaestus.proximity.json.api.JsonElement;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;

public class FileOption<D extends RenderJob> extends Option<Path, FileOption<D>.Widget, D> {
    private final FileChooser.ExtensionFilter[] filters;

    public FileOption(String id, Path defaultValue, FileChooser.ExtensionFilter... filters) {
        super(id, defaultValue);
        this.filters = filters;
    }

    public FileOption(String id, Path defaultValue, String... extensions) {
        super(id, defaultValue);
        this.filters = new FileChooser.ExtensionFilter[extensions.length];

        for (int i = 0; i < extensions.length; i++) {
            String extension = extensions[i];
            this.filters[i] = new FileChooser.ExtensionFilter(extension, extension);
        }
    }

    @Override
    public JsonElement toJson(Path value) {
        return Json.create(value.toString());
    }

    @Override
    public Path fromJson(JsonElement json) {
        return Path.of(json.asString());
    }

    @Override
    public Widget createControl(D renderJob) {
        return new Widget(renderJob);
    }

    public class Widget extends HBox implements Option.Widget<Path> {
        private final Property<Path> value = new SimpleObjectProperty<>();

        @Override
        public Property<Path> getValueProperty() {
            return this.value;
        }

        public Widget(D renderJob) {
            super();
            Label label = new Label(FileOption.this.getId());
            Pane spacer = new Pane();
            Button button = new Button();

            label.getStyleClass().add("sidebar-text");
            this.getStyleClass().add("sidebar-entry");

            this.getChildren().addAll(label, spacer, button);

            HBox.setHgrow(spacer, Priority.ALWAYS);

            button.setText("Open...");

            button.setOnAction(event -> {
                FileChooser fileChooser = new FileChooser();

                fileChooser.setTitle("Open File");
                fileChooser.getExtensionFilters().addAll(FileOption.this.filters);

                File file = fileChooser.showOpenDialog(Proximity.getWindow());

                if (file == null) {
                    this.value.setValue(null);
                } else {
                    this.value.setValue(file.toPath());
                    renderJob.getOptionProperty(FileOption.this).bind(this.value);
                }
            });
        }
    }
}
