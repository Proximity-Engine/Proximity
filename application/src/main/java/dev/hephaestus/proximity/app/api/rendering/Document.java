package dev.hephaestus.proximity.app.api.rendering;

import dev.hephaestus.proximity.app.api.rendering.elements.Element;
import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.Template;

import java.io.InputStream;

public interface Document<D extends RenderJob<?>> extends Iterable<Element<D>> {
    InputStream getResource(String src, String... alternateFileExtensions);

    Template<D> getTemplate();
}
