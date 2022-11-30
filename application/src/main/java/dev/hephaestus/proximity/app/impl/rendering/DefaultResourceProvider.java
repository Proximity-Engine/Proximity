package dev.hephaestus.proximity.app.impl.rendering;

import dev.hephaestus.proximity.app.api.ResourceProvider;
import dev.hephaestus.proximity.app.api.exceptions.ResourceNotFoundException;

import java.net.URL;

public class DefaultResourceProvider implements ResourceProvider {
    private final ClassLoader classLoader;

    public DefaultResourceProvider(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public boolean hasResource(String name) {
        return this.classLoader.getResource(name) != null;
    }

    @Override
    public URL getResource(String name) throws ResourceNotFoundException {
        URL url = this.classLoader.getResource(name);

        if (url == null) {
            throw new ResourceNotFoundException(name);
        }

        return url;
    }
}
