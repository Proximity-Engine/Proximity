package dev.hephaestus.proximity.app.impl.logging;

import dev.hephaestus.proximity.app.api.logging.Log;

public final class NamedLog implements Log {
    private final LogImpl logger;
    private final String name;

    public NamedLog(LogImpl logger, String name) {
        this.logger = logger;
        this.name = name;
    }

    @Override
    public Log derive(String name) {
        return this.logger.derive(name);
    }

    @Override
    public void print(String message, Object... args) {
        this.logger.print(this.name, message, false, args);
    }

    @Override
    public void print(Throwable error) {
        this.logger.print(this.name, error, false);
    }

    @Override
    public void print(String message, Throwable error) {
        this.logger.print(message, error);
    }

    @Override
    public void write(String message, Object... args) {
        this.logger.print(this.name, message, true, args);
    }

    @Override
    public void write(String message, Throwable error) {
        this.logger.write(message, error);
    }

    @Override
    public void write(Throwable error) {
        this.logger.print(this.name, error, true);
    }
}
