package dev.hephaestus.proximity.app.api.rendering.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public interface ImageSource {
    InputStream get() throws IOException;

    Type getType();

    URL getUrl();

    enum Type {
        UNSET, TEMPLATE_RESOURCE, DYNAMIC
    }
}
