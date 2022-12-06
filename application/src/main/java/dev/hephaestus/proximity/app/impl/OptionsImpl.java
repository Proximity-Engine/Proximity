package dev.hephaestus.proximity.app.impl;

import dev.hephaestus.proximity.app.api.Option;
import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.Template;
import dev.hephaestus.proximity.app.api.plugins.DataWidget;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class OptionsImpl<D extends RenderJob> implements Template.Options<D> {
    protected final List<Option<?, ?, ? super D>> options = new ArrayList<>();
    protected final Map<String, Category> categories = new LinkedHashMap<>();

    @Override
    public void add(Option<?, ?, ? super D> option) {
        this.options.add(option);
    }

    @Override
    public void add(Option<?, ?, ? super D> option, String category) {
        this.categories.computeIfAbsent(category, c -> new Category(c, false)).options.add(option);
    }

    @Override
    public void category(String id, Consumer<Template.Options<D>> category) {
        this.category(id, false, category);
    }

    @Override
    public void category(String id, boolean expandedByDefault, Consumer<Template.Options<D>> category) {
        category.accept(this.categories.computeIfAbsent(id, c -> new Category(id, expandedByDefault)));
    }

    public void createWidgets(DataWidget.Entry<D> entry, ObservableList<Node> options, ObservableList<Node> categories) {
        List<Node> optionControls = new ArrayList<>(this.options.size());

        for (Option<?, ?, ? super D> option : this.options) {
            optionControls.add(this.createWidget(entry, option));
        }

        Node spacer = new AnchorPane();

        VBox.setVgrow(spacer, Priority.ALWAYS);

        optionControls.add(spacer);

        options.setAll(optionControls);

        List<Node> categoryPanes = new ArrayList<>(this.categories.size());

        for (Category category : this.categories.values()) {
            GridPane grid = new GridPane();
            TitledPane categoryPane = new TitledPane(category.id, grid);

            grid.setVgap(5);
            grid.setHgap(5);

            List<Region> widgets = new ArrayList<>(category.options.size());

            for (Option<?, ?, ? super D> option : category.options) {
                Label label = new Label(option.getId());

                label.getStyleClass().add("sidebar-text");

                Node node = this.createWidget(entry, option);

                GridPane.setHgrow(label, Priority.ALWAYS);

                grid.addRow(grid.getRowCount(), label, node);

                if (node instanceof Region region) {
                    widgets.add(region);
                }
            }

            DoubleBinding maxWidthBinding = Bindings.createDoubleBinding(() -> {
                return widgets.stream().map(Region::widthProperty).map(ReadOnlyDoubleProperty::get).max(Double::compareTo).orElse(0D);
            }, widgets.stream().map(Region::widthProperty).toList().toArray(new ReadOnlyDoubleProperty[widgets.size()]));

            for (Region widget : widgets) {
                widget.minWidthProperty().bind(maxWidthBinding);
            }

            categoryPane.setAnimated(false);

            categoryPanes.add(categoryPane);

            categoryPane.setExpanded(category.expandedByDefault);
        }

        categories.setAll(categoryPanes);
    }

    private <T, W extends Node & Option.Widget<T>> W createWidget(DataWidget.Entry<D> entry, Option<T, W, ? super D> option) {
        D job = entry.getValue();
        W control = option.createControl(job);
        Property<T> property = job.getOptionProperty(option);

        control.getValueProperty().setValue(property.getValue());

        control.getValueProperty().addListener(((observable, oldValue, newValue) -> {
            property.setValue(newValue);
            Proximity.rerender(entry);
        }));

        return control;
    }

    private class Category extends OptionsImpl<D> {
        private final String id;
        private final boolean expandedByDefault;

        private Category(String id, boolean expandedByDefault) {
            this.id = id;
            this.expandedByDefault = expandedByDefault;
        }
    }
}
