package dev.hephaestus.proximity.app.api.rendering;

import dev.hephaestus.proximity.app.api.rendering.elements.Group;
import javafx.collections.ObservableList;

public interface Document<D extends RenderData> {
    D getRenderData();
    Template<D> getTemplate();
    ObservableList<String> getErrors();
    Group getElements();
}
