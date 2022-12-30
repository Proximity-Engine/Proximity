package dev.hephaestus.proximity.app.impl.rendering.elements;

import dev.hephaestus.proximity.app.api.rendering.Document;
import dev.hephaestus.proximity.app.api.rendering.RenderData;
import dev.hephaestus.proximity.app.api.rendering.elements.Image;
import dev.hephaestus.proximity.app.api.rendering.util.Alignment;
import dev.hephaestus.proximity.app.api.rendering.util.ImagePosition;
import dev.hephaestus.proximity.app.api.rendering.util.ImageSource;
import dev.hephaestus.proximity.app.api.rendering.util.Rect;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

import javax.imageio.ImageIO;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Consumer;

public class ImageImpl<D extends RenderData> extends ElementImpl<D> implements Image {
    public static final String[] IMAGE_FILE_TYPES = {
            "png", "jpg", "jpeg", "jif", "jfif", "jfi"
    };

    private final Property<ImageSource> source = new SimpleObjectProperty<>(new ImageSource() {
        @Override
        public InputStream get() {
            String src = ImageImpl.this.path;
            InputStream in = ImageImpl.this.document.getTemplate().getResource(src);

            // Check for files when the file extension is not specified
            for (int i = 0; in == null && i < IMAGE_FILE_TYPES.length; i++) {
                in = ImageImpl.this.document.getTemplate().getResource(src + "." + IMAGE_FILE_TYPES[i]);
            }

            return in;
        }

        @Override
        public Type getType() {
            return Type.TEMPLATE_RESOURCE;
        }

        @Override
        public URL getUrl() {
            throw new UnsupportedOperationException();
        }
    });

    private final Property<ImagePosition> position = new SimpleObjectProperty<>(new ImagePosition.Direct(0, 0));
    private final Property<Rect> imageDimensions = new SimpleObjectProperty<>();
    private final Property<Rectangle2D> bounds = new SimpleObjectProperty<>();

    public ImageImpl(String id, Document<D> document, ParentImpl<D> parent) {
        super(id, document, parent);

        this.imageDimensions.bind(Bindings.createObjectBinding(() -> {
            byte[] imageBytes = new byte[50];
            InputStream stream = this.source.getValue().get();

            int width;
            int height;

            if (stream == null) {
                return new Rect(0, 0);
            }

            if (stream.read(imageBytes, 0, 50) == 0) {
                ImageImpl.this.document.getErrors().add("Unexpected empty image");
                throw new RuntimeException("Unexpected empty image");
            } else if (imageBytes[1] == 'P' && imageBytes[2] == 'N' && imageBytes[3] == 'G') {
                // PNG
                if (imageBytes[12] == 'I' && imageBytes[13] == 'H' && imageBytes[14] == 'D' && imageBytes[15] == 'R') {
                    width = readInt(imageBytes, 16);
                    height = readInt(imageBytes, 20);
                } else {
                    ImageImpl.this.document.getErrors().add("Invalid PNG header");
                    throw new RuntimeException("Invalid PNG header");
                }
            } else if (imageBytes[0] == 0xFFFFFFFF && imageBytes[1] == 0xFFFFFFD8 && imageBytes[2] == 0xFFFFFFFF && imageBytes[3] == 0xFFFFFFE0) {
                // JPEG
                try {
                    BufferedImage image = ImageIO.read(this.source().get()); // It is unfortunate that we have to read the entire image with JPEG
                    width = image.getWidth();
                    height = image.getHeight();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                ImageImpl.this.document.getErrors().add("Unexpected file type");
                throw new RuntimeException("Unexpected file type");
            }

            return new Rect(width, height);
        }, this.source));

        this.bounds.bind(Bindings.createObjectBinding(() -> {
            if (this.imageDimensions.getValue() != null) {
                return this.position.getValue().getBounds(this.imageDimensions.getValue());
            } else {
                return null;
            }
        }, this.imageDimensions, this.position));
    }

    @Override
    protected void getAttributes(Consumer<Observable> attributes) {
        attributes.accept(this.source);
        attributes.accept(this.position);
    }

    @Override
    public void source(URL url) {
        this.source.setValue(new ImageSource() {
            @Override
            public InputStream get() throws IOException {
                return url.openStream();
            }

            @Override
            public Type getType() {
                return Type.DYNAMIC;
            }

            @Override
            public URL getUrl() {
                return url;
            }
        });
    }

    @Override
    public void source(String src) {
        this.source.setValue(new ImageSource() {
            @Override
            public InputStream get() {
                InputStream in = ImageImpl.this.document.getTemplate().getResource(src);

                // Check for files when the file extension is not specified
                for (int i = 0; in == null && i < IMAGE_FILE_TYPES.length; i++) {
                    in = ImageImpl.this.document.getTemplate().getResource(src + "." + IMAGE_FILE_TYPES[i]);
                }

                if (in == null) {
                    ImageImpl.this.document.getErrors().add(String.format("\"%s\" not found.%n", src));
                }

                return in;
            }

            @Override
            public Type getType() {
                return Type.TEMPLATE_RESOURCE;
            }

            @Override
            public URL getUrl() {
                throw new UnsupportedOperationException();
            }
        });
    }

    @Override
    public void pos(int x, int y) {
        this.position.setValue(new ImagePosition.Direct(x, y));
    }

    @Override
    public void fill(int x, int y, int width, int height) {
        this.position.setValue(new ImagePosition.Fill(x, y, width, height));
    }

    @Override
    public void cover(int x, int y, int width, int height, Alignment horizontalAlignment, Alignment verticalAlignment) {
        this.position.setValue(new ImagePosition.Cover(x, y, width, height, horizontalAlignment, verticalAlignment));
    }

    @Override
    public Rectangle2D bounds() {
        return this.bounds.getValue();
    }

    @Override
    public Property<Rectangle2D> boundsProperty() {
        return this.bounds;
    }

    @Override
    public ImageSource source() {
        return this.source.getValue();
    }

    @Override
    public Node render() {
        ImageView imageView = new ImageView();
        Group group = new Group(imageView);

        group.setUserData(this.bounds);

        group.clipProperty().bind(Bindings.createObjectBinding(() -> {
            var pos = this.position.getValue();

            if (pos instanceof ImagePosition.Cover cover) {
                return new Rectangle(cover.width(), cover.height());
            }

            return null;
        }, this.position, this.imageDimensions));

        imageView.imageProperty().bind(Bindings.createObjectBinding(() -> {
            return new javafx.scene.image.Image(this.source.getValue().get());
        }, this.source));

        group.translateXProperty().bind(Bindings.createDoubleBinding(() -> {
            return this.bounds.getValue().getMinX();
        }, this.bounds));

        group.translateYProperty().bind(Bindings.createDoubleBinding(() -> {
            return this.bounds.getValue().getMinY();
        }, this.bounds));

        imageView.fitWidthProperty().bind(Bindings.createDoubleBinding(() -> {
            return this.bounds.getValue().getWidth();
        }, this.bounds));

        imageView.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> {
            return this.bounds.getValue().getHeight();
        }, this.bounds));

        return group;
    }

    private static int readInt(byte[] bytes, int start) {
        return ((((int) bytes[start]) & 0xFF) << 24) | ((((int) bytes[start + 1]) & 0xFF) << 16) | ((((int) bytes[start + 2]) & 0xFF) << 8) | ((((int) bytes[start + 3]) & 0xFF));
    }
}
