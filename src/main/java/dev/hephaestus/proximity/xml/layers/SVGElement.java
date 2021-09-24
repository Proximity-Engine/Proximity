package dev.hephaestus.proximity.xml.layers;

import com.kitfox.svg.RenderableElement;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.templates.TemplateSource;
import dev.hephaestus.proximity.templates.layers.SVGLayer;
import dev.hephaestus.proximity.util.ContentAlignment;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.SVG;
import dev.hephaestus.proximity.xml.Properties;
import org.w3c.dom.Element;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SVGElement extends LayerElement<SVGLayer> {
    private String src;
    private Integer width, height;
    private ContentAlignment verticalAlignment, horizontalAlignment;
    private TemplateSource source;

    public SVGElement(Element element) {
        super(element);
    }

    @Override
    protected Result<LayerElement<SVGLayer>> parseLayer(Context context, Properties properties) {
        this.src = this.hasAttribute("src") ? this.getAttribute("src") : this.getId();
        this.verticalAlignment = this.hasAttribute("vertical_alignment") ? ContentAlignment.valueOf(this.getAttribute("vertical_alignment").toUpperCase(Locale.ROOT)) : ContentAlignment.MIDDLE;
        this.horizontalAlignment = this.hasAttribute("horizontal_alignment") ? ContentAlignment.valueOf(this.getAttribute("horizontal_alignment").toUpperCase(Locale.ROOT)) : ContentAlignment.MIDDLE;
        this.width = this.hasAttribute("width") ? Integer.decode(this.getAttribute("width")) : null;
        this.height = this.hasAttribute("height") ? Integer.decode(this.getAttribute("height")) : null;

        return Result.of(this);
    }

    @Override
    public Result<LayerElement<SVGLayer>> createFactoryImmediately(Template template) {
        this.source = template.getSource();

        return Result.of(this);
    }

    @Override
    protected Result<SVGLayer> createLayer(String parentId, JsonObject card) {
        String src = getFileLocation(parentId, this.getId(), this.src, card) + ".svg";

        try {
            SVGUniverse universe = new SVGUniverse();
            URI uri = universe.loadSVG(this.source.getInputStream(src), src);
            SVGDiagram diagram = universe.getDiagram(uri);

            diagram.setIgnoringClipHeuristic(true);
            SVG svg = deconstruct(diagram);

            int width = (int) svg.drawnBounds().getWidth(), height = (int) svg.drawnBounds().getHeight();

            if (this.width == null && this.height != null) {
                double ratio = svg.drawnBounds().getWidth() / svg.drawnBounds().getHeight();
                height = this.height;
                width = (int) Math.round(ratio * height);
            } else if (this.height == null && this.width != null) {
                double ratio = svg.drawnBounds().getWidth() / svg.drawnBounds().getHeight();
                width = this.width;
                height = (int) Math.round(width / ratio);
            } else if (this.width != null) {
                width = this.width;
                height = this.height;
            }

            double wr = width / svg.drawnBounds().getWidth();
            double hr = height / svg.drawnBounds().getHeight();

            return Result.of(new SVGLayer(
                    src,
                    parentId,
                    this.getId(),
                    this.getX(),
                    this.getY(),
                    svg,
                    (float) Math.min(wr, hr),
                    this.verticalAlignment,
                    this.horizontalAlignment
            ));
        } catch (IOException e) {
            return Result.error(e.getMessage());
        }
    }

    private static SVG deconstruct(SVGDiagram diagram) {
        double minX, minY = minX = Float.MAX_VALUE;
        double maxX, maxY = maxX = -Float.MAX_VALUE;

        var root = diagram.getRoot();

        List<RenderableElement> renderableElements = new ArrayList<>();

        for (int i = 0; i < root.getNumChildren(); ++i) {
            if (root.getChild(i) instanceof RenderableElement renderable) {
                try {
                    renderableElements.add(renderable);
                    Rectangle bounds = renderable.getBoundingBox().getBounds();
                    minX = Math.min(bounds.getMinX(), minX);
                    minY = Math.min(bounds.getMinY(), minY);
                    maxX = Math.max(bounds.getMaxX(), maxX);
                    maxY = Math.max(bounds.getMaxY(), maxY);
                } catch (SVGException e) {
                    e.printStackTrace();
                }
            }
        }

        return new SVG(diagram.getViewRect().getBounds(),
                new Rectangle(
                        (int) minX, (int) minY, (int) (maxX - minX) + 1, (int) (maxY - minY) + 1
                ),
                renderableElements);
    }
}
