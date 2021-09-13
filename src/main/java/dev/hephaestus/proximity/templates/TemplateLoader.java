package dev.hephaestus.proximity.templates;

import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.util.Result;
import org.quiltmc.json5.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public interface TemplateLoader {
    Result<TemplateSource> getTemplateFiles(String name);

    default Result<Template> load(TemplateSource files, JsonObject options) {
        String name = files.getTemplateName();

        try {
            InputStream inputStream = files.getInputStream("template.json5");
            InputStreamReader streamReader = new InputStreamReader(inputStream);
            JsonReader jsonReader = JsonReader.json5(streamReader);
            JsonObject object = JsonObject.parseObject(jsonReader);
            Template.Parser parser = new Template.Parser(name, object, files, options);

            return Result.of(parser.parse());
        } catch (IOException e) {
            return Result.error("Exception loading template '%s': %s", name, e.getMessage());
        }
    }
}
