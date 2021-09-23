package dev.hephaestus.proximity.templates;


import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.layers.Layer;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.StatefulGraphics;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Template {
    private final TemplateSource source;
    private final int width, height;
    private final List<LayerFactory<?>> layers = new ArrayList<>();
    private final JsonObject options;
    private final Map<String, Style> styles;
    private final Logger log;

    public Template(TemplateSource source, int width, int height, List<LayerFactoryFactory<?>> factories, JsonObject options, Map<String, Style> styles, Logger log) {
        this.source = source;
        this.width = width;
        this.height = height;
        this.options = options;
        this.styles = Map.copyOf(styles);
        this.log = log;

        for (LayerFactoryFactory<?> factory : factories) {
            @SuppressWarnings("unchecked")
            Result<LayerFactory<?>> r = (Result<LayerFactory<?>>) factory.createFactory(this);

            if (r.isError()) {
                throw new RuntimeException(String.format("Failed to create layer '%s': %s", factory.id, r.getError()));
            } else {
                this.layers.add(r.get());
            }
        }

    }

    public Style getStyle(String name, Style orDefault) {
        if (name == null) return Style.EMPTY;

        return this.styles.getOrDefault(name, orDefault);
    }

    public Style getStyle(String name) {
        return this.getStyle(name, Style.EMPTY);
    }

    public void draw(JsonObject card, BufferedImage out) {
        StatefulGraphics graphics = new StatefulGraphics(out);

        try {
            for (LayerFactory<?> factory : this.layers) {
                Result<? extends Layer> r = factory.create("", card);

                if (r.isError()) {
                    log.error("Failed to draw layer '{}': {}", factory.id, r.getError());
                } else {
                    r.get().draw(graphics, null, true, 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TemplateSource getSource() {
        return this.source;
    }

    public JsonObject getOptions() {
        return this.options;
    }

    public Logger log() {
        return this.log;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
}
