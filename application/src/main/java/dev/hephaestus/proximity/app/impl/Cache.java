package dev.hephaestus.proximity.app.impl;

import dev.hephaestus.proximity.app.api.logging.Log;
import dev.hephaestus.proximity.json.api.Json;
import dev.hephaestus.proximity.json.api.JsonElement;
import dev.hephaestus.proximity.json.api.JsonNumber;
import dev.hephaestus.proximity.json.api.JsonObject;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

public final class Cache {
    private final Log log;
    private final Path dir;
    private final Path path;
    private final JsonObject.Mutable json;
    private int saveCount = 0;

    @ApiStatus.Internal
    public Cache(Log log, Path dir) {
        this.log = log;

        JsonObject.Mutable json;

        this.dir = dir;
        this.path = this.dir.resolve("cache.json");

        try {
            Files.createDirectories(this.dir);

            if (Files.exists(this.path)) {
                json = Json.parseObject(this.path);

                for (Iterator<Map.Entry<String, JsonElement>> iterator = json.iterator(); iterator.hasNext(); ) {
                    Map.Entry<String, JsonElement> entry = iterator.next();

                    if (((JsonNumber) entry.getValue()).get().longValue() < System.currentTimeMillis()) {
                        iterator.remove();
                    }
                }
            } else {
                json = JsonObject.create();
            }
        } catch (IOException e) {
            json = JsonObject.create();

            this.log.write("Failed to load cache", e);
        }

        this.json = json;
    }

    /**
     * Saves the remote URL to the local cache and returns the URL referencing the saved file.
     *
     * @param url some remote resource
     * @return a URL to a local file
     */
    public URL cache(URL url) throws IOException {
        return switch (url.getProtocol()) {
            case "file" -> url;
            case "http", "https" -> {
                Path dir = this.dir.resolve(URLEncoder.encode(url.getHost(), StandardCharsets.US_ASCII)).resolve(URLEncoder.encode(url.getPath().substring(1), StandardCharsets.US_ASCII));
                Path dest = dir.resolve(URLEncoder.encode(url.getQuery(), StandardCharsets.US_ASCII));

                Files.createDirectories(dir);

                if (this.json.has(dest.toString()) && this.json.getLong(dest.toString()) < System.currentTimeMillis()) {
                    if (Files.exists(dest)) {
                        Files.delete(dest);
                    }
                }

                if (!Files.exists(dest)) {
                    Files.copy(url.openStream(), dest);
                    this.json.put(dest.toString(), Json.create(System.currentTimeMillis() + 259200000)); // Cache for 72 hours

                    Thread t = new Thread(this::save);

                    t.setDaemon(true);
                    t.setName("cache-worker-thread-" + this.saveCount++);
                    t.start();
                }

                yield dest.toUri().toURL();
            }
            default -> throw new IllegalStateException("Unexpected URL protocol: " + url.getProtocol());
        };
    }

    private synchronized void save() {
        try {
            this.json.write(this.path);
        } catch (IOException e) {
            this.log.print(e);
        }
    }
}
