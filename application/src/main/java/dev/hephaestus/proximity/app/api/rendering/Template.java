package dev.hephaestus.proximity.app.api.rendering;

import dev.hephaestus.proximity.app.api.Option;
import dev.hephaestus.proximity.app.api.ResourceProvider;
import dev.hephaestus.proximity.app.api.exceptions.ResourceNotFoundException;
import dev.hephaestus.proximity.app.api.rendering.elements.Group;
import dev.hephaestus.proximity.app.impl.rendering.DefaultResourceProvider;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class Template<D extends RenderData> {
    private final Map<String, Font> fonts = new HashMap<>();
    private final Map<String, javafx.scene.text.Font> fxFonts = new HashMap<>();
    private final List<ResourceProvider> resourceProviders = new ArrayList<>(1);

    public Template() {
        this.addResourceProviders();
    }

    public abstract int getDPI();
    public abstract int getWidth();
    public abstract int getHeight();
    public abstract void build(D data, Group image);
    public abstract void createOptions(Options<D> options);
    public abstract boolean canHandle(RenderData data);

    protected void addResourceProviders() {
        this.addResourceProvider(new DefaultResourceProvider(this.getClass().getModule()));
    }

    public final void addResourceProvider(ResourceProvider resourceProvider) {
        this.resourceProviders.add(resourceProvider);
    }

    public final boolean hasResource(String name) {
        for (ResourceProvider resourceProvider : this.resourceProviders) {
            if (resourceProvider.hasResource(name)) {
                return true;
            }
        }

        return false;
    }

    public final InputStream getResource(String name) {
        for (ResourceProvider resourceProvider : this.resourceProviders) {
            if (resourceProvider.hasResource(name)) {
                return resourceProvider.getResource(name);
            }
        }

        return null;
    }

    public final Font getFont(String fontName, float size) {
        Font font = this.fonts.get(fontName);

        if (font == null) {
            InputStream stream;

            if (this.hasResource("fonts/" + fontName + ".otf")) {
                stream = this.getResource("fonts/" + fontName + ".otf");
            } else {
                stream = this.getResource("fonts/" + fontName + ".ttf");
            }

            if (stream != null) {
                try {
                    font = Font.createFont(Font.TRUETYPE_FONT, stream);
                    this.fonts.put(fontName, font);
                } catch (FontFormatException | IOException e) {
                    // TODO: Log this somehow
                }
            }
        }

        return font == null ? null : font.deriveFont(size);
    }

    public final javafx.scene.text.Font getFXFont(String fontName, float size) {
        javafx.scene.text.Font font = this.fxFonts.get(fontName);

        if (font == null) {
            InputStream stream;

            if (this.hasResource("fonts/" + fontName + ".otf")) {
                stream = this.getResource("fonts/" + fontName + ".otf");
            } else {
                stream = this.getResource("fonts/" + fontName + ".ttf");
            }

            if (stream != null) {
                font = javafx.scene.text.Font.loadFont(stream, size);
                this.fxFonts.put(fontName, font);
                return font;
            } else {
                throw new RuntimeException("Font not found \"" + fontName + "\"");
            }
        } else {
            return new javafx.scene.text.Font(font.getName(), size);
        }
    }


    protected static Observable all(Observable... observables) {
        return new Observable() {
            @Override
            public void addListener(InvalidationListener listener) {
                for (Observable observable : observables) {
                    if (observable != null) {
                        observable.addListener(listener);
                    }
                }
            }

            @Override
            public void removeListener(InvalidationListener listener) {
                for (Observable observable : observables) {
                    if (observable != null) {
                        observable.removeListener(listener);
                    }
                }
            }
        };
    }

    protected static ObservableBooleanValue and(ObservableBooleanValue... values) {
        SimpleBooleanProperty property = new SimpleBooleanProperty();

        property.bind(Bindings.createBooleanBinding(() -> {
            boolean bl = true;

            for (ObservableBooleanValue value : values) {
                bl &= value.get();
            }

            return bl;
        }, values));

        return property;
    }

    protected static ObservableBooleanValue not(ObservableBooleanValue value) {
        SimpleBooleanProperty property = new SimpleBooleanProperty();

        property.bind(Bindings.createBooleanBinding(() -> !value.get(), value));

        return property;
    }

    protected static ReadOnlyBooleanProperty[] level(ReadOnlyBooleanProperty... properties) {
        return properties;
    }

    protected static ReadOnlyBooleanProperty[] level(String defaultBranch, ReadOnlyBooleanProperty... properties) {
        ReadOnlyBooleanProperty[] result = new ReadOnlyBooleanProperty[properties.length + 1];

        result[0] = new SimpleBooleanProperty(null, defaultBranch, true);

        System.arraycopy(properties, 0, result, 1, properties.length);

        return result;
    }

    public interface Options<D> {
        void add(Option<?, ?, ? super D> option);
        void add(Option<?, ?, ? super D> option, String category);
        void category(String id, Consumer<Options<D>> category);
        void category(String id, boolean expandedByDefault, Consumer<Options<D>> category);
    }
}
