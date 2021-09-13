package dev.hephaestus.proximity.templates;

import dev.hephaestus.proximity.util.Result;

import java.io.IOException;
import java.nio.file.Path;

public record FileSystemTemplateLoader(Path root) implements TemplateLoader {
    @Override
    public Result<TemplateSource> getTemplateFiles(String name) {
        try {
            return Result.of(new FileSystemTemplateSource(this.root.resolve(name)));
        } catch (IOException e) {
            return Result.error(e.getMessage());
        }
    }
}
