package dev.hephaestus.proximity.effects;

import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.xml.RenderableData;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.function.Function;

public class HSB {
    public static void apply(JsonObject card, BufferedImage image, RenderableData.XMLElement element) {
        String hue = element.hasAttribute("hue") ? element.getAttribute("hue") : "+0";
        String saturation = element.hasAttribute("saturation") ? element.getAttribute("saturation") : "+0";
        String brightness = element.hasAttribute("brightness") ? element.getAttribute("brightness") : "+0";

        apply(image, hue, saturation, brightness);
    }

    public static void hue(JsonObject card, BufferedImage image, RenderableData.XMLElement element) {
        HSB.apply(image, element.getAttribute("amount"), "+0", "+0");
    }

    public static void saturation(JsonObject card, BufferedImage image, RenderableData.XMLElement element) {
        HSB.apply(image, "+0", element.getAttribute("amount"), "+0");
    }

    public static void brightness(JsonObject card, BufferedImage image, RenderableData.XMLElement element) {
        HSB.apply(image, "+0", "+0", element.getAttribute("amount"));
    }

    public static void apply(BufferedImage image, String hue, String saturation, String brightness) {
        float dHueValue = (hue.endsWith("%")
                ? Float.parseFloat(hue.substring(0, hue.length() - 1))
                : Float.parseFloat(hue))/100;

        Function<Float, Float> dHue = hue.startsWith("+") || hue.startsWith("-")
                ? f -> f + dHueValue
                : f -> dHueValue;

        float dHueSaturationValue = (saturation.endsWith("%")
                ? Float.parseFloat(saturation.substring(0, saturation.length() - 1))
                : Float.parseFloat(saturation))/100;

        Function<Float, Float> dSaturation = saturation.startsWith("+") || saturation.startsWith("-")
                ? f -> f > 0 ? f + dHueSaturationValue : f
                : f -> f > 0 ? dHueSaturationValue : 0;

        float dBrightnessValue = (brightness.endsWith("%")
                ? Float.parseFloat(brightness.substring(0, brightness.length() - 1))
                : Float.parseFloat(brightness))/100;

        Function<Float, Float> dBrightness = brightness.startsWith("+") || brightness.startsWith("-")
                ? f -> f + dBrightnessValue
                : f -> dBrightnessValue;

        apply(image, dHue, dSaturation, dBrightness);
    }

    public static void apply(BufferedImage image, Function<Float, Float> dHue, Function<Float, Float> dSaturation, Function<Float, Float> dBrightness) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] rgb = image.getRGB( 0, 0, width, height, null, 0, width);

        for (int i = 0; i < rgb.length; ++i) {
            int a = (rgb[i] >> 24) & 0xFF;
            int r = (rgb[i] >> 16) & 0xFF;
            int g = (rgb[i] >> 8) & 0xFF;
            int b = rgb[i] & 0xFF;

            float[] hsb = Color.RGBtoHSB(r, g, b, null);

            hsb[0] = dHue.apply(hsb[0]);
            hsb[1] = dSaturation.apply(hsb[1]);
            hsb[2] = dBrightness.apply(hsb[2]);

            hsb[0] = ((hsb[0] % 1F) + 1) % 1F;
            hsb[1] = hsb[1] > 1 ? 1 : hsb[1] < 0 ? 0 : hsb[1];
            hsb[2] = hsb[2] > 1 ? 1 : hsb[2] < 0 ? 0 : hsb[2];

            rgb[i] = (a << 24) | (0x00FFFFFF & Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
        }

        image.setRGB( 0, 0, width, height, rgb, 0, width);
    }
}
