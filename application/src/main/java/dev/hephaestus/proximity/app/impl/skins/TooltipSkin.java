package dev.hephaestus.proximity.app.impl.skins;

import javafx.animation.FadeTransition;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

public class TooltipSkin extends javafx.scene.control.skin.TooltipSkin {
    /**
     * Creates a new TooltipSkin instance for the given {@link Tooltip}.
     *
     * @param t the tooltip
     */
    public TooltipSkin(Tooltip t) {
        super(t);

        FadeTransition fadeIn = new FadeTransition(new Duration(100));

        fadeIn.setToValue(1);
        fadeIn.setFromValue(0);

        FadeTransition fadeOut = new FadeTransition(new Duration(100));

        fadeOut.setToValue(0);
        fadeOut.setFromValue(1);


        t.setShowDelay(new Duration(100));
        t.setHideDelay(Duration.ZERO);

        t.activatedProperty().addListener(invalidation -> {
            if (t.activatedProperty().getValue()) {
                fadeIn.playFromStart();
            } else {
                fadeOut.playFromStart();
            }
        });
    }
}
