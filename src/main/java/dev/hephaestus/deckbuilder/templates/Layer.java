package dev.hephaestus.deckbuilder.templates;

import dev.hephaestus.deckbuilder.util.StatefulGraphics;

import java.awt.*;

public abstract class Layer {
    public int x, y;

    public static Layer EMPTY = new Empty();

    protected Layer(int x, int y) {
        this.x = x;
        this.y = y;
    }

    protected abstract Rectangle draw(StatefulGraphics out, Rectangle wrap);

    private static class Empty extends Layer {
        protected Empty() {
            super(0, 0);
        }

        @Override
        public Rectangle draw(StatefulGraphics out, Rectangle wrap) {
            return new Rectangle();
        }
    }
}