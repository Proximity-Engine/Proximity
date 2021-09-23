package dev.hephaestus.proximity.templates.layers.factories;

import dev.hephaestus.proximity.cards.predicates.CardPredicate;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.LayerFactory;
import dev.hephaestus.proximity.templates.TemplateSource;
import dev.hephaestus.proximity.templates.layers.ImageLayer;
import dev.hephaestus.proximity.util.Result;

import java.awt.image.BufferedImage;
import java.nio.file.FileSystems;
import java.util.List;

public class ImageFactory extends LayerFactory<ImageLayer> {
    private final TemplateSource source;
    private final String src;

    public ImageFactory(String id, int x, int y, List<CardPredicate> predicates, TemplateSource source, String src) {
        super(id, x, y, predicates);
        this.source = source;
        this.src = src;
    }

    @Override
    public Result<ImageLayer> createLayer(String parentId, JsonObject card) {
        String sep = FileSystems.getDefault().getSeparator();
        StringBuilder src = new StringBuilder(parentId.replace(".", sep)
                + FileSystems.getDefault().getSeparator());

        if (this.src == null) {
            src.append(this.id);
        } else {
            String[] split = substitute(this.src, card).split("/");

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
                this.id,
                this.x,
                this.y,
                image,
                image.getWidth(),
                image.getHeight(),
                src.toString()));
    }
}
