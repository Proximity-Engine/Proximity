package dev.hephaestus.proximity.app.impl;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.io.IOException;

public class Spinner extends ImageView {
    public Spinner() {
        RotateTransition transition = new RotateTransition(Duration.millis(2000), this);

        transition.setFromAngle(0);
        transition.setToAngle(360);
        transition.setInterpolator(Interpolator.LINEAR);
        transition.setCycleCount(Timeline.INDEFINITE);

        transition.play();

        try {
            this.setImage(new Image(this.getClass().getModule().getResourceAsStream("spinner.png")));
        } catch (IOException e) {
            Proximity.print(e);
        }
    }
}
