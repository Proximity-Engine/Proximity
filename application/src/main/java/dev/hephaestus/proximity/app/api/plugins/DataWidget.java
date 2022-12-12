package dev.hephaestus.proximity.app.api.plugins;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.Template;
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
import javafx.scene.layout.Pane;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public abstract class DataWidget<D extends RenderJob<?>> implements Iterable<DataWidget.Entry<D>> {
    protected final ObservableList<String> errors = FXCollections.observableArrayList();
    protected final DataProvider.Context context;
    protected final SimpleListProperty<Entry<D>> entries = new SimpleListProperty<>(FXCollections.observableArrayList());

    public DataWidget(DataProvider.Context context) {
        this.context = context;
    }

    public final ObservableList<String> getErrorProperty() {
        return this.errors;
    }

    public final boolean isEmpty() {
        return this.entries.size() == 0;
    }

    public final SimpleListProperty<Entry<D>> getEntries() {
        return this.entries;
    }

    public abstract Pane getRootPane();

    /**
     * Save this widgets state to json. Only called if this widget doesn't contain any entries.
     */
    public abstract JsonElement toJson();

    @NotNull
    @Override
    public Iterator<Entry<D>> iterator() {
        return this.entries.iterator();
    }

    public final int size() {
        return this.entries.size();
    }

    public final Entry<D> get(int i) {
        return this.entries.get(i);
    }

    public static abstract class Entry<D extends RenderJob<?>> extends SimpleObjectProperty<D> {
        private final ObjectProperty<Template<D>> template = new SimpleObjectProperty<>();
        private final ObjectProperty<DocumentImpl<D>> document = new SimpleObjectProperty<>();

        public Entry() {
            this.document.bind(Bindings.createObjectBinding(() -> this.template.get() == null ? null : new DocumentImpl<>(this.get(), this.template.getValue(), this.getWidget().errors), this, this.template));
        }

        public abstract Pane getRootPane();
        public abstract DataWidget<D> getWidget();

        public abstract JsonElement toJson();

        @ApiStatus.Internal
        public final JsonObject toJsonImpl() {
            var object = JsonObject.create();

            if (this.template.getValue() != null) {
                object.put("template", this.template.getValue().getName());
            }

            if (this.getValue() != null) {
                object.put("job", this.getValue().toJsonImpl());
            }

            JsonElement json = this.toJson();

            if (json != null) {
                object.put("entry", json);
            }

            return object;
        }

        @ApiStatus.Internal
        public final Property<Template<D>> template() {
            return this.template;
        }

        @ApiStatus.Internal
        public final Property<DocumentImpl<D>> document() {
            return this.document;
        }
    }
}
