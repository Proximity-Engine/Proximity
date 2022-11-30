package dev.hephaestus.proximity.app.api.controls;

import javafx.animation.FillTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class SwitchButton extends StackPane {
    private final int radius, width, height;
    private final Color onColor, offColor;
    private final Button button;
    private final Rectangle slot;

    private final BooleanProperty state = new SimpleBooleanProperty();

    private Rectangle createSlot() {
        Rectangle slot = new Rectangle(this.width, this.height);

        this.getChildren().add(slot);
        slot.setArcHeight(slot.getHeight());
        slot.setArcWidth(slot.getHeight());
        slot.setFill(this.state.get() ? this.onColor : this.offColor);

        return slot;
    }

    public SwitchButton(int radius, int width, int height, Color onColor, Color offColor, boolean state) {
        this.state.setValue(state);
        this.radius = radius;
        this.width = width;
        this.height = height;
        this.onColor = onColor;
        this.offColor = offColor;
        this.slot = this.createSlot();
        Circle circle = new Circle(this.radius);

        this.button = new Button("", circle);

        this.getChildren().add(this.button);

        circle.setFill(Color.WHITE);

        setAlignment(this.button, Pos.CENTER_LEFT);
        this.button.setMaxSize(this.radius * 2, this.radius * 2);
        this.button.setMinSize(this.radius * 2, this.radius * 2);
        this.setStyle("-fx-cursor: hand;");

        this.button.getStyleClass().add("switch-button");
        this.button.setFocusTraversable(false);
        this.button.setOnMouseClicked(this::handleMouseClicked);
        this.button.setTranslateX(this.state.get() ? this.width - this.radius * 2 - ((this.height / 2D) - this.radius) : (this.height / 2D - this.radius));

        setOnMouseClicked(this::handleMouseClicked);

        this.state.addListener(this::update);
    }

    private void update(Observable observable) {
        TranslateTransition translate = new TranslateTransition(Duration.millis(50), this.button);

        translate.setToX(this.state.getValue() ? (this.getWidth() - button.getWidth() - (this.height / 2D - this.radius)) : (this.height / 2D - this.radius));
        translate.play();

        FillTransition fill = new FillTransition(Duration.millis(50), this.slot);

        fill.setToValue(this.state.getValue() ? this.onColor : this.offColor);
        fill.play();
    }

    private void handleMouseClicked(Event e) {
        this.state.setValue(!this.state.getValue());
    }

    public void setValue(boolean value) {
        this.state.setValue(value);
    }

    public BooleanProperty getState() {
        return this.state;
    }
}