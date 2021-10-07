package dev.hephaestus.proximity.templates;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface TemplateSource {
    /**
     * @param file A list of images to try to get
     * @return the first image found
     */
    BufferedImage getImage(String file);
    InputStream getInputStream(String file) throws IOException;
    boolean exists(String file);
    String getTemplateName();

    final class Compound implements TemplateSource {
        public final List<TemplateSource> wrapped;

        public Compound(TemplateSource... sources) {
            this.wrapped = new ArrayList<>(Arrays.asList(sources));
        }

        @Override
        public BufferedImage getImage(String file) {
            for (TemplateSource source : this.wrapped) {
                if (source.exists(file)) {
                    return source.getImage(file);
                }
            }

            return null;
        }

        @Override
        public InputStream getInputStream(String file) throws IOException {
            for (TemplateSource source : this.wrapped) {
                if (source.exists(file)) {
                    return source.getInputStream(file);
                }
            }

            return null;
        }

        @Override
        public boolean exists(String file) {
            for (TemplateSource source : this.wrapped) {
                if (source.exists(file)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public String getTemplateName() {
            return "Compound";
        }
    }
}
