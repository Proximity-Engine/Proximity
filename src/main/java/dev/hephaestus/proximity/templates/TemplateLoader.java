package dev.hephaestus.proximity.templates;

import dev.hephaestus.proximity.util.Result;

public interface TemplateLoader {
    Result<TemplateSource> getTemplateFiles(String name);
}
