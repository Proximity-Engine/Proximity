package dev.hephaestus.proximity.app.impl;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.Template;
import dev.hephaestus.proximity.app.api.logging.Log;
import dev.hephaestus.proximity.app.api.plugins.DataProvider;
import dev.hephaestus.proximity.app.api.plugins.DataWidget;
import dev.hephaestus.proximity.app.impl.skins.TooltipSkin;
import dev.hephaestus.proximity.json.api.JsonElement;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Iterator;

public final class DataRow<D extends RenderJob<?>> extends HBox implements Iterable<DataWidget.Entry<D>> {
    private final StackPane status;
    private final DataEntryArea dataEntryArea;

    private DataWidget<D> dataWidget;

    public DataRow(DataEntryArea rows) {
        //noinspection unchecked
        this(rows, ((DataProvider<D>) Proximity.getDataProvider()).createDataEntryElement(new DataProvider.Context() {
            @Override
            public Log log() {
                return Proximity.deriveLogger(Proximity.getDataProviderPluginID());
            }

            @Override
            public JsonElement data() {
                return null;
            }
        }));
    }

    public DataWidget<D> getDataWidget() {
        return this.dataWidget;
    }

    public DataRow(DataEntryArea dataEntryArea, DataWidget<D> widget) {
        this.dataEntryArea = dataEntryArea;
        this.dataWidget = widget;
        this.setPadding(new Insets(10));

        Rectangle rect = new Rectangle(25, 25, Color.RED);
        this.status = new StackPane(new Rectangle(25, 25, Color.TRANSPARENT), rect);

        try {
            ImageView view = new ImageView(new Image(this.getClass().getModule().getResourceAsStream("icons/alert.png")));

            view.setFitWidth(25);
            view.setFitHeight(25);

            rect.setClip(view);
        } catch (IOException e) {
            Proximity.print(e);
        }

        AnchorPane statusPane = new AnchorPane(status);

        statusPane.setPrefWidth(25);
        statusPane.setPrefHeight(25);

        Background background = dataEntryArea.rowCount() % 2 == 1
                ? Background.fill(Color.web("#33333380"))
                : Background.EMPTY;

        this.setBackground(background);

        this.onMousePressedProperty().addListener(this::select);

        this.getChildren().addAll(this.status, this.dataWidget.getRootPane());

        HBox.setHgrow(this.dataWidget.getRootPane(), Priority.ALWAYS);

        this.dataWidget.getEntries().addListener((observable, oldValue, newValue) -> {
            if (newValue == oldValue) return;

            for (DataWidget.Entry<D> entry : this.dataWidget) {
                for (Template<?> template : Proximity.templates()) {
                    if (template.canHandle(entry.getValue())) {
                        //noinspection unchecked
                        entry.template().setValue((Template<D>) template);
                        break;
                    }
                }

                if (entry.template().getValue() == null) {
                    entry.getWidget().getErrorProperty().add(String.format("Supported template not found for '%s'", entry.get().getName()));
                }
            }

            if (this.dataWidget.size() == 1) {
                Proximity.select(this.dataWidget.get(0));
            }

            if (newValue == null || newValue.isEmpty()) {
                Proximity.clearPreview();
            } else {
                DataRow<D> r1 = DataRow.this;
                Node r2 = this.dataEntryArea.getRow(this.dataEntryArea.rowCount() - 1);

                if (!this.dataWidget.isEmpty() && r1 == r2) {
                    this.dataEntryArea.addRow();
                }
            }
        });

        this.status.visibleProperty().bind(Bindings.createBooleanBinding(() -> !this.dataWidget.getErrorProperty().isEmpty(), this.dataWidget.getErrorProperty()));

        Tooltip tooltip = new Tooltip();

        tooltip.textProperty().bind(Bindings.createStringBinding(() -> String.join("\n", this.dataWidget.getErrorProperty()), this.dataWidget.getErrorProperty()));

        Tooltip.install(this.status, tooltip);

        tooltip.setSkin(new TooltipSkin(tooltip));
    }

    public boolean represents(DataWidget<?> widget) {
        return widget == this.dataWidget;
    }

    private void select(Observable observable) {
        if (this.dataWidget.isEmpty()) {
            Proximity.select(this);
        }
    }

    @NotNull
    @Override
    public Iterator<DataWidget.Entry<D>> iterator() {
        return this.dataWidget.iterator();
    }

    public boolean isEmpty() {
        return this.dataWidget.isEmpty();
    }
}
