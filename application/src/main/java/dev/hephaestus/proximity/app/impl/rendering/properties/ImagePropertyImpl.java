package dev.hephaestus.proximity.app.impl.rendering.properties;

import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.rendering.elements.Image;
import dev.hephaestus.proximity.app.api.rendering.properties.ImageProperty;
import dev.hephaestus.proximity.app.api.rendering.util.ThrowingFunction;
import dev.hephaestus.proximity.app.impl.rendering.elements.ImageImpl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Supplier;

public class ImagePropertyImpl<D extends RenderJob> implements ImageProperty<D> {
    private final D data;
    private final Image<D> result;

    private Supplier<InputStream> value = InputStream::nullInputStream;

    public ImagePropertyImpl(Image<D> result, D data) {
        this.data = data;
        this.result = result;
    }

    @Override
    public InputStream get() throws IOException {
        return this.value.get();
    }

    @Override
    public Image<D> set(ThrowingFunction<D, URL, IOException> getter) {
        this.value = new UrlGetter<>(this.data, getter);

        return this.result;
    }

    @Override
    public Image<D> set(String src) {
        this.value = () -> this.result.getDocument().getResource(src, ImageImpl.IMAGE_FILE_TYPES);

        return this.result;
    }

    @Override
    public Type getType() {
        if (this.value instanceof UrlGetter) {
            return Type.DYNAMIC;
        } else if (this.value != null) {
            return Type.TEMPLATE_RESOURCE;
        } else {
            return Type.UNSET;
        }
    }

    public URL getUrl() {
        if (this.value instanceof UrlGetter<?> urlGetter) {
            return urlGetter.getUrl();
        } else {
            throw new UnsupportedOperationException("Can't get URL from non-dynamic image source.");
        }
    }

    public Supplier<InputStream> getter() {
        return this.value;
    }

    private static class UrlGetter<D extends RenderJob> implements Supplier<InputStream> {
        private final D data;
        private final ThrowingFunction<D, URL, IOException> getter;

        private UrlGetter(D data, ThrowingFunction<D, URL, IOException> getter) {
            this.data = data;
            this.getter = getter;
        }

        @Override
        public InputStream get() {
            try {
                return this.getter.apply(this.data).openStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public URL getUrl() {
            try {
                return this.getter.apply(this.data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
