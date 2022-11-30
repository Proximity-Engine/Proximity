package dev.hephaestus.proximity.app.api;

import dev.hephaestus.proximity.app.api.exceptions.ResourceNotFoundException;

import java.net.URL;

public interface ResourceProvider {
    boolean hasResource(String name);
    URL getResource(String name) throws ResourceNotFoundException;
}
