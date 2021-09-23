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
        if (!this.element.hasAttribute("src") && !this.element.hasAttribute("id")) {
            return Result.error("Image layer must have either 'src' or 'id' attribute");
        }

        this.src = this.element.hasAttribute("src") ? this.element.getAttribute("src") : this.getId();

        return Result.of(this);
    }

    @Override
    public Result<LayerElement<ImageLayer>> createFactory(Template template) {
        this.source = template.getSource();

        return Result.of(this);
    }

    @Override
    public Result<ImageLayer> createLayer(String parentId, JsonObject card) {
        String sep = FileSystems.getDefault().getSeparator();
        StringBuilder src = new StringBuilder(parentId.replace(".", sep)
                + FileSystems.getDefault().getSeparator());

        if (this.src == null) {
            src.append(this.getId());
        } else {
            String[] split = LayerElement.substitute(this.src, card).split("/");

            for (int i = 0; i < split.length; i++) {
                String string = split[i];

                if (string.equals("..")) {
                    if (src.chars().filter(j -> Character.toString(j).equals(sep)).count() > 1) {
                        src = new StringBuilder(src.substring(0, src.length() - 1));
                        src = new StringBuilder(src.substring(0, src.lastIndexOf(sep)));
                    } else {
                        src = new StringBuilder();
                    }
                } else {
                    src.append(string);
                }

                if (i < split.length - 1 && !src.isEmpty()) {
                    src.append(sep);
                }
            }
        }

        src.append(".png");

        BufferedImage image = this.source.getImage(src.toString());

        return Result.of(new ImageLayer(
                parentId,
                this.getId(),
                this.getX(),
                this.getY(),
                image,
                image.getWidth(),
                image.getHeight(),
                src.toString()));
    }
}
