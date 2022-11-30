package dev.hephaestus.proximity.app.api.plugins;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.Template;

import java.util.function.Consumer;

public interface TemplateProvider<D extends RenderJob> {
    Class<D> getDataClass();
    void createTemplates(Consumer<Template<D>> templates);
}
