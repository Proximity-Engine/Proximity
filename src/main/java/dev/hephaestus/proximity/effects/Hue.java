package dev.hephaestus.proximity.effects;

import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.xml.RenderableData;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class Hue {
    public static void apply(JsonObject card, BufferedImage image, RenderableData.XMLElement element) {
        String stringAmount = element.getAttribute("amount");
        float hue = (stringAmount.endsWith("%")
                ? Float.parseFloat(stringAmount.substring(0, stringAmount.length() - 1))
                : Float.parseFloat(stringAmount))/100;

        HSB.apply(image, hue, 0, 0);
    }
}
