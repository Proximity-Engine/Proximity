package dev.hephaestus.proximity.xml.layers;

import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.layers.Layer;
import dev.hephaestus.proximity.templates.layers.SquishBoxLayer;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.XMLUtil;
import dev.hephaestus.proximity.xml.Properties;
import org.w3c.dom.Element;

public class SquishBoxElement extends LayerElement<SquishBoxLayer> {
    private LayerElement<?> main;
    private LayerElement<?> flex;

    public SquishBoxElement(Element element) {
        super(element);
    }

    @Override
    protected Result<LayerElement<SquishBoxLayer>> parseLayer(Context context, Properties properties) {
        Result<LayerElement<?>> main = XMLUtil.applyToFirstElement(this.element, "main", e -> getFactory(e.getTagName()).create(e).parse(context, properties)).unwrap();
        Result<LayerElement<?>> flex = XMLUtil.applyToFirstElement(this.element, "flex", e -> getFactory(e.getTagName()).create(e).parse(context, properties)).unwrap();

        if (main.isError() ^ flex.isError()) {
            return Result.error((main.isError() ? main : flex).getError());
        } else if (main.isError()) {
            return Result.error("Error creating factories for layer %s:\n\tmain: %s\n\tflex: %s",
                    this.getId(), main.getError(), flex.getError()
            );
        }

        this.main = main.get();
        this.flex = flex.get();

        return Result.of(this);
    }

    @Override
    public Result<LayerElement<SquishBoxLayer>> createFactory(Template template) {
        Result<? extends LayerElement<?>> main = this.main.createFactory(template);
        Result<? extends LayerElement<?>> flex = this.flex.createFactory(template);

        if (main.isError() ^ flex.isError()) {
            return Result.error((main.isError() ? main : flex).getError());
        } else if (main.isError()) {
            return Result.error("Error creating factories for layer %s:\n\tmain: %s\n\tflex: %s",
                    this.getId(), main.getError(), flex.getError()
            );
        }

        this.main = main.get();
        this.flex = flex.get();

        return Result.of(this);
    }

    @Override
    public Result<SquishBoxLayer> createLayer(String parentId, JsonObject card) {
        Result<? extends Layer> mainResult = this.main.create(this.getId(), card);
        Result<? extends Layer> flexResult = this.flex.create(this.getId(), card);

        if (mainResult.isError() ^ flexResult.isError()) {
            return Result.error((mainResult.isError() ? mainResult : flexResult).getError());
        } else if (mainResult.isError()) {
            return Result.error("Error creating factories for layer %s:\n\tmain: %s\n\tflex: %s",
                    Layer.id(parentId, this.getId()), mainResult.getError(), flexResult.getError()
            );
        }

        return Result.of(new SquishBoxLayer(parentId, this.getId(), this.getX(), this.getY(),
                mainResult.get(),
                flexResult.get()
        ));
    }
}
