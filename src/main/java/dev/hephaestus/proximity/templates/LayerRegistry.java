package dev.hephaestus.proximity.templates;

import dev.hephaestus.proximity.templates.layers.renderers.*;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableData;

import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class LayerRegistry {
    private final Map<String /* XML tag name */, LayerRenderer.Factory<?>> factories;

    public LayerRegistry() {
        this(Collections.emptyMap());
    }

    private LayerRegistry(Map<String, LayerRenderer.Factory<?>> factories) {
        this.factories = factories;
    }

    public Map<String, LayerRenderer> create(RenderableData data) {
        Map<String, LayerRenderer> factories = new HashMap<>();

        for (var entry : this.factories.entrySet()) {
            factories.put(entry.getKey(), entry.getValue().create(data));
        }

        return factories;
    }

    public static LayerRegistry createDefault() {
        return new Builder()
                .factory(RectangleLayerRenderer::new, "Rectangle", "Spacer")
                .factory(ForkLayerRenderer::new, "Fork")
                .factory(LayerGroupRenderer::new, "Group", "Main")
                .factory(ImageLayerRenderer::new, "Image")
                .factory(LayerSelectorRenderer::new, "Selector", "Flex")
                .factory(SquishBoxRenderer::new, "SquishBox")
                .factory(TextLayerRenderer::new, "Text")
                .factory(SVGLayerRenderer::new, "SVG")
                .factory(NoiseLayerRenderer::new, "Noise")
                .factory(EffectLayerRenderer::new, "Effect")
                .factory(data -> new LayoutElementRenderer(data, "x", "y",
                        Rectangle2D::getWidth,
                        Rectangle2D::getHeight
                ), "HorizontalLayout")
                .factory(data -> new LayoutElementRenderer(data, "y", "x",
                        Rectangle2D::getHeight,
                        Rectangle2D::getWidth
                ), "VerticalLayout")
                .build();
    }

    public static final class Builder {
        private final Map<String, LayerRenderer.Factory<?>> factories = new HashMap<>();

        public Builder factory(LayerRenderer.Factory<?> factory, String tagName0, String... tagNames) {
            this.factories.put(tagName0, factory);

            for (String tagName : tagNames) {
                this.factories.put(tagName, factory);
            }

            return this;
        }

        public LayerRegistry build() {
            return new LayerRegistry(this.factories);
        }
    }
}
