package dev.hephaestus.proximity.cards.layers;

import dev.hephaestus.proximity.templates.layers.SVGLayer;
import dev.hephaestus.proximity.util.*;
import dev.hephaestus.proximity.xml.LayerRenderer;
import dev.hephaestus.proximity.xml.RenderableCard;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.dom.svg.SVGDocumentFactory;
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
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Optional;

public class SVGLayerRenderer extends LayerRenderer {
    @Override
    public Result<Optional<Rectangles>> renderLayer(RenderableCard card, RenderableCard.XMLElement element, StatefulGraphics graphics, Rectangles wrap, boolean draw, Box<Float> scale, Rectangle2D bounds) {
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

            return Result.of(Optional.ofNullable(new SVGLayer(
                    element.getId(),
                    file,
                    x,
                    y,
                    svgBounds,
                    graphicsNode,
                    s,
                    verticalAlignment,
                    horizontalAlignment
            ).draw(graphics, wrap, draw, scale)));
        });
    }

    private Result<SVGDocument> load(RenderableCard card, String src) {
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = documentBuilder.parse(card.getInputStream(src));
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
            return Result.error(e.getMessage());
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
                copyAttribute("style", element, (Element) stop);
            }
        }
    }

    private static void copyAttribute(String name, Element from, Element to) {
        if (from.hasAttribute(name)) {
            to.setAttribute(name, from.getAttribute(name));
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

}
