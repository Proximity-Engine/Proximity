package dev.hephaestus.proximity.app.api.rendering.elements;

import dev.hephaestus.proximity.app.api.Parent;
import dev.hephaestus.proximity.app.api.RenderJob;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface Group<D extends RenderJob<?>> extends Parent<D>, Element<D>, Iterable<Element<D>> {
}
