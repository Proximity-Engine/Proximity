package dev.hephaestus.proximity.util;

import dev.hephaestus.proximity.TemplateFiles;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DrawingUtil {
    private static final Map<String, Font> FONTS = new ConcurrentHashMap<>();
    private static final Map<Integer, Color> COLORS = new ConcurrentHashMap<>();

    private DrawingUtil() {}

    // TODO: Allow loading fonts from template zip files
    public static Font getFont(TemplateFiles files, String fontName, float size) {
        Font font = FONTS.computeIfAbsent(fontName, name -> {
            try {
                InputStream stream = files.getInputStream("fonts/" + fontName + ".ttf");

                return stream != null ? Font.createFont(Font.TRUETYPE_FONT, stream) : null;
            } catch (FontFormatException | IOException e) {
                e.printStackTrace();
                return null;
            }
        });

        return font == null ? null : font.deriveFont(size);
    }

    public static Color getColor(int color) {
        return COLORS.computeIfAbsent(color, Color::new);
    }

    public static Rectangle encompassing(Rectangle r1, Rectangle r2) {
        int x = Math.min(r1.x, r2.x);
        int y = Math.min(r1.y, r2.y);

        int width = Math.max(
                r1.x + r1.width,
                r2.x + r2.width
        ) - x;

        int height = Math.max(
                r1.y + r1.height,
                r2.y + r2.height
        ) - y;

        return new Rectangle(x, y, width, height);
    }
}
