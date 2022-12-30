package dev.hephaestus.proximity.app.api.rendering.elements;

import java.util.Optional;

public interface Selector extends Parent, Element {
    Optional<Element> getSelected();
}
