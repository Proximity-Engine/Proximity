package dev.hephaestus.proximity.app.impl.skins;

import dev.hephaestus.proximity.app.impl.sidebar.Category;
import javafx.scene.control.Button;
import javafx.scene.control.skin.ButtonSkin;
import javafx.scene.layout.Background;

public class SidebarButtonSkin extends ButtonSkin {
    public SidebarButtonSkin(Button control) {
        super(control);

        control.setOnMouseEntered(e -> control.setOpacity(1));
        control.setOnMouseExited(e -> {
            if (!((Boolean) control.getProperties().getOrDefault(Category.ACTIVE, false))) {
                control.setOpacity(0.5);
            }
        });

        control.setOpacity(0.5);

        control.setBackground(Background.EMPTY);
    }
}
