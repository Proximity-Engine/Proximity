package dev.hephaestus.proximity.xml.layers;

import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.layers.ImageLayer;
import dev.hephaestus.proximity.templates.layers.Layer;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.xml.Properties;
import org.w3c.dom.Element;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;

public class ArtElement extends LayerElement<ImageLayer> {
    private Integer width, height;

    public ArtElement(Element element) {
        super(element);
    }

    @Override
    protected Result<LayerElement<ImageLayer>> parseLayer(Context context, Properties properties) {
        if (!this.hasAttribute("width") && !this.hasAttribute("height")) {
            return Result.error("Image layer must have either 'width' or 'height' attribute");
        }

        this.width = this.hasAttribute("width") ? Integer.decode(this.getAttribute("width")) : null;
        this.height = this.hasAttribute("height") ? Integer.decode(this.getAttribute("height")) : null;

        return Result.of(this);
    }

    @Override
    public Result<LayerElement<ImageLayer>> createFactoryImmediately(Template template) {
        return Result.of(this);
    }

    @Override
    public Result<ImageLayer> createLayer(String parentId, JsonObject card) {
        try {
            String fileLocation = card.getAsString("image_uris", "art_crop");

            return Result.of(new ImageLayer(
                    parentId,
                    this.getId(),
                    this.getX(),
                    this.getY(),
                    ImageIO.read(new URL(fileLocation)),
                    this.width,
                    this.height,
                    fileLocation));
        } catch (IOException e) {
            return Result.error("Failed to create layer '%s': %s", Layer.id(parentId, this.getId()), e.getMessage());
        }
    }
}
