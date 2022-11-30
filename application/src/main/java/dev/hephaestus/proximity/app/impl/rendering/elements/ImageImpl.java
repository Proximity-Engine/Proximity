package dev.hephaestus.proximity.app.impl.rendering.elements;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.rendering.elements.Image;
import dev.hephaestus.proximity.app.api.rendering.properties.Property;
import dev.hephaestus.proximity.app.api.rendering.properties.ThrowingProperty;
import dev.hephaestus.proximity.app.api.rendering.util.BoundingBoxes;
import dev.hephaestus.proximity.app.api.rendering.util.ImagePosition;
import dev.hephaestus.proximity.app.api.rendering.util.Rect;
import dev.hephaestus.proximity.app.impl.rendering.DocumentImpl;
import dev.hephaestus.proximity.app.impl.rendering.properties.BasicProperty;
import dev.hephaestus.proximity.app.impl.rendering.properties.BasicThrowingProperty;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public final class ImageImpl<D extends RenderJob> extends ElementImpl<D> implements Image<D> {
    public static final String[] IMAGE_FILE_TYPES = {
            "png", "jpg", "jpeg", "jif", "jfif", "jfi"
    };

    private final VisibilityProperty<Image<D>> visibility;
    private final BasicProperty<D, ImagePosition, Image<D>> position;
    private final BasicThrowingProperty<D, URL, Image<D>, MalformedURLException> src;

    private Rect dimensions;

    public ImageImpl(DocumentImpl<D> document, String id, ElementImpl<D> parent) {
        super(document, id, parent);

        D data = document.getData();

        this.visibility = new VisibilityProperty<Image<D>>(this, data);
        this.position = new BasicProperty<>(this, data, new ImagePosition.Direct(0, 0));
        this.src = new BasicThrowingProperty<>(this, data);

        this.src.set(d -> document.getResourceLocation(this.getPath(), IMAGE_FILE_TYPES));
    }

    @Override
    public ThrowingProperty<D, URL, Image<D>, MalformedURLException> src() {
        return this.src;
    }

    @Override
    public Property<D, ImagePosition, Image<D>> position() {
        return this.position;
    }

    @Override
    public VisibilityProperty<Image<D>> visibility() {
        return this.visibility;
    }

    @Override
    public BoundingBoxes getDimensions() {
        return new BoundingBoxes(this.position.get().getBounds(this.calculateDimensions()));
    }

    private byte[] getImageBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            InputStream in = this.src.get().openStream();

            // Check for files when the file extension is not specified
            for (int i = 0; in == null && i < IMAGE_FILE_TYPES.length; i++) {
                in = this.getDocument().getTemplate().getResourceAsStream(this.src + "." + IMAGE_FILE_TYPES[i]);
            }

            if (in != null) {
                in.transferTo(out);
            } else {
                // TODO: Errors!
                System.out.printf("\"%s\" not found.%n", this.src);
            }
        } catch (IOException e) {
            // TODO: Errors!
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    @Override
    public String getFormat() {
        byte[] imageBytes = this.getImageBytes();

        if (imageBytes[1] == 'P' && imageBytes[2] == 'N' && imageBytes[3] == 'G') {
            return  "png";
        } else if (imageBytes[0] == 0xFFFFFFFF && imageBytes[1] == 0xFFFFFFD8 && imageBytes[2] == 0xFFFFFFFF && imageBytes[3] == 0xFFFFFFE0) {
            return  "jpeg";
        } else {
            // TODO: Better errors
            throw new RuntimeException("Unexpected file type!");
        }
    }

    @Override
    public Rect getSourceImageDimensions() {
        return this.calculateDimensions();
    }

    private Rect calculateDimensions() {
        if (this.dimensions == null) {
            byte[] imageBytes = this.getImageBytes();

            int width;
            int height;

            if (imageBytes[1] == 'P' && imageBytes[2] == 'N' && imageBytes[3] == 'G') {
                // PNG
                if (imageBytes[12] == 'I' && imageBytes[13] == 'H' && imageBytes[14] == 'D' && imageBytes[15] == 'R') {
                    width = readInt(imageBytes, 16);
                    height = readInt(imageBytes, 20);
                } else {
                    // TODO: Better errors
                    throw new RuntimeException("Invalid PNG header!");
                }
            } else if (imageBytes[0] == 0xFFFFFFFF && imageBytes[1] == 0xFFFFFFD8 && imageBytes[2] == 0xFFFFFFFF && imageBytes[3] == 0xFFFFFFE0) {
                // JPEG
                try {
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
                    width = image.getWidth();
                    height = image.getHeight();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                // TODO: Better errors
                throw new RuntimeException("Unexpected file type!");
            }

            this.dimensions = new Rect(width, height);
        }

        return this.dimensions;
    }

    private static int readInt(byte[] bytes, int start) {
        return ((((int) bytes[start]) & 0xFF) << 24) | ((((int) bytes[start + 1]) & 0xFF) << 16) | ((((int) bytes[start + 2]) & 0xFF) << 8) | ((((int) bytes[start + 3]) & 0xFF));
    }
}
