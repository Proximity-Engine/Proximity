package dev.hephaestus.proximity.app.api.text;

import dev.hephaestus.proximity.app.api.text.TextStyle;

public class TextComponent {
    public TextStyle style;
    public String text;
    public boolean italic;

    public TextComponent(TextStyle style, String text, boolean isItalic) {
        this.style = style;
        this.text = text;
        this.italic = isItalic;
    }

    public TextComponent(TextStyle style, String text) {
        this(style, text, false);
    }

    public TextComponent(String text) {
        this(new TextStyle(null), text);
    }
}
