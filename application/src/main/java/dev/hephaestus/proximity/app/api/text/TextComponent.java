package dev.hephaestus.proximity.app.api.text;

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

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TextComponent component && component.text.equals(this.text) && component.style.equals(this.style) && component.italic == this.italic;
    }
}
