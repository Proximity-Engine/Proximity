package dev.hephaestus.proximity.app.api.controls;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.converter.NumberStringConverter;

public class ColorPicker extends VBox {
    private static final LinearGradient HUE_GRADIENT;

    static {
        Stop[] stops = new Stop[36];

        for (int i = 0; i < 36; ++i) {
            stops[i] = new Stop((1D/36) * i, Color.hsb(i * 10, 1, 1));
        }

        HUE_GRADIENT = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);
    }

    private final DoubleProperty hue = new SimpleDoubleProperty(), saturation = new SimpleDoubleProperty(), brightness = new SimpleDoubleProperty();
    private final Rectangle saturationSelector;
    private final Rectangle hueSelector = new Rectangle(16, 15, Color.CYAN);
    private final Circle hueCircle;
    private final Circle colorCircle;
    private final ObjectProperty<Color> colorProperty = new SimpleObjectProperty<>();

    public ColorPicker(double defaultHue, double defaultSaturation, double defaultBrightness) {
        this.hue.set(defaultHue);
        this.saturation.set(defaultSaturation);
        this.brightness.set(defaultBrightness);

        this.saturationSelector = new Rectangle();
        Rectangle brightness = new Rectangle();

        brightness.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(0, 0, 0, 0)),
                new Stop(1, Color.rgb(0, 0, 0, 1))
        ));

        this.colorCircle = new Circle(8, 8, 8);

        this.colorCircle.setStroke(Color.WHITE);
        this.colorCircle.setStrokeWidth(2);

        StackPane.setAlignment(this.colorCircle, Pos.TOP_LEFT);

        StackPane gradients = new StackPane(this.saturationSelector, brightness, this.colorCircle);

        gradients.setOnMouseClicked(this::setSaturationAndBrightness);
        gradients.setOnMouseDragged(this::setSaturationAndBrightness);

        this.hueSelector.setFill(HUE_GRADIENT);

        this.hueCircle = new Circle(8, 8, 8);

        this.hueCircle.setStroke(Color.WHITE);
        this.hueCircle.setStrokeWidth(2);

        StackPane hueThingy = new StackPane(hueSelector, this.hueCircle);

        StackPane.setAlignment(this.hueCircle, Pos.TOP_CENTER);

        HBox hBox = new HBox(gradients, hueThingy);

        hBox.setSpacing(10);

        HBox.setHgrow(gradients, Priority.ALWAYS);
        this.saturationSelector.widthProperty().bind(gradients.widthProperty());
        this.saturationSelector.heightProperty().bind(gradients.widthProperty());
        brightness.widthProperty().bind(gradients.widthProperty());
        brightness.heightProperty().bind(gradients.widthProperty());
        this.hueSelector.heightProperty().bind(gradients.widthProperty());
        hueThingy.prefHeightProperty().bind(gradients.widthProperty());

        this.hueSelector.setOnMouseClicked(this::setHue);
        this.hueSelector.setOnMouseDragged(this::setHue);

        this.saturationSelector.widthProperty().addListener(this::updateAppearance);

        this.getChildren().add(hBox);

        TextField hueField = new TextField();

        hueField.textProperty().bindBidirectional(this.hue, new NumberStringConverter());
        hueField.getStyleClass().addAll("sidebar-text", "sidebar-text-entry");

        Label hueLabel = new Label("Hue");

        hueLabel.getStyleClass().add("sidebar-text");

        TextField saturationField = new TextField();

        saturationField.textProperty().bindBidirectional(this.saturation, new NumberStringConverter());
        saturationField.getStyleClass().addAll("sidebar-text", "sidebar-text-entry");

        Label saturationLabel = new Label("Saturation");

        saturationLabel.getStyleClass().add("sidebar-text");

        TextField brightnessField = new TextField();


        brightnessField.textProperty().bindBidirectional(this.brightness, new NumberStringConverter());
        brightnessField.getStyleClass().addAll("sidebar-text", "sidebar-text-entry");

        Label brightnessLabel = new Label("Brightness");

        brightnessLabel.getStyleClass().add("sidebar-text");

        GridPane grid = new GridPane();

        this.getChildren().addAll(
                new Rectangle(5, 2.5, Color.TRANSPARENT),
                grid
        );

        grid.add(hueLabel, 0, 0);
        grid.add(hueField, 1, 0);
        grid.add(saturationLabel, 0, 1);
        grid.add(saturationField, 1, 1);
        grid.add(brightnessLabel, 0, 2);
        grid.add(brightnessField, 1, 2);

        GridPane.setFillWidth(hueField, true);
        GridPane.setFillWidth(saturationField, true);
        GridPane.setFillWidth(brightnessField, true);

        GridPane.setHgrow(hueField, Priority.ALWAYS);
        GridPane.setHgrow(saturationField, Priority.ALWAYS);
        GridPane.setHgrow(brightnessField, Priority.ALWAYS);

        grid.setVgap(5);
        grid.setHgap(10);

        grid.prefWidthProperty().bind(this.widthProperty());

        this.hue.addListener(this::updateAppearance);
        this.saturation.addListener(this::updateAppearance);
        this.brightness.addListener(this::updateAppearance);

        this.setSpacing(10);

        this.colorProperty.bind(Bindings.createObjectBinding(() -> {
            return Color.hsb(this.hue.get(), this.saturation.get(), this.brightness.get());
        }, this.hue, this.saturation, this.brightness));
    }

    private void setHue(MouseEvent event) {
        if (event.getY() >= 0 && event.getY() <= this.hueSelector.getHeight()) {
            this.hue.set(event.getY() / this.hueSelector.getHeight());
        }
    }

    private void setSaturationAndBrightness(MouseEvent event) {
        double rX = event.getX() / this.saturationSelector.getWidth();
        double rY = event.getY() / this.saturationSelector.getHeight();

        rX = Math.max(Math.min(rX, 1), 0);
        rY = Math.max(Math.min(rY, 1), 0);

        this.saturation.set(rX);
        this.brightness.set(1 - rY);
    }

    private void updateAppearance(Observable observable) {
        Color color = Color.hsb(this.hue.get() * 360, 1, 1);

        this.saturationSelector.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE),
                new Stop(1, color)
        ));

        this.hueCircle.setFill(color);
        this.hueCircle.setTranslateY(this.hue.get() * this.hueSelector.getHeight() - 8);

        color = Color.hsb(this.hue.get() * 360, this.saturation.get(), this.brightness.get());

        this.colorCircle.setFill(color);
        this.colorCircle.setTranslateX(this.saturation.get() * this.saturationSelector.getWidth() - 8);
        this.colorCircle.setTranslateY((1 - this.brightness.get()) * this.saturationSelector.getHeight() - 8);
    }

    public ObjectProperty<Color> getColorProperty() {
        return this.colorProperty;
    }
}
