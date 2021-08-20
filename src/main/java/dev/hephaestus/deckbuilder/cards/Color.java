package dev.hephaestus.deckbuilder.cards;

import java.util.HashMap;
import java.util.Map;

public final class Color {
    private static final Map<Character, Color> COLORS = new HashMap<>();

    public static final Color WHITE = Color.of('W', 255, 253, 234);
    public static final Color BLUE = Color.of('U', 167, 224, 249);
    public static final Color BLACK = Color.of('B', 213, 207, 207);
    public static final Color RED = Color.of('R', 252, 179, 146);
    public static final Color GREEN = Color.of('G', 166, 222, 187);

    public static final Color GOLD = Color.of(null, 245, 203, 66);
    public static final Color ARTIFACT = Color.of(null, 255, 32, 32);
    public static final Color LAND = Color.of(null, 255, 32, 32);
    public static final Color NONE = Color.of(null, 255, 32, 32);

    public static Color of(char symbol) {
        return COLORS.get(symbol);
    }

    private static Color of(Character symbol, int r, int g, int b) {
        if (symbol != null) {
            return COLORS.computeIfAbsent(symbol, c -> new Color(c, r, g, b));
        } else {
            return new Color(null, r, g, b);
        }
    }

    private final Character symbol;
    private final int value;

    private Color(Character symbol, int r, int g, int b) {
        this.symbol = symbol;
        this.value = r << 16 | g << 8 | b;
    }

    public char symbol() {
        return symbol;
    }

    public int a() {
        return (this.value >> 24) & 0xFF;
    }

    public int r() {
        return (this.value >> 16) & 0xFF;
    }

    public int g() {
        return (this.value >> 8) & 0xFF;
    }

    public int b() {
        return this.value & 0xFF;
    }

    public int rgb() {
        return this.value;
    }

    @Override
    public String toString() {
        return "Color[" +
                "symbol=" + symbol + ", " +
                "r=" + this.r() + ", " +
                "g=" + this.g() + ", " +
                "b=" + this.b() + ']';
    }
}
