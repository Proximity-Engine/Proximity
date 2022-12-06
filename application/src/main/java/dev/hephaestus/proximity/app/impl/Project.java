package dev.hephaestus.proximity.app.impl;

import java.nio.file.Path;

public class Project {
    private Path path;

    private Path lastOpenedDirectory;
    private Path lastSavedDirectory;

    public Path getPath() {
        return this.path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public void save() {
        // TODO
    }

    public Path getLastOpenedDirectory() {
        return this.lastOpenedDirectory == null ? this.path : this.lastOpenedDirectory;
    }

    public Path getLastSavedDirectory() {
        return this.lastSavedDirectory == null ? this.path : this.lastSavedDirectory;
    }

    public void setLastOpenedDirectory(Path path) {
        this.lastOpenedDirectory = path;
    }

    public void setLastSavedDirectory(Path path) {
        this.lastSavedDirectory = path;
    }
}
