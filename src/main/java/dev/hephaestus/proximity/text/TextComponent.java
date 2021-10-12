package dev.hephaestus.proximity.text;

public interface TextComponent {
    Style style();
    String string();

    record Literal(Style style, String string) implements TextComponent {
        public Literal(Style style, char c) {
            this(style, "" + c);
        }
    }
}
