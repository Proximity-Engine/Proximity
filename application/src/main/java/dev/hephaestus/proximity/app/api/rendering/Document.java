package dev.hephaestus.proximity.app.api.rendering;

import dev.hephaestus.proximity.app.api.rendering.elements.Element;
import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.Template;

import java.net.URL;

public interface Document<D extends RenderJob> extends Iterable<Element<D>> {
    URL getResourceLocation(String src, String... alternateFileExtensions);

    Template<D> getTemplate();
}
