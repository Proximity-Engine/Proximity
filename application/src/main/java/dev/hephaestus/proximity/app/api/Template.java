package dev.hephaestus.proximity.app.api;

import dev.hephaestus.proximity.app.api.exceptions.ResourceNotFoundException;
import dev.hephaestus.proximity.app.impl.rendering.DefaultResourceProvider;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class Template<D extends RenderJob<?>> {
    private final Map<String, Font> fonts = new HashMap<>();
    private final List<ResourceProvider> resourceProviders = new ArrayList<>(1);

    protected final String name;
    protected final int width, height, dpi;

    protected Template(String name, int width, int height, int dpi, boolean addDefaultResourceProvider) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.dpi = dpi;

        if (addDefaultResourceProvider) {
            this.addResourceProvider(new DefaultResourceProvider(this.getClass().getModule()));
        }
    }

    public final String getName() {
        return this.name;
    }

    public final int getWidth() {
        return this.width;
    }

    public final int getHeight() {
        return this.height;
    }

    public final int getDPI() {
        return this.dpi;
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

        throw new ResourceNotFoundException(name);
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

    public abstract boolean canHandle(Object data);

    public abstract void createLayers(Parent<D> layers);

    public void createOptions(Options<D> options) {

    }

    public interface Options<D extends RenderJob<?>> {
        void add(Option<?, ?, ? super D> option);
        void add(Option<?, ?, ? super D> option, String category);
        void category(String id, Consumer<Options<D>> category);
        void category(String id, boolean expandedByDefault, Consumer<Options<D>> category);
    }
}
