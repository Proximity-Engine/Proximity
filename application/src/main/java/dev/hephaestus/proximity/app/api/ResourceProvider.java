package dev.hephaestus.proximity.app.api;

import dev.hephaestus.proximity.app.api.exceptions.ResourceNotFoundException;

import java.io.InputStream;

public interface ResourceProvider {
    boolean hasResource(String name);
    InputStream getResource(String name) throws ResourceNotFoundException;
}
