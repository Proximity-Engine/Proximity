package dev.hephaestus.proximity.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Stack;

public class XMLHandler extends DefaultHandler implements LexicalHandler {
    private final static String LINE_NUMBER_KEY_NAME = "lineNumber";

    private final Document doc;
    private final StringBuilder textBuffer = new StringBuilder();
    private final Stack<Element> elementStack = new Stack<>();
    private Locator locator;
    private StringBuilder comment = new StringBuilder();

    public XMLHandler(Document doc) {
        this.doc = doc;
    }

    @Override
    public void setDocumentLocator(final Locator locator) {
        this.locator = locator; // Save the locator, so that it can be used later for line tracking when traversing nodes.
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {
        addTextIfNeeded();
        final Element el = doc.createElement(qName);

        for (int i = 0; i < attributes.getLength(); i++) {
            el.setAttribute(attributes.getQName(i), attributes.getValue(i));
        }

        if (!this.comment.isEmpty()) {
            el.setUserData("comment", this.comment.toString(), null);
            this.comment = new StringBuilder();
        }

        el.setUserData(LINE_NUMBER_KEY_NAME, String.valueOf(this.locator.getLineNumber()), null);
        elementStack.push(el);
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) {
        addTextIfNeeded();

        final Element closedEl = elementStack.pop();

        if (elementStack.isEmpty()) {
            doc.appendChild(closedEl);
        } else {
            final Element parentEl = elementStack.peek();
            parentEl.appendChild(closedEl);
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) {
        textBuffer.append(ch, start, length);
    }

    // Outputs text accumulated under the current node
    private void addTextIfNeeded() {
        if (textBuffer.length() > 0) {
            final Element el = elementStack.peek();
            final Node textNode = doc.createTextNode(textBuffer.toString());
            el.appendChild(textNode);
            textBuffer.delete(0, textBuffer.length());
        }
    }

    @Override
    public void startDTD(String name, String publicId, String systemId) {

    }

    @Override
    public void endDTD() {

    }

    @Override
    public void startEntity(String name) {

    }

    @Override
    public void endEntity(String name) {

    }

    @Override
    public void startCDATA() {

    }

    @Override
    public void endCDATA() {

    }

    @Override
    public void comment(char[] ch, int start, int length) {
        if (!this.comment.isEmpty()) {
            this.comment.append('\n');
        }

        this.comment.append(new String(ch, start, length).trim());
    }
}
