package dev.hephaestus.proximity.app.impl;

import dev.hephaestus.proximity.app.impl.sidebar.SidebarPane;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.VBox;

public class Sidebar extends VBox {

    private final SimpleObjectProperty<SidebarPane> activeCategory = new SimpleObjectProperty<>();

    public Sidebar() {
        this.setPrefWidth(38);
        this.setMaxWidth(38);
        this.setMinWidth(38);
        this.setBackground(Appearance.SIDEBAR);
    }

    public boolean isExpanded() {
        return this.activeCategory.get() != null;
    }

    public SidebarPane getActiveCategory() {
        return this.activeCategory.get();
    }

    public void setActiveCategory(SidebarPane sidebarPane) {
        this.activeCategory.setValue(sidebarPane);
    }
}
