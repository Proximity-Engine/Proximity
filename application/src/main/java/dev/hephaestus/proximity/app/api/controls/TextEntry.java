package dev.hephaestus.proximity.app.api.controls;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class TextEntry extends HBox {
    public final Label label = new Label();
    public final TextField textField = new TextField();

    public TextEntry() {
        this.label.getStyleClass().add("sidebar-text");

        HBox hBox = new HBox(this.textField);

        this.getChildren().addAll(this.label, hBox);
    }
}
