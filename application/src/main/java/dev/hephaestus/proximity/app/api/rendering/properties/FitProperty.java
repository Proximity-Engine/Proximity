package dev.hephaestus.proximity.app.api.rendering.properties;

public interface FitProperty<D, R> {
    R x(int x);
    R y(int y);
    R pos(int x, int y);

    R fill(int x, int y, int width, int height);

    R cover(int x, int y, int width, int height);
}
