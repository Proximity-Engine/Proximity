package dev.hephaestus.proximity.app.impl.rendering;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.Template;
import dev.hephaestus.proximity.app.api.rendering.Document;
import dev.hephaestus.proximity.app.api.rendering.elements.Element;
import dev.hephaestus.proximity.app.impl.rendering.elements.ElementImpl;
import dev.hephaestus.proximity.app.impl.rendering.elements.ParentImpl;
import dev.hephaestus.proximity.utils.UnmodifiableIterator;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DocumentImpl<D extends RenderJob> implements ParentImpl<D>, Document<D> {
    private final D data;
    private final Template<D> template;
    private final List<ElementImpl<D>> children = new ArrayList<>(5);

    public DocumentImpl(D data, Template<D> template) {
        this.data = data;
        this.template = template;
        this.template.createLayers(this);
    }

    @Override
    public void addChild(ElementImpl<D> child) {
        this.children.add(child);
    }

    @Override
    public DocumentImpl<D> getDocument() {
        return this;
    }

    @NotNull
    @Override
    public Iterator<Element<D>> iterator() {
        return new UnmodifiableIterator<>(this.children);
    }

    public D getData() {
        return this.data;
    }

    public Template<D> getTemplate() {
        return this.template;
    }

    @Override
    public URL getResourceLocation(String src, String... alternateFileExtensions) {
        String path = src;

        // Check for files when the file extension is not specified
        for (int i = 0; !this.getTemplate().hasResource(path) && i < alternateFileExtensions.length; i++) {
            path = src + "." + alternateFileExtensions[i];
        }

        if (this.getTemplate().hasResource(path)) {
            return this.getTemplate().getResource(path);
        } else {
            return this.getTemplate().getResource(src);
        }
    }

    @Override
    public boolean isVisible() {
        return true;
    }
}
