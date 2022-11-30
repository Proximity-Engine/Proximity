package dev.hephaestus.proximity.app.impl.sidebar;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.Template;
import dev.hephaestus.proximity.app.api.plugins.DataWidget;
import dev.hephaestus.proximity.app.impl.OptionsImpl;
import dev.hephaestus.proximity.app.impl.Proximity;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

public class OptionsPane extends SidebarPane {
    private final ComboBox<Template<?>> templateSelector = new ComboBox<>();
    private final ChangeListener<Template<?>> templateChangeListener = (observable, oldValue, newValue) -> {
        this.container.getChildren().clear();

        if (newValue != null) {
            this.doThing(newValue);

        }
    };

    private DataWidget.Entry<?> selected;

    private <D extends RenderJob> void doThing(Template<D> newValue) {
        //noinspection unchecked
        DataWidget.Entry<D> entry = (DataWidget.Entry<D>) this.selected;

        this.setTemplate(newValue);

        Proximity.rerender(entry);

        OptionsImpl<D> options = new OptionsImpl<>();

        newValue.createOptions(options);

        options.createWidgets(entry, this.container.getChildren());
    }

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

        this.getChildren().add(0, hBox);

        this.templateSelector.valueProperty().addListener(this.templateChangeListener);
    }

    private <D extends RenderJob> void setTemplate(Template<D> template) {
        //noinspection unchecked
        ((DataWidget.Entry<D>) this.selected).template().setValue(template);
    }

    public <D extends RenderJob> void select(DataWidget.Entry<D> widget) {
        this.selected = widget;

        this.templateSelector.getItems().clear();

        for (Template<?> template : Proximity.templates()) {
            if (template.canHandle(widget.getValue())) {
                this.templateSelector.getItems().add(template);
            }
        }

        if (this.templateSelector.getItems().size() > 0 && widget.template() == null) {
            this.templateSelector.setValue(this.templateSelector.getItems().get(0));
        } else if (widget.template().getValue() != null) {
            Template<D> template = widget.template().getValue();

            this.templateSelector.valueProperty().removeListener(this.templateChangeListener);
            this.templateSelector.setValue(template);
            this.templateSelector.valueProperty().addListener(this.templateChangeListener);

            template.createOptions(/* TODO */ null);
        }
    }

    public DataWidget.Entry<?> getSelected() {
        return this.selected;
    }
}
