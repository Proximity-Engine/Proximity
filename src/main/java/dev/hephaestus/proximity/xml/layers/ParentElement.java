package dev.hephaestus.proximity.xml.layers;

import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.layers.Layer;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.XMLUtil;
import dev.hephaestus.proximity.xml.Properties;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class ParentElement<L extends Layer> extends LayerElement<L> {
    private List<LayerElement<?>> children;

    public ParentElement(Element element) {
        super(element);
    }

    protected final Iterable<LayerElement<?>> getChildren() {
        Objects.requireNonNull(this.children);

        return this.children;
    }

    protected abstract Result<LayerElement<L>> createLayerFactory(Template template);

    @Override
    protected final Result<LayerElement<L>> parseLayer(Context context, Properties properties) {
        this.children = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        XMLUtil.iterate(this.getElement(), (e, i) -> {
            Factory<LayerElement<?>> factory = getFactory(e.getTagName());

            if (factory != null) {
                factory.create(e).parse(context, properties)
                        .ifPresent(this.children::add)
                        .ifError(errors::add);
            }
        });

        if (errors.isEmpty()) {
            return Result.of(this);
        } else {
            return Result.error("Error(s) while parsing layer %s: \n\t%s", this.getId(), String.join("\n\t%s", errors));
        }
    }

    @Override
    public final Result<LayerElement<L>> createFactoryImmediately(Template template) {
        ArrayList<LayerElement<?>> children = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (LayerElement<?> element : this.children) {
            element.createFactory(template)
                    .ifPresent(children::add)
                    .ifError(errors::add);
        }

        if (!errors.isEmpty()) {
            return Result.error("Error creating child factories for layer %s:\n\t%s", this.getId(), String.join("\n\t"));
        }

        this.children = children;

        return this.createLayerFactory(template);
    }
}
