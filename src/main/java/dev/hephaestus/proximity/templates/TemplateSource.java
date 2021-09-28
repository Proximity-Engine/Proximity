package dev.hephaestus.proximity.templates;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public interface TemplateSource {
    /**
     * @param file A list of images to try to get
     * @return the first image found
     */
    BufferedImage getImage(String file);
    InputStream getInputStream(String file) throws IOException;
    boolean exists(String file);
    String getTemplateName();

}
