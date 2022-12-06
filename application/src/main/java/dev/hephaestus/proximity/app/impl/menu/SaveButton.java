package dev.hephaestus.proximity.app.impl.menu;

import dev.hephaestus.proximity.app.impl.Initializable;
import dev.hephaestus.proximity.app.impl.Project;
import dev.hephaestus.proximity.app.impl.Proximity;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;

public class SaveButton extends MenuItem implements Initializable {
    public void initialize() {
        this.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.ModifierValue.UP, KeyCombination.ModifierValue.DOWN, KeyCombination.ModifierValue.UP, KeyCombination.ModifierValue.UP, KeyCombination.ModifierValue.UP));
        this.setOnAction(ev -> {
            Project project = Proximity.getCurrentProject();
            Path path = project.getPath();

            if (path == null) {
                FileChooser fileChooser = new FileChooser();
                Path lastSavedDirectory = Proximity.getLastSavedDirectory();

                fileChooser.setTitle("Save Project");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Proximity Project", "*.pxproject"));

                if (lastSavedDirectory != null) {
                    fileChooser.setInitialDirectory(lastSavedDirectory.toFile());
                }

                File file = fileChooser.showSaveDialog(Proximity.getWindow());

                if (file != null) {
                    Proximity.setLastSavedDirectory(file.getParentFile().toPath());
                    path = file.toPath();
                }
            }

            if (path != null) {
                project.setPath(path);
                project.save();
            }
        });
    }
}
