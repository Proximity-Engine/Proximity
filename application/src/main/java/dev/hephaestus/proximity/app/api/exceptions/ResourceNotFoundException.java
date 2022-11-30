package dev.hephaestus.proximity.app.api.exceptions;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(Throwable e) {
        super(e);
    }

    public ResourceNotFoundException(String resource) {
        this("Resource '%s' not found.", resource);
    }

    public ResourceNotFoundException(String s, Object... args) {
        super(String.format(s, args));
    }
}
