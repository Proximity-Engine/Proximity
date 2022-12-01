package dev.hephaestus.proximity.app.impl.sidebar;

import dev.hephaestus.proximity.app.api.Option;
import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.Template;
import dev.hephaestus.proximity.app.api.plugins.DataWidget;
import dev.hephaestus.proximity.app.impl.OptionsImpl;
import dev.hephaestus.proximity.app.impl.Proximity;
import dev.hephaestus.proximity.app.impl.rendering.DocumentImpl;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class OptionsPane extends SidebarPane {
    private final VBox options = new VBox();
    private final VBox categories = new VBox();

    private final ComboBox<Template<?>> templateSelector = new ComboBox<>();
    private final ChangeListener<Template<?>> templateChangeListener = (observable, oldValue, newValue) -> {
        this.options.getChildren().clear();
        this.categories.getChildren().clear();

        if (newValue != null) {
            this.setTemplate(newValue);
        }
    };

    private final ChangeListener<DocumentImpl<?>> documentChangeListener = ((observable, oldValue, newValue) -> {
        this.updateDocument(newValue);
    });

    private <D extends RenderJob> void updateDocument(DocumentImpl<D> document) {
        //noinspection unchecked
        DataWidget.Entry<D> entry = (DataWidget.Entry<D>) this.selected;

        Proximity.rerender(entry);

        OptionsImpl<D> options = new OptionsImpl<>();

        document.getTemplate().createOptions(options);

        for (Option<?, ?, D> option : document.getSelectorOverrides()) {
            options.add(option, "Advanced");
        }

        options.createWidgets(entry, this.options.getChildren(), this.categories.getChildren());
    }

    private DataWidget.Entry<?> selected;

    public OptionsPane() {
        super();

        this.templateSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(Template<?> object) {
                return object == null ? "" : object.getName();
            }

            @Override
            public Template<?> fromString(String string) {
                return Proximity.getTemplate(string);
            }
        });

        this.templateSelector.disableProperty().bind(Bindings.createBooleanBinding(() -> this.templateSelector.getItems().isEmpty(), this.templateSelector.getItems()));

        this.templateSelector.setPromptText("Select a card");

        this.templateSelector.getStyleClass().add("sidebar-dropdown");

        HBox hBox = new HBox(this.templateSelector);

        this.templateSelector.prefWidthProperty().bind(hBox.widthProperty());

        hBox.getStyleClass().add("sidebar-entry");

        VBox.setVgrow(this.options, Priority.ALWAYS);

        this.getChildren().add(0, hBox);
        this.getChildren().addAll(this.options, this.categories);

        this.templateSelector.valueProperty().addListener(this.templateChangeListener);
    }

    private <D extends RenderJob> void setTemplate(Template<D> template) {
        //noinspection unchecked
        ((DataWidget.Entry<D>) this.selected).template().setValue(template);
    }

    public <D extends RenderJob> void select(DataWidget.Entry<D> entry) {
        this.selected = entry;

        // Remove any existing listeners to prevent duplicates
        entry.document().removeListener(this.documentChangeListener);
        entry.document().addListener(this.documentChangeListener);

        this.templateSelector.getItems().clear();

        for (Template<?> template : Proximity.templates()) {
            if (template.canHandle(entry.getValue())) {
                this.templateSelector.getItems().add(template);
            }
        }

        if (this.templateSelector.getItems().size() > 0 && entry.template() == null) {
            this.templateSelector.setValue(this.templateSelector.getItems().get(0));
        } else if (entry.template().getValue() != null) {
            Template<D> template = entry.template().getValue();
            DocumentImpl<D> document = entry.document().getValue();

            this.templateSelector.valueProperty().removeListener(this.templateChangeListener);
            this.templateSelector.setValue(template);
            this.templateSelector.valueProperty().addListener(this.templateChangeListener);

            OptionsImpl<D> options = new OptionsImpl<>();

            document.getTemplate().createOptions(options);

            for (Option<?, ?, D> option : document.getSelectorOverrides()) {
                options.add(option, "Advanced");
            }

            options.createWidgets(entry, this.options.getChildren(), this.categories.getChildren());
        }
    }

    public DataWidget.Entry<?> getSelected() {
        return this.selected;
    }
}
