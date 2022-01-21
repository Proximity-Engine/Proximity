package dev.hephaestus.proximity.util;

import dev.hephaestus.proximity.Proximity;
import dev.hephaestus.proximity.api.json.JsonObject;
import org.quiltmc.json5.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
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
        Path cache = Path.of(".cache");
        Path path = cache.resolve("index.json");

        if (!Files.exists(cache)) {
            Files.createDirectories(cache);
        }

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
    private Result<Path> fetch(URI file) throws IOException {
        return Result.of(this.compute(file, uri -> uri.toURL().openStream()));
    }

    public InputStream open(URI file) throws IOException {
        if (this.index.has(file.toString())) {
            return Files.newInputStream(Path.of(this.index.getAsString(file.toString())));
        } else {
            Result<Path> result = fetch(file);

            if (result.isError()) {
                Proximity.LOG.info(result.getError());
                return null;
            } else {
                return Files.newInputStream(result.get());
            }
        }
    }

    public Path compute(URI file, Fetcher fetcher) throws IOException {
        if (!this.index.has(file.toString())) {
            Path path;

            do {
                path = Path.of(".cache", randomId());
            } while (Files.exists(path));

            Files.copy(fetcher.fetch(file), path);

            this.index.addProperty(file.toString(), path.toString());

            Files.writeString(Path.of(".cache", "index.json"), this.index.toString());

            return path;
        } else {
            return Path.of(this.index.getAsString(file.toString()));
        }
    }

    public Result<URL> getLocation(URI file) throws IOException {
        if (this.index.has(file.toString())) {
            try {
                return Result.of(Path.of(this.index.getAsString(file.toString())).toUri().toURL());
            } catch (MalformedURLException e) {
                return Result.error(ExceptionUtil.getErrorMessage(e));
            }
        } else {
            Result<Path> result = fetch(file);

            if (result.isError()) {
                return result.unwrap();
            } else {
                return Result.of(result.get().toUri().toURL());
            }
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

    public interface Fetcher {
        InputStream fetch(URI uri) throws IOException;
    }
}
