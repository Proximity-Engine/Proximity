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
import java.util.function.Function;

public class FileSystemTemplateSource implements TemplateSource {
    private final String templateName;
    private final Function<String, Path> pathGetter;
    private final Map<String, BufferedImage> imageCache = new HashMap<>();

    public FileSystemTemplateSource(Path path) throws IOException {
        String templateName = path.getFileName().toString();

        this.templateName = templateName.contains(".")
                ? templateName.substring(0, templateName.lastIndexOf("."))
                : templateName;

        if (path.toString().endsWith(".zip")) {
            FileSystem fileSystem = FileSystems.newFileSystem(path);
            this.pathGetter = fileSystem::getPath;
        } else if (Files.isDirectory(path)) {
            this.pathGetter = path::resolve;
        } else {
            throw new RuntimeException("Template must be either a zip file or a directory!");
        }
    }

    public BufferedImage getImage(String image) {
        try {
            if (image.startsWith("/")) {
                image = image.substring(1);
            }

            if (!this.imageCache.containsKey(image)) {
                Path path = this.pathGetter.apply(image);

                this.imageCache.put(image, ImageIO.read(Files.newInputStream(path)));
            }

            return this.imageCache.get(image);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public InputStream getInputStream(String first) throws IOException {
        return Files.newInputStream(this.pathGetter.apply(first));
    }

    @Override
    public boolean exists(String file) {
        return Files.exists(this.pathGetter.apply(file));
    }

    @Override
    public String getTemplateName() {
        return this.templateName;
    }
}
