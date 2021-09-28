package dev.hephaestus.proximity.templates;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

public class FileSystemTemplateSource implements TemplateSource {
    private final String templateName;
    private final Function<String, Path> pathGetter;

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

            Path path = this.pathGetter.apply(image);

            return ImageIO.read(Files.newInputStream(path));
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public InputStream getInputStream(String name) throws IOException {
        return Files.newInputStream(this.pathGetter.apply(name));
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
