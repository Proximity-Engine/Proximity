package dev.hephaestus.proximity.templates.layers.factories;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.LayerFactory;
import dev.hephaestus.proximity.templates.layers.ImageLayer;
import dev.hephaestus.proximity.templates.layers.Layer;
import dev.hephaestus.proximity.util.Result;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class ArtFactory extends LayerFactory<ImageLayer> {
    private final Integer width, height;

    public ArtFactory(String id, int x, int y, List<CardPredicate> predicates, Integer width, Integer height) {
        super(id, x, y, predicates);
        this.width = width;
        this.height = height;
    }

    @Override
    public Result<ImageLayer> createLayer(String parentId, JsonObject card) {
        try {
            String fileLocation = card.getAsString("image_uris", "art_crop");

            return Result.of(new ImageLayer(
                    parentId,
                    this.id,
                    this.x,
                    this.y,
                    ImageIO.read(new URL(fileLocation)),
                    this.width,
                    this.height,
                    fileLocation));
        } catch (IOException e) {
            return Result.error("Failed to create layer '%s': %s", Layer.id(parentId, this.id), e.getMessage());
        }
    }
}
