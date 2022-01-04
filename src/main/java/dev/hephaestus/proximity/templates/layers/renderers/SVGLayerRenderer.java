package dev.hephaestus.proximity.templates.layers.renderers;

import dev.hephaestus.proximity.util.*;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableData;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.dom.svg.SVGDocumentFactory;
import org.apache.batik.gvt.CompositeGraphicsNode;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.*;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGRect;
import org.w3c.dom.svg.SVGSVGElement;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Optional;

public class SVGLayerRenderer extends LayerRenderer {
    public SVGLayerRenderer(RenderableData data) {
        super(data);
    }

    @Override
    public Result<Optional<Rectangles>> renderLayer(RenderableData card, RenderableData.XMLElement element, StatefulGraphics graphics, Rectangles wrap, boolean draw, Box<Float> scale, Rectangle2D bounds) {
        int x = (element.hasAttribute("x") ? Integer.decode(element.getAttribute("x")) : 0);
        int y = (element.hasAttribute("y") ? Integer.decode(element.getAttribute("y")) : 0);
        String src = element.hasAttribute("src") ? element.getAttribute("src") : element.getId();
        ContentAlignment verticalAlignment = element.hasAttribute("vertical_alignment") ? ContentAlignment.valueOf(element.getAttribute("vertical_alignment").toUpperCase(Locale.ROOT)) : ContentAlignment.MIDDLE;
        ContentAlignment horizontalAlignment = element.hasAttribute("horizontal_alignment") ? ContentAlignment.valueOf(element.getAttribute("horizontal_alignment").toUpperCase(Locale.ROOT)) : ContentAlignment.MIDDLE;
        Integer width = element.hasAttribute("width") ? Integer.decode(element.getAttribute("width")) : null;
        Integer height = element.hasAttribute("height") ? Integer.decode(element.getAttribute("height")) : null;

        String file = ParsingUtil.getFileLocation(element.getParentId(), element.getAttribute("id"), src) + ".svg";

        return this.load(card, file).then(document -> {
            SVGSVGElement root = document.getRootElement();

            GVTBuilder builder = new GVTBuilder();

            GraphicsNode graphicsNode = builder.build(new BridgeContext(new UserAgentAdapter()), document);
            graphicsNode.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Rectangle2D svgBounds = graphicsNode.getBounds();
            float s = 1;

            if (width != null || height != null) {
                double svgWidth, svgHeight;

                if (root.hasAttribute("viewBox")) {
                    SVGRect rect = root.getViewBox().getBaseVal();
                    svgBounds = new Rectangle2D.Double(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                    svgWidth = svgBounds.getWidth();
                    svgHeight = svgBounds.getHeight();
                } else if (root.hasAttribute("width") && root.hasAttribute("height")) {
                    String w = root.getAttribute("width");
                    String h = root.getAttribute("height");
                    svgWidth = Double.parseDouble(w);
                    svgHeight = Double.parseDouble(h);
                } else {
                    return Result.error("SVG file must have 'viewbox' or 'width' and 'height' attributes to scale!");
                }

                double frameWidth = svgWidth, frameHeight;

                if (width == null) {
                    double ratio = frameWidth / svgHeight;
                    frameHeight = height;
                    frameWidth = ratio * frameHeight;
                } else if (height == null) {
                    double ratio = frameWidth / svgHeight;
                    frameWidth = width;
                    frameHeight = frameWidth / ratio;
                } else {
                    frameWidth = width;
                    frameHeight = height;
                }

                double vScale = frameHeight / svgHeight;
                double hScale = frameWidth / svgWidth;
                s = (float) Math.min(vScale, hScale);
            }

            return Result.of(Optional.ofNullable(new Job(
                    element.getId(),
                    file,
                    x,
                    y,
                    svgBounds,
                    graphicsNode,
                    s,
                    verticalAlignment,
                    horizontalAlignment
            ).draw(graphics)));
        });
    }

    private Result<SVGDocument> load(RenderableData card, String src) {
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputStream inputStream = card.getInputStream(src);
            Document document = documentBuilder.parse(inputStream);
            Element root = document.getDocumentElement();
            NodeList listOfDefs = root.getElementsByTagName("defs");

            if (listOfDefs.getLength() > 0) {
                for (int i = 0; i < listOfDefs.getLength(); ++i) {
                    Node child = listOfDefs.item(i);

                    if (child instanceof Element defs) {
                        NodeList defsList = defs.getChildNodes();

                        for (int j = 0; j < defsList.getLength(); ++j) {
                            child = defsList.item(j);

                            if (child instanceof Element e && e.getParentNode() == defs) {
                                Element to = (Element) document.adoptNode(e.cloneNode(true));

                                if (e.hasAttribute("id") && card.hasGradient(e.getAttribute("id"))) {
                                    Element from = (Element) card.getGradient(e.getAttribute("id")).cloneNode(true);
                                    copyStopColors(from, to);
                                }

                                // Sometimes we define gradients in icons that refer to the original rarity gradient.
                                // In those cases, we need to copy over the colors to each defined gradient that
                                // references the rarity as well.
                                if (e.hasAttribute("xlink:href") && card.hasGradient(e.getAttribute("xlink:href").substring(1))) {
                                    Element from = (Element) card.getGradient(e.getAttribute("xlink:href").substring(1)).cloneNode(true);
                                    copyStopColors(from, to);
                                }

                                defs.replaceChild(to, e);
                            }
                        }
                    }
                }
            } else {
                Element defs = document.createElement("defs");

                for (Element element : card.getGradients()) {
                    defs.appendChild(document.adoptNode(element.cloneNode(true)));
                }
            }

            SVGDocumentFactory factory = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());
            String svgString = toString(document);

            return Result.of(factory.createSVGDocument(src, new StringReader(svgString)));
        } catch (ParserConfigurationException | IOException | SAXException | DOMException e) {
            return Result.error(ExceptionUtil.getErrorMessage(e));
        }
    }

    private static void copyStopColors(Element from, Element to) {
        NodeList stops = to.getChildNodes();
        NodeList oldStops = from.getChildNodes();

        int l = 0;
        Node oldStop = null;

        for (int k = 0; k < stops.getLength(); ++k) {
            Node stop = stops.item(k);

            if (!(stop instanceof Element)) continue;

            Node nextStop;

            for (; l < oldStops.getLength(); ++l) {
                nextStop = oldStops.item(l);

                if (nextStop instanceof Element) {
                    oldStop = nextStop;
                    ++l;
                    break;
                }
            }
            if (oldStop instanceof Element element) {
                copyStyle(element, (Element) stop);
            }
        }
    }

    private static void copyStyle(Element from, Element to) {
        if (from.hasAttribute("style")) {
            to.setAttribute("style", from.getAttribute("style"));
        }
    }

    private static String toString(Document doc) {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }

    public static class Job {
        private final String id;
        private final int x, y;
        protected Rectangle2D bounds;
        protected Rectangles wrap;
        private final String src;
        private final Rectangle2D svgBounds;
        private final GraphicsNode svg;
        private final float scale;
        private final ContentAlignment verticalAlignment, horizontalAlignment;

        public Job(String id, String src, int x, int y, Rectangle2D svgBounds, GraphicsNode svg, float scale, ContentAlignment verticalAlignment, ContentAlignment horizontalAlignment) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.src = src;
            this.svgBounds = svgBounds;
            this.svg = svg;
            this.scale = scale;
            this.verticalAlignment = verticalAlignment;
            this.horizontalAlignment = horizontalAlignment;
        }

        public Rectangles draw(StatefulGraphics out) {
            int x = this.x, y = this.y;

            switch (this.horizontalAlignment) {
                case MIDDLE -> x -= (int) (this.svgBounds.getWidth() * this.scale * 0.5);
                case END -> x -= (int) (this.svgBounds.getWidth() * this.scale);
            }

            switch (this.verticalAlignment) {
                case MIDDLE -> y -= (int) (this.svgBounds.getHeight() * this.scale * 0.5);
                case END -> y -= (int) (this.svgBounds.getHeight() * this.scale);
            }

            out.push((int) (x - this.svgBounds.getX() * this.scale), (int) (y - this.svgBounds.getY() * this.scale));
            out.push(this.scale, this.scale);
            out.push(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            this.adjust(this.svg.getRoot());
            this.svg.getRoot().setClip(null);
            this.svg.getRoot().paint(out);

            out.pop(3);

            return Rectangles.singleton(new Rectangle(x, y, (int) (this.svgBounds.getWidth() * this.scale), (int) (this.svgBounds.getHeight() * this.scale)));
        }

        private void adjust(Object object) {
            if (object instanceof CompositeGraphicsNode composite) {
                for (Object o : composite) {
                    adjust(o);
                }
            }

            if (object instanceof GraphicsNode node) {
                node.setClip(null);
            }
        }

        @Override
        public String toString() {
            return "SVG[" + this.id + ";" + this.src + "]";
        }
    }
}
