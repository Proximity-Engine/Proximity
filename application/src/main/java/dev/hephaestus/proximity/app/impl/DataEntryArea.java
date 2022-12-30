package dev.hephaestus.proximity.app.impl;

import dev.hephaestus.proximity.app.api.plugins.DataProvider;
import dev.hephaestus.proximity.app.api.plugins.DataWidget;
import dev.hephaestus.proximity.app.api.rendering.RenderData;
import dev.hephaestus.proximity.app.api.rendering.Template;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class DataEntryArea extends StackPane {
    private final VBox content = new VBox();
    private final VBox rowPane = new VBox();
    private final StackPane pauseShade = new StackPane();
    private final ScrollPane scrollPane = new ScrollPane(this.rowPane);
    private final Label pauseText = new Label("WHAT THE FUCK");
    private final List<DataRow<?>> rows = new ArrayList<>();

    public DataEntryArea() {
        super();
        this.getStyleClass().add("data-entry");
        this.setBackground(Appearance.DATA_ENTRY);

        this.scrollPane.setBackground(Appearance.DATA_ENTRY);
        this.rowPane.setBackground(Appearance.DATA_ENTRY);
        this.rowPane.setFillWidth(true);
        this.scrollPane.setFitToWidth(true);

        this.pauseShade.setBackground(Background.fill(Color.color(0, 0, 0, 0.5)));

        Spinner spinner = new Spinner();

        this.pauseText.setTextFill(Color.WHITE);
        this.pauseText.setTranslateY(50);

        StackPane.setAlignment(this.pauseText, Pos.CENTER);

        this.pauseShade.getChildren().addAll(spinner, this.pauseText);
        this.getChildren().addAll(this.content, this.pauseShade);

        this.pauseShade.visibleProperty().bind(Proximity.getPausedProperty());

        DataProvider<?> dataProvider = Proximity.getDataProvider();

        if (dataProvider != null) {
            Pane header = dataProvider.createHeaderElement();

            header.setPadding(new Insets(10));

            HBox.setHgrow(header, Priority.ALWAYS);

            this.content.getChildren().setAll(new HBox(new Rectangle(25, 25, Color.TRANSPARENT), header), this.scrollPane);

            this.addRow();
        }
    }

    public void addRow() {
        DataRow<?> row = new DataRow<>(this);

        this.rows.add(row);
        this.rowPane.getChildren().add(row);
    }

    @NotNull
    public Iterable<DataRow<?>> rows() {
        return this.rows;
    }

    public int rowCount() {
        return this.rows.size();
    }

    public <D extends RenderData> void add(DataWidget<D> widget) {
        this.add(Collections.singletonList(widget), false);
    }

    public void add(List<DataWidget<?>> widgets, boolean clear) {
        if (this.rows.get(this.rows.size() - 1).isEmpty()) {
            this.rowPane.getChildren().remove(this.rows.size() - 1);
            this.rows.remove(this.rows.size() - 1);
        }

        Collection<DataRow<?>> rows = new ArrayList<>(widgets.size());

        for (DataWidget<?> widget : widgets) {
            this.add(widget, rows);
        }

        if (clear) {
            this.rowPane.getChildren().setAll(rows);
        } else {
            this.rowPane.getChildren().addAll(rows);
        }

        if (!widgets.get(widgets.size() - 1).isEmpty()) {
            this.addRow();
        }
    }

    private <D extends RenderData> void add(DataWidget<D> widget, Collection<DataRow<?>> rows) {
        DataRow<D> row = new DataRow<>(this, widget);

        this.rows.add(row);
        rows.add(row);

        for (DataWidget<D>.Entry entry : widget) {
            for (Template<?> template : Proximity.templates()) {
                if (template.canHandle(entry.getValue())) {
                    //noinspection unchecked
                    entry.template().setValue((Template<D>) template);
                    break;
                }
            }

            if (entry.template().getValue() == null) {
                entry.getWidget().getErrorProperty().add(String.format("Supported template not found for '%s'", entry.get().getClass().getName()));
            }
        }
    }

    public DataRow<?> getRow(int i) {
        return this.rows.get(i);
    }

    public void setPauseText(String text) {
        this.pauseText.setText(text);
    }

    public void clear() {
        this.rows.clear();
        this.getChildren().clear();
    }
}
