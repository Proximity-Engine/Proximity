package dev.hephaestus.proximity.xml.layers;

import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.TemplateSource;
import dev.hephaestus.proximity.templates.layers.ImageLayer;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.xml.Properties;
import org.w3c.dom.Element;

import java.awt.image.BufferedImage;
import java.nio.file.FileSystems;

public class ImageElement extends LayerElement<ImageLayer> {
    private String src;
    private TemplateSource source;

    public ImageElement(Element element) {
        super(element);
    }

    @Override
    protected Result<LayerElement<ImageLayer>> parseLayer(Context context, Properties properties) {
        if (!this.hasAttribute("src") && !this.hasAttribute("id")) {
            return Result.error("Image layer must have either 'src' or 'id' attribute");
        }

        this.src = this.hasAttribute("src") ? this.getAttribute("src") : this.getId();

        return Result.of(this);
    }

    @Override
    public Result<LayerElement<ImageLayer>> createFactoryImmediately(Template template) {
        this.source = template.getSource();

        return Result.of(this);
    }

    @Override
    public Result<ImageLayer> createLayer(String parentId, JsonObject card) {
        String src = getFileLocation(parentId, this.getId(), this.src, card) + ".png";
        BufferedImage image = this.source.getImage(src.toString());

        return Result.of(new ImageLayer(
                parentId,
                this.getId(),
                this.getX(),
                this.getY(),
                image,
                image.getWidth(),
                image.getHeight(),
                src));
    }
}
