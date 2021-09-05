package dev.hephaestus.proximity;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public interface TemplateFiles {
    BufferedImage getImage(String... images);
    InputStream getInputStream(String first, String... more) throws IOException;

    class Implementation implements TemplateFiles {
        private final PathGetter pathGetter;
        private final Map<String, BufferedImage> imageCache = new HashMap<>();

        public Implementation(Path template) throws IOException {
            if (template.getFileName().toString().endsWith(".zip")) {
                FileSystem fileSystem = FileSystems.newFileSystem(template);
                this.pathGetter = fileSystem::getPath;
            } else if (Files.isDirectory(template)) {
                this.pathGetter = (first, more) -> {
                    Path p = template.resolve(first);

                    for (String s : more) {
                        p = p.resolve(s);
                    }

                    return p;
                };
            } else {
                throw new RuntimeException("Template must be either a zip file or a directory!");
            }
        }

        public BufferedImage getImage(String... images) {
            try {
                for (String image : images) {
                    if (image.startsWith("/")) {
                        image = image.substring(1);
                    }

                    if (!this.imageCache.containsKey(image)) {
                        Path path = this.pathGetter.apply(image);

                        this.imageCache.put(image, ImageIO.read(Files.newInputStream(path)));
                    }

                    return this.imageCache.get(image);
                }
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }

            throw new RuntimeException("Invalid resources: [" + String.join(", ", images) + "]");
        }

        public InputStream getInputStream(String first, String... more) throws IOException {
            return Files.newInputStream(this.pathGetter.apply(first, more));
        }

        private interface PathGetter {
            Path apply(String first, String... more);
        }
    }
}
