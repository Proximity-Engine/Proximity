package dev.hephaestus.proximity.app.impl.exceptions;

public class PluginInstantiationException extends Exception {
    public PluginInstantiationException() {
    }

    public PluginInstantiationException(String message) {
        super(message);
    }

    public PluginInstantiationException(String message, Object... args) {
        super(String.format(message, args));
    }

    public PluginInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginInstantiationException(Throwable cause) {
        super(cause);
    }

    public PluginInstantiationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
