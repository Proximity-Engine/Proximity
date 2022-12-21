package dev.hephaestus.proximity.app.api;

import dev.hephaestus.proximity.app.api.plugins.DataWidget;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class Proximity {
    /**
     * Saves the remote URL to the local cache and returns the URL referencing the saved file.
     *
     * @param url some remote resource
     * @return a URL to a local file
     */
    public static URL cache(URL url) throws IOException {
        return dev.hephaestus.proximity.app.impl.Proximity.cache(url);
    }

    public static boolean isPaused() {
        return dev.hephaestus.proximity.app.impl.Proximity.isPaused();
    }

    public static void pause() {
        dev.hephaestus.proximity.app.impl.Proximity.pause();
    }

    public static void resume() {
        dev.hephaestus.proximity.app.impl.Proximity.resume();
    }

    public static void setPauseText(String text) {
        dev.hephaestus.proximity.app.impl.Proximity.setPauseText(text);
    }

    public static void print(String message, Object... args) {
        dev.hephaestus.proximity.app.impl.Proximity.print(message, args);
    }

    public static void print(Throwable error) {
        dev.hephaestus.proximity.app.impl.Proximity.print(error);
    }

    public static void write(String message, Object... args) {
        dev.hephaestus.proximity.app.impl.Proximity.write(message, args);
    }

    public static void write(Throwable error) {
        dev.hephaestus.proximity.app.impl.Proximity.write(error);
    }

    public static void select(DataWidget<?> widget) {
        dev.hephaestus.proximity.app.impl.Proximity.select(widget);
    }

    public static void select(DataWidget<?>.Entry entry) {
        dev.hephaestus.proximity.app.impl.Proximity.select(entry);
    }

    public static void add(List<DataWidget<?>> rows) {
        dev.hephaestus.proximity.app.impl.Proximity.add(rows);
    }
}
