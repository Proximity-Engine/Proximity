package dev.hephaestus.proximity.util;

import com.kitfox.svg.RenderableElement;
import com.kitfox.svg.SVGDiagram;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.List;

public record SVG(Rectangle documentBounds, Rectangle2D drawnBounds, SVGDiagram diagram, List<RenderableElement> elements) {
}
