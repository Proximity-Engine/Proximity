package dev.hephaestus.proximity.app.impl.sidebar;

import dev.hephaestus.proximity.app.impl.Initializable;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Locale;

public class SidebarPane extends VBox implements Initializable {
    private final Label label = new Label();

    private Category button;

    public SidebarPane() {
        super();
        this.setPrefWidth(350);

        this.label.getStyleClass().add("sidebar-text");
        this.label.getStyleClass().add("sidebar-header");

        this.getStyleClass().add("sidebar-pane");

        this.setVisible(false);
        this.setManaged(false);

        this.getChildren().addAll(label);
    }

    public Category getButton() {
        return button;
    }

    public void setButton(Category button) {
        this.button = button;
    }

    @Override
    public void initialize() {
        // TODO: Translate
        this.label.textProperty().bind(Bindings.createStringBinding(() -> this.button.getName().toUpperCase(Locale.ROOT), this.button.nameProperty()));
    }
}
