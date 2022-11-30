package dev.hephaestus.proximity.app.api.rendering.util;

public interface ImagePosition {
    int x();

    int y();

    BoundingBox getBounds(Rect imageDimensions);

    /**
     * Draws the image at the given location without resizing it.
     */
    record Direct(int x, int y) implements ImagePosition {
        @Override
        public BoundingBox getBounds(Rect imageDimensions) {
            return new BoundingBox(this.x, this.y, imageDimensions.width(), imageDimensions.height(), false);
        }
    }

    /**
     * Stretches the image proportionally to cover the given area.
     *
     * <p>Some portions of the image may ie outside of the given area.</p>
     */
    record Cover(int x, int y, int width, int height, Alignment horizontalAlignment,
                 Alignment verticalAlignment) implements ImagePosition {
        @Override
        public BoundingBox getBounds(Rect imageDimensions) {
            int x, y, width, height;

            double rW = ((double) this.width) / imageDimensions.width();
            double rH = ((double) this.height) / imageDimensions.height();
            double r = Math.max(rW, rH);

            width = (int) (imageDimensions.width() * r);
            height = (int) (imageDimensions.height() * r);

            x = switch (this.horizontalAlignment) {
                case START -> this.x;
                case CENTER -> this.x + (this.width / 2) - (width / 2);
                case END -> this.x + this.width - width;
            };

            y = switch (this.verticalAlignment) {
                case START -> this.y;
                case CENTER -> this.y + (this.height / 2) - (height / 2);
                case END -> this.y + this.height - height;
            };

            return new BoundingBox(x, y, width, height, false);
        }
    }

    /**
     * Fills the given area, stretching the image to fit.
     */
    record Fill(int x, int y, int width, int height) implements ImagePosition {
        @Override
        public BoundingBox getBounds(Rect imageDimensions) {
            return new BoundingBox(this.x, this.y, this.width, this.height, false);
        }
    }
}
