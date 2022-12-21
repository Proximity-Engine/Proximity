package dev.hephaestus.proximity.app.api.plugins;

import dev.hephaestus.proximity.app.api.Proximity;
import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.Template;
import dev.hephaestus.proximity.app.api.rendering.Document;
import dev.hephaestus.proximity.app.impl.rendering.DocumentImpl;
import dev.hephaestus.proximity.json.api.JsonElement;
import dev.hephaestus.proximity.json.api.JsonObject;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public abstract class DataWidget<D extends RenderJob<?>> implements Iterable<DataWidget<D>.Entry> {
    protected final ObservableList<String> errors = FXCollections.observableArrayList();
    protected final DataProvider.Context context;
    protected final SimpleListProperty<Entry> entries = new SimpleListProperty<>(FXCollections.observableArrayList());

    public DataWidget(DataProvider.Context context) {
        this.context = context;
    }

    public final ObservableList<String> getErrorProperty() {
        return this.errors;
    }

    public final boolean isEmpty() {
        return this.entries.size() == 0;
    }

    public final SimpleListProperty<Entry> getEntries() {
        return this.entries;
    }

    public abstract Pane getRootPane();

    /**
     * Save this widgets state to json
     */
    public abstract JsonElement saveState();

    @NotNull
    @Override
    public Iterator<Entry> iterator() {
        return this.entries.iterator();
    }

    public final int size() {
        return this.entries.size();
    }

    public final Entry get(int i) {
        return this.entries.get(i);
    }

    public abstract class Entry extends SimpleObjectProperty<D> {
        private final ObjectProperty<Template<D>> template = new SimpleObjectProperty<>();
        private final ObjectProperty<Document<D>> document = new SimpleObjectProperty<>();

        protected final Pane pane;

        private Entry(Pane pane, D data) {
            this.pane = pane;
            this.document.bind(Bindings.createObjectBinding(() -> this.template.get() == null ? null : new DocumentImpl<>(this.get(), this.template.getValue(), this.getWidget().errors), this, this.template));
            this.set(data);
        }

        public DataWidget<D> getWidget() {
            return DataWidget.this;
        }

        public Pane getRootPane() {
            return this.pane;
        }

        @ApiStatus.Internal
        public final JsonObject toJson() {
            var object = JsonObject.create();

            if (this.template.getValue() != null) {
                object.put("template", this.template.getValue().getName());
            }

            if (this.getValue() != null) {
                object.put("job", this.getValue().toJsonImpl());
            }

            return object;
        }

        @ApiStatus.Internal
        public final Property<Template<D>> template() {
            return this.template;
        }

        @ApiStatus.Internal
        public final Property<Document<D>> document() {
            return this.document;
        }
    }

    public class SingleEntry extends DataWidget<D>.Entry {
        public SingleEntry(Pane pane, D data) {
            super(pane, data);
        }

        @Override
        public DataWidget<D> getWidget() {
            return DataWidget.this;
        }

        private void select(MouseEvent event) {
            Proximity.select(this);
        }
    }

    public class MultiEntry extends DataWidget<D>.Entry {
        public MultiEntry(D data) {
            super(new StackPane(), data);

            Label label = new Label(data.getName());

            label.setStyle("-fx-text-fill: #FFFFFFD0; -fx-prompt-text-fill: #FFFFFFA0;");

            StackPane stack = (StackPane) this.getRootPane();

            stack.getChildren().add(label);
            stack.setPadding(new Insets(5, 5, 5, 15));
            stack.setOnMouseClicked(observable -> Proximity.select(this));

            label.setOnMouseClicked(observable -> Proximity.select(this));

            StackPane.setAlignment(label, Pos.CENTER_LEFT);
        }

        @Override
        public DataWidget<D> getWidget() {
            return DataWidget.this;
        }
    }
}
