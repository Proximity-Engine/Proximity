package dev.hephaestus.proximity.app.api.plugins;

import dev.hephaestus.proximity.app.api.rendering.RenderData;
import dev.hephaestus.proximity.app.api.rendering.Template;

import java.util.function.Consumer;

public interface TemplateProvider<D extends RenderData> {
    Class<D> getDataClass();
    void createTemplates(Consumer<Template<D>> templates);
}
