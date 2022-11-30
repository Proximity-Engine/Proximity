package dev.hephaestus.proximity.app.impl.sidebar;

import dev.hephaestus.proximity.app.impl.Initializable;
import dev.hephaestus.proximity.app.impl.Proximity;
import dev.hephaestus.proximity.app.impl.skins.SidebarButtonSkin;
import dev.hephaestus.proximity.app.impl.skins.TooltipSkin;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;

public class Category extends Button implements Initializable {
    public static final String ACTIVE = "proximity:active";

    private final Tooltip tooltip = new Tooltip();
    private SidebarPane pane;

    public Category() {
        this.setSkin(new SidebarButtonSkin(this));
    }

    public void initialize() {
        Tooltip.install(this, this.tooltip);
        this.tooltip.setSkin(new TooltipSkin(this.tooltip));

        ImageView graphic = (ImageView) this.getGraphic();

        graphic.setX(8);
        graphic.setY(8);
        graphic.setFitWidth(24);
        graphic.setFitHeight(24);

        Pane sidebar = (Pane) this.getParent();

        this.prefWidthProperty().bind(sidebar.prefWidthProperty());
        this.prefHeightProperty().bind(sidebar.prefWidthProperty());
        this.maxWidthProperty().bind(sidebar.prefWidthProperty());
        this.maxHeightProperty().bind(sidebar.prefWidthProperty());

        this.setPadding(new Insets(0, 0, 0, 0));

        this.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (Proximity.isSidebarExpanded() && Proximity.getActiveSidebarPane() != this.pane) {
                    Proximity.getActiveSidebarPane().getButton().getProperties().put(ACTIVE, false);
                    Proximity.getActiveSidebarPane().getButton().setOpacity(0.5);
                    Proximity.getActiveSidebarPane().setVisible(false);
                    Proximity.getActiveSidebarPane().setManaged(false);
                    Proximity.setActiveSidebarPane(this.pane);
                    this.getProperties().put(ACTIVE, true);

                    this.pane.setVisible(true);
                    this.pane.setManaged(true);
                } else if (Proximity.isSidebarExpanded()) {
                    Proximity.setActiveSidebarPane(null);
                    this.getProperties().put(ACTIVE, false);

                    this.pane.setVisible(false);
                    this.pane.setManaged(false);
                } else {
                    Proximity.setActiveSidebarPane(this.pane);
                    this.getProperties().put(ACTIVE, true);
                    this.setOpacity(1);

                    this.pane.setManaged(true);
                    this.pane.setVisible(true);
                }
            }
        });

        if (this.pane != null) {
            Proximity.add(this.pane);
            pane.setButton(this);
        }
    }

    public SidebarPane getPane() {
        return pane;
    }

    public void setPane(SidebarPane pane) {
        this.pane = pane;
        pane.setButton(this);
    }

    public String getName() {
        return this.tooltip.getText();
    }

    public void setName(String name) {
        this.tooltip.setText(name);
    }

    public StringProperty nameProperty() {
        return this.tooltip.textProperty();
    }
}
