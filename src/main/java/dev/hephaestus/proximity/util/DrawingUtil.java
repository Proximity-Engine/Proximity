package dev.hephaestus.proximity.util;

import dev.hephaestus.proximity.templates.TemplateSource;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DrawingUtil {
    private static final Map<String, Font> FONTS = new ConcurrentHashMap<>();
    private static final Map<Integer, Color> COLORS = new ConcurrentHashMap<>();

    private DrawingUtil() {}

    public static Font getFont(TemplateSource files, String fontName, float size) {
        Font font = FONTS.computeIfAbsent(fontName, name -> {
            String fontLocation = null;

            if (files.exists("fonts/" + fontName + ".otf")) {
                fontLocation = "fonts/" + fontName + ".otf";
            } else if (files.exists("fonts/" + fontName + ".ttf")) {
                fontLocation = "fonts/" + fontName + ".ttf";
            }

            if (fontLocation != null) {
                try {
                    InputStream stream = files.getInputStream(fontLocation);

                    return stream != null ? Font.createFont(Font.TRUETYPE_FONT, stream) : null;
                } catch (FontFormatException | IOException ignored) {
                }
            }

            return new Font(fontName, Font.PLAIN, (int) size);
        });

        return font == null ? null : font.deriveFont(size);
    }

    public static Color getColor(int color) {
        return COLORS.computeIfAbsent(color, value -> new Color(value, ((value >>> 24) > 0)));
    }

    public static Rectangle2D encompassing(Rectangle2D r1, Rectangle2D r2) {
        double x = Math.min(r1.getX(), r2.getX());
        double y = Math.min(r1.getY(), r2.getY());

        double width = Math.max(
                r1.getMaxX(),
                r2.getMaxX()
        ) - x;

        double height = Math.max(
                r1.getMaxY(),
                r2.getMaxY()
        ) - y;

        return new Rectangle2D.Double(x, y, width, height);
    }
}
