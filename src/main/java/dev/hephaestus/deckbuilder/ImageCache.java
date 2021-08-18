package dev.hephaestus.deckbuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ImageCache {
    private final Map<String, BufferedImage> cache = new HashMap<>();

    public BufferedImage get(String... images) {
        try {
            for (String image : images) {
                if (image.startsWith("/")) {
                    image = image.substring(1);
                }

                if (this.cache.containsKey(image)) {
                    return this.cache.get(image);
                } else {
                    Path userOverride = Path.of("templates", image + ".png");
                    InputStream stream;

                    if (Files.exists(userOverride)) {
                        stream = Files.newInputStream(userOverride);

                        this.cache.put(image, ImageIO.read(stream));

                        return this.cache.get(image);
                    } else {
                        image = "/" + image;

                        stream = Main.class.getResourceAsStream(image);

                        if (stream != null) {
                            this.cache.put(image, ImageIO.read(stream));
                            return this.cache.get(image);
                        }
                    }
                }
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        throw new RuntimeException("Invalid resources: [" + String.join(", ", images) + "]");
    }
}
