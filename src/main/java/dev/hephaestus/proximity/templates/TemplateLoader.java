package dev.hephaestus.proximity.templates;

import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.util.Result;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public interface TemplateLoader {
    Result<TemplateSource> getTemplateFiles(String name);

    default Result<Template> load(Logger log, TemplateSource source, JsonObject options) {
        String name = source.getTemplateName();

        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = documentBuilder.parse(source.getInputStream("template.xml"));
            TemplateParser parser = new TemplateParser(log);

            return parser.parse(document, source, options);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            return Result.error("Exception loading template '%s': %s", name, e.getMessage());
        }
    }
}
