package dev.hephaestus.proximity.util;

import com.kitfox.svg.RenderableElement;

import java.awt.Rectangle;
import java.util.List;

public record SVG(Rectangle documentBounds, Rectangle drawnBounds, List<RenderableElement> elements) {
}
