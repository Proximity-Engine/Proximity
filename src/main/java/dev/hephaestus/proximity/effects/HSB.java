package dev.hephaestus.proximity.effects;

import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.xml.RenderableData;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class HSB {
    public static void apply(JsonObject card, BufferedImage image, RenderableData.XMLElement element) {
        String hue = element.getAttribute("hue");
        float dHue = (hue.endsWith("%")
                ? Float.parseFloat(hue.substring(0, hue.length() - 1))
                : Float.parseFloat(hue))/100;

        String saturation = element.getAttribute("saturation");
        float dSaturation = (saturation.endsWith("%")
                ? Float.parseFloat(saturation.substring(0, saturation.length() - 1))
                : Float.parseFloat(saturation))/100;

        String brightness = element.getAttribute("brightness");
        float dBrightness = (brightness.endsWith("%")
                ? Float.parseFloat(brightness.substring(0, brightness.length() - 1))
                : Float.parseFloat(brightness))/100;

        apply(image, dHue, dSaturation, dBrightness);
    }

    public static void apply(BufferedImage image, float dHue, float dSaturation, float dBrightness) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] rgb = image.getRGB( 0, 0, width, height, null, 0, width);

        for (int i = 0; i < rgb.length; ++i) {
            int a = (rgb[i] >> 24) & 0xFF;
            int r = (rgb[i] >> 16) & 0xFF;
            int g = (rgb[i] >> 8) & 0xFF;
            int b = rgb[i] & 0xFF;

            float[] hsb = Color.RGBtoHSB(r, g, b, null);

            hsb[0] += dHue;
            hsb[1] += dSaturation;
            hsb[2] += dBrightness;

            hsb[0] %= 1F;
            hsb[1] %= 1F;
            hsb[2] %= 1F;

            rgb[i] = (a << 24) | Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        }

        image.setRGB( 0, 0, width, height, rgb, 0, width);
    }
}
