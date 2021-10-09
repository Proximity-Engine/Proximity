package dev.hephaestus.proximity.templates;

import dev.hephaestus.proximity.util.RemoteFileCache;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class RemoteFileSource implements TemplateSource {
    private final RemoteFileCache cache;
    private final String location;

    public RemoteFileSource(RemoteFileCache cache, String location) {
        this.cache = cache;
        this.location = location;
    }

    @Override
    public BufferedImage getImage(String image) {
        try {
            if (image.startsWith("/")) {
                image = image.substring(1);
            }


            return ImageIO.read(this.cache.open(URI.create(this.location + "/" + URLEncoder.encode(image.replace("\\", "/"), StandardCharsets.UTF_8))));
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public InputStream getInputStream(String file) throws IOException {
        return this.cache.open(URI.create(this.location + "/" + URLEncoder.encode(file.replace("\\", "/"), StandardCharsets.UTF_8)));
    }

    @Override
    public boolean exists(String file) {
        return this.cache.exists(URI.create(this.location + "/" + URLEncoder.encode(file.replace("\\", "/"), StandardCharsets.UTF_8)));
    }

    @Override
    public String getTemplateName() {
        return null;
    }
}
