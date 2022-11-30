package dev.hephaestus.proximity.app.impl;

import dev.hephaestus.proximity.app.api.Option;
import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.Template;
import dev.hephaestus.proximity.app.api.plugins.DataWidget;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

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

    public void createWidgets(DataWidget.Entry<D> entry, ObservableList<Node> pane) {
        List<Node> optionControls = new ArrayList<>();

        for (Option<?, ?, ? super D> option : this.options) {
            optionControls.add(this.createWidget(entry, option));
        }

        for (Category category : this.categories.values()) {
            VBox box = new VBox();
            TitledPane categoryPane = new TitledPane(category.id, box);

            for (Option<?, ?, ? super D> option : category.options) {
                box.getChildren().add(this.createWidget(entry, option));
            }

            pane.add(categoryPane);

            categoryPane.setExpanded(!category.expandedByDefault);
        }

        pane.setAll(optionControls);
    }

    private <T, W extends Node & Option.Widget<T>, D extends RenderJob> W createWidget(DataWidget.Entry<D> entry, Option<T, W, ? super D> option) {
        D job = entry.getValue();
        W control = option.createControl(job);

        control.getValueProperty().addListener((observable, oldValue, newValue) -> {
            option.setValue(job, newValue);
            Proximity.rerender(entry);
        });

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
