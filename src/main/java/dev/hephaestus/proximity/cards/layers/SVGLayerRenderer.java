package dev.hephaestus.proximity.cards.layers;

import com.kitfox.svg.*;
import com.kitfox.svg.animation.AnimationElement;
import dev.hephaestus.proximity.templates.layers.SVGLayer;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.util.*;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableCard;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class SVGLayerRenderer extends LayerRenderer {
    @Override
    public Result<Optional<Rectangle2D>> renderLayer(RenderableCard card, RenderableCard.XMLElement element, StatefulGraphics graphics, Rectangle2D wrap, boolean draw, float scale, Rectangle2D bounds) {
        int x = (element.hasAttribute("x") ? Integer.decode(element.getAttribute("x")) : 0);
        int y = (element.hasAttribute("y") ? Integer.decode(element.getAttribute("y")) : 0);
        String src = element.hasAttribute("src") ? element.getAttribute("src") : element.getId();
        ContentAlignment verticalAlignment = element.hasAttribute("vertical_alignment") ? ContentAlignment.valueOf(element.getAttribute("vertical_alignment").toUpperCase(Locale.ROOT)) : ContentAlignment.MIDDLE;
        ContentAlignment horizontalAlignment = element.hasAttribute("horizontal_alignment") ? ContentAlignment.valueOf(element.getAttribute("horizontal_alignment").toUpperCase(Locale.ROOT)) : ContentAlignment.MIDDLE;
        Integer width = element.hasAttribute("width") ? Integer.decode(element.getAttribute("width")) : null;
        Integer height = element.hasAttribute("height") ? Integer.decode(element.getAttribute("height")) : null;
        boolean solid = element.hasAttribute("solid") && Boolean.parseBoolean(element.getAttribute("solid"));
        String fill = element.hasAttribute("fill") ? element.getAttribute("fill") : null;

        Optional<Style.Outline> outline = element.apply("outline", e -> {
            return new Style.Outline(
                    Integer.decode(e.getAttribute("color")),
                    Integer.decode(e.getAttribute("weight"))
            );
        });

        src = ParsingUtil.getFileLocation(element.getParentId(), element.getAttribute("id"), src) + ".svg";

        try {
            SVGUniverse universe = new SVGUniverse();
            URI uri = universe.loadSVG(card.getInputStream(src), src);
            SVGDiagram diagram = universe.getDiagram(uri);

            diagram.setIgnoringClipHeuristic(true);
            SVG svg = deconstruct(diagram);

            int svgWidth = (int) svg.drawnBounds().getWidth(), svgHeight = (int) svg.drawnBounds().getHeight();

            if (width == null && height != null) {
                double ratio = svg.drawnBounds().getWidth() / svg.drawnBounds().getHeight();
                svgHeight = height;
                svgWidth = (int) Math.round(ratio * svgHeight);
            } else if (height == null && width != null) {
                double ratio = svg.drawnBounds().getWidth() / svg.drawnBounds().getHeight();
                svgWidth = width;
                svgHeight = (int) Math.round(svgWidth / ratio);
            } else if (width != null) {
                svgWidth = width;
                svgHeight = height;
            }

            if (solid) {
                setSolid(diagram.getRoot());
            }

            double wr = svgWidth / svg.drawnBounds().getWidth();
            double hr = svgHeight / svg.drawnBounds().getHeight();

            float s = (float) Math.min(wr, hr);

            if (outline.isPresent()) {
                setStroke(diagram.getRoot(), toHexString(outline.get().color()), outline.get().weight() / s);
            }

            if (fill != null) {
                setFill(diagram.getRoot(), fill);
            }

            return Result.of(Optional.ofNullable(new SVGLayer(
                    element.getId(),
                    src,
                    x,
                    y,
                    svg,
                    s,
                    verticalAlignment,
                    horizontalAlignment
            ).draw(graphics, wrap, draw, scale)));
        } catch (IOException | SVGException e) {
            return Result.error(e.getMessage());
        }
    }

    private void setStroke(SVGElement node, String color, float width) throws SVGException {
        if (node instanceof Path) {
            addOrSet(node, "stroke", color);
            addOrSet(node, "stroke-width", Float.toString(width));
        }

        for (int i = 0; i < node.getNumChildren(); ++i) {
            setStroke(node.getChild(i), color, width);
        }
    }

    private void setFill(SVGElement node, String color) throws SVGException {
        if (!node.hasAttribute("fill", AnimationElement.AT_CSS)) {
            node.addAttribute("fill", AnimationElement.AT_CSS, color);
        } else {
            node.setAttribute("fill", AnimationElement.AT_CSS, color);
        }

        for (int i = 0; i < node.getNumChildren(); ++i) {
            setFill(node.getChild(i), color);
        }
    }

    // TODO: Make this work
    private void setSolid(SVGElement node) throws SVGException {
        if (node instanceof Path path) {
            if (node.hasAttribute("fill-rule", AnimationElement.AT_CSS)) {
                node.setAttribute("fill-rule", AnimationElement.AT_CSS, "nonzero");
            } else {
                node.addAttribute("fill-rule", AnimationElement.AT_CSS, "nonzero");
            }

            path.updateTime(0);
        }

        for (int i = 0; i < node.getNumChildren(); ++i) {
            setSolid(node.getChild(i));
        }
    }

    private static void addOrSet(SVGElement node, String attribute, String value) throws SVGElementException {
        if (node.hasAttribute(attribute, AnimationElement.AT_CSS)) {
            node.setAttribute(attribute, AnimationElement.AT_CSS, value);
        } else {
            node.addAttribute(attribute, AnimationElement.AT_CSS, value);
        }
    }

    private String toHexString(int color) {
        return String.format("#%06x", (0xFFFFFF & color));
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

        if (renderableElements.isEmpty()) {
            minX = minY = 0;
            maxX = diagram.getWidth();
            maxY = diagram.getHeight();
        }

        return new SVG(diagram.getViewRect().getBounds(),
                new Rectangle(
                        (int) minX, (int) minY, (int) (maxX - minX) + 1, (int) (maxY - minY) + 1
                ),
                diagram, renderableElements);
    }
}
