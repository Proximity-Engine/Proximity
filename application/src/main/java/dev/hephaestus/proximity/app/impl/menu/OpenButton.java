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

public class OpenButton extends MenuItem implements Initializable {
    public void initialize() {
        this.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.ModifierValue.UP, KeyCombination.ModifierValue.DOWN, KeyCombination.ModifierValue.UP, KeyCombination.ModifierValue.UP, KeyCombination.ModifierValue.UP));
        this.setOnAction(ev -> {
            FileChooser fileChooser = new FileChooser();

            fileChooser.setTitle("Open Project");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Proximity Project", "*.pxproject"));

            File file = fileChooser.showOpenDialog(this.getGraphic().getScene().getWindow());

            if (file != null) {
                Project project = Proximity.getCurrentProject();

                project.setPath(file.toPath());
                project.save();
            }
        });
    }
}
