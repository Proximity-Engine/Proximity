package dev.hephaestus.proximity.xml.layers;

import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.layers.FillLayer;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.xml.Properties;
import org.w3c.dom.Element;

public class FillElement extends LayerElement<FillLayer> {
    private int width, height, color;

    public FillElement(Element element) {
        super(element);
    }

    @Override
    protected Result<LayerElement<FillLayer>> parseLayer(Context context, Properties properties) {
        this.width = Integer.decode(this.getAttribute("width"));
        this.height = Integer.decode(this.getAttribute("height"));
        this.color = this.hasAttribute("color")
                ? Integer.decode(this.getAttribute("color")) : 0;

        return Result.of(this);
    }

    @Override
    public Result<LayerElement<FillLayer>> createFactoryImmediately(Template template) {
        return Result.of(this);
    }

    @Override
    public Result<FillLayer> createLayer(String parentId, JsonObject card) {
        return Result.of(new FillLayer(parentId, this.getId(), this.getX(), this.getY(), this.width, this.height, this.color));
    }
}
