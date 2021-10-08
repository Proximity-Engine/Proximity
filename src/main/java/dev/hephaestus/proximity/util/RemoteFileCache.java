package dev.hephaestus.proximity.util;

import dev.hephaestus.proximity.json.JsonObject;
import org.quiltmc.json5.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public final class RemoteFileCache {
    private static final char[] CHARS = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private final JsonObject index;
    private final Map<String, Boolean> existenceCache = new ConcurrentHashMap<>();

    private RemoteFileCache(JsonObject index) {
        this.index = index;
    }

    public static RemoteFileCache load() throws IOException {
        Path path = Path.of(".cache", "index.json");

        if (Files.exists(path)) {
            return new RemoteFileCache(JsonObject.parseObject(JsonReader.json(path)));
        } else {
            return new RemoteFileCache(new JsonObject());
        }
    }

    public boolean exists(URI file) {
        return this.existenceCache.computeIfAbsent(file.toString(), key -> this.index.has(file.toString()) || existsRemotely(file));
    }

    private boolean existsRemotely(URI file) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(file)
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            return false;
        }
    }

    public InputStream open(URI file) throws IOException {
        if (this.index.has(file.toString())) {
            return Files.newInputStream(Path.of(this.index.getAsString(file.toString())));
        } else {
            Path path;

            do {
                path = Path.of(".cache", randomId());
            } while (Files.exists(path));

            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }

            Files.copy(
                    file.toURL().openStream(),
                    path
            );

            this.index.addProperty(file.toString(), path.toString());

            Files.writeString(Path.of(".cache", "index.json"), this.index.toString());

            return Files.newInputStream(path);
        }
    }

    private static String randomId() {
        Random random = new Random();
        char[] result = new char[32];

        for (int i = 0; i < result.length; i++) {
            int randomCharIndex = random.nextInt(CHARS.length);
            result[i] = CHARS[randomCharIndex];
        }
        return new String(result);
    }
}
