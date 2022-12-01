package dev.hephaestus.proximity.app.impl.rendering;

import dev.hephaestus.proximity.app.api.ResourceProvider;
import dev.hephaestus.proximity.app.api.exceptions.ResourceNotFoundException;

import java.io.IOException;
import java.io.InputStream;

public class DefaultResourceProvider implements ResourceProvider {
    private final Module module;

    public DefaultResourceProvider(Module module) {
        this.module = module;
    }

    @Override
    public boolean hasResource(String name) {
        try {
            return this.module.getResourceAsStream(name) != null;
        } catch (IOException ignored) {
            return false;
        }
    }

    @Override
    public InputStream getResource(String name) throws ResourceNotFoundException {
        try {
            var is = this.module.getResourceAsStream(name);

            if (is == null) {
                throw new ResourceNotFoundException(name);
            }

            return is;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
