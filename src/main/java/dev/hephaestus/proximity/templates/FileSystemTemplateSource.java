package dev.hephaestus.proximity.templates;

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

public class FileSystemTemplateSource implements TemplateSource {
    private final String templateName;
    private final PathGetter pathGetter;
    private final Map<String, BufferedImage> imageCache = new HashMap<>();

    public FileSystemTemplateSource(Path path) throws IOException {
        String templateName = path.getFileName().toString();

        this.templateName = templateName.contains(".")
                ? templateName.substring(0, templateName.lastIndexOf("."))
                : templateName;

        if (path.endsWith(".zip")) {
            FileSystem fileSystem = FileSystems.newFileSystem(path);
            this.pathGetter = fileSystem::getPath;
        } else if (Files.isDirectory(path)) {
            this.pathGetter = (first, more) -> {
                Path p = path.resolve(first);

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

    @Override
    public String getTemplateName() {
        return this.templateName;
    }

    private interface PathGetter {
        Path apply(String first, String... more);
    }
}
