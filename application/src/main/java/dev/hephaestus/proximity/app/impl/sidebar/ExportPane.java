package dev.hephaestus.proximity.app.impl.sidebar;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.Template;
import dev.hephaestus.proximity.app.api.plugins.DataWidget;
import dev.hephaestus.proximity.app.api.rendering.Document;
import dev.hephaestus.proximity.app.api.rendering.ImageRenderer;
import dev.hephaestus.proximity.app.api.rendering.Renderer;
import dev.hephaestus.proximity.app.impl.DataRow;
import dev.hephaestus.proximity.app.impl.Sidebar;
import dev.hephaestus.proximity.app.impl.rendering.DocumentImpl;
import javafx.beans.Observable;
import javafx.scene.control.Button;

import java.io.IOException;

public class ExportPane extends SidebarPane {
    public ExportPane() {
        super();

        Button exportAll = new Button("Export All");

        exportAll.onMouseClickedProperty().addListener(this::exportAll);

        this.getChildren().add(exportAll);
    }

    private void exportAll(Observable observable) {
        ImageRenderer imageRenderer = new ImageRenderer();

//        for (DataRow<?> row : this.app.dataEntryArea.rows()) {
//        }
    }

    private <D extends RenderJob, CANVAS> void export(Renderer<CANVAS> renderer, DataRow<D> row) {
        for (DataWidget.Entry<D> entry : row) {
            Template<D> template = entry.template().getValue();
            CANVAS canvas = renderer.createCanvas(template.getWidth(), template.getHeight());

            Document<D> document = new DocumentImpl<>(entry.getValue(), template, entry.getWidget().getErrorProperty());

            try {
                renderer.render(document, canvas);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
