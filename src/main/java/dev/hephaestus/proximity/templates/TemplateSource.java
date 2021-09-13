package dev.hephaestus.proximity.templates;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public interface TemplateSource {
    /**
     * @param images A list of images to try to get
     * @return the first image found
     */
    BufferedImage getImage(String... images);
    InputStream getInputStream(String first, String... more) throws IOException;
    String getTemplateName();
}
