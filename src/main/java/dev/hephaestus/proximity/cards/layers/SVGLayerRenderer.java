package dev.hephaestus.proximity.cards.layers;

import dev.hephaestus.proximity.templates.layers.SVGLayer;
import dev.hephaestus.proximity.util.*;
import dev.hephaestus.proximity.xml.LayerProperty;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableCard;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGRect;
import org.w3c.dom.svg.SVGSVGElement;

import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
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
        boolean forceOutline = element.hasAttribute("force_outline") && Boolean.parseBoolean(element.getAttribute("solid"));
        Integer fill = element.hasAttribute("fill") ? element.getInteger("fill") : null;

        Outline outline = element.getProperty(LayerProperty.OUTLINE);

        src = ParsingUtil.getFileLocation(element.getParentId(), element.getAttribute("id"), src) + ".svg";

        try {
            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());

            SVGDocument document = factory.createSVGDocument(src, card.getInputStream(src));
            SVGSVGElement root = document.getRootElement();

            GVTBuilder builder = new GVTBuilder();
            GraphicsNode graphicsNode = builder.build(new BridgeContext(new UserAgentAdapter()), document);
            graphicsNode.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            SVGRect svgBounds = root.getViewBox().getBaseVal();

            double svgWidth = svgBounds.getWidth(), svgHeight = svgBounds.getHeight();

            if (width == null && height != null) {
                double ratio = svgBounds.getWidth() / svgBounds.getHeight();
                svgHeight = height;
                svgWidth = ratio * svgHeight;
            } else if (height == null && width != null) {
                double ratio = svgBounds.getWidth() / svgBounds.getHeight();
                svgWidth = width;
                svgHeight = svgWidth / ratio;
            } else if (width != null) {
                svgWidth = width;
                svgHeight = height;
            }

            double wr = svgWidth / svgBounds.getWidth();
            double hr = svgHeight / svgBounds.getHeight();

            float s = (float) Math.min(wr, hr);

            return Result.of(Optional.ofNullable(new SVGLayer(
                    element.getId(),
                    src,
                    x,
                    y,
                    fill,
                    outline,
                    forceOutline,
                    svgBounds,
                    graphicsNode,
                    s,
                    verticalAlignment,
                    horizontalAlignment
            ).draw(graphics, wrap, draw, scale)));
        } catch (IOException e) {
            return Result.error(e.getMessage());
        }
    }
}
