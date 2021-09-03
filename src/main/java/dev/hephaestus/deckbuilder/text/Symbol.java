package dev.hephaestus.deckbuilder.text;

import dev.hephaestus.deckbuilder.TextComponent;
import dev.hephaestus.deckbuilder.templates.Template;

import java.util.*;

public record Symbol(String glyphs, int color) {
    private static final String[] COLORS = new String[] {"W", "U", "B", "R", "G"};

    private static final Map<String, Symbol> DEFAULT_SYMBOLS = new HashMap<>();

    private static final int BLACK1 = 0xFF000000;

    public static final int WHITE = 0xFFFFFDEA;
    public static final int BLUE = 0xFFA7E0F9;
    public static final int BLACK = 0xFFBEBCB5;
    public static final int RED = 0xFFFFAA92;
    public static final int GREEN = 0xFFA6DEBB;
    public static final int GENERIC = 0xFFBEBCB5;

    private static final Map<String, Factory> SYMBOLS = new HashMap<>();

    static {
        symbol("W", WHITE);
        symbol("U", BLUE);
        symbol("B", BLACK);
        symbol("R", RED);
        symbol("G", GREEN);
        symbol("C", GENERIC);

        // Phyrexian colored costs
        for (String color : COLORS) {
            phyrexian(color);
        }

        register("P", BLACK1, GENERIC, true, "p");

        // Normal colored costs
        for (String color : COLORS) {
            normal(color);
        }

        register("C");
        register("S", "n");

        // Hybrid costs
        for (String symbol1 : COLORS) {
            for (String symbol2 : COLORS) {
                if (!symbol1.equals(symbol2)) {
                    hybrid(symbol1, symbol2);
                }
            }

            hybrid("2", symbol1);
        }

        // Generic costs
        register("X");
        register("10", "A");
        register("11", "B");
        register("12", "C");
        register("13", "D");
        register("14", "E");
        register("15", "F");
        register("16", "G");
        register("20", "H");

        for (int i = 0; i < 10; ++i) {
            register("" + i);
        }

        // Tap
        register("T", GENERIC);

        // Untap
        register("Q", GENERIC, BLACK1, false, "l");

        // Miscellaneous
        register("CHAOS", BLACK1, 0, false, "?");
        register("E", (template, base, context) -> {
            Style mods = template.getStyle(context.name + ".E");
            base = base.merge(mods == null ? template.getStyle("E") : mods);

            return Collections.singletonList(new TextComponent(copyWithDefaultColor(base, BLACK1), "e"));
        });
    }

    public static Symbol of(String symbol) {
        return DEFAULT_SYMBOLS.get(symbol);
    }

    private static void symbol(String symbol, int color) {
        DEFAULT_SYMBOLS.put(symbol, new Symbol(symbol, color));
    }

    public static List<TextComponent> symbol(String symbol, Template template, Style base, Factory.Context context) {
        if (!SYMBOLS.containsKey(symbol)) {
            System.out.printf("Unrecognized glyphs '%s'%n", symbol);
            return Collections.emptyList();
        }

        return SYMBOLS.get(symbol).apply(template, base, context);
    }

    private static void register(String symbol, Factory function) {
        SYMBOLS.put(symbol, function);
    }

    private static void phyrexian(String symbol) {
        register(symbol + "/P", BLACK1, DEFAULT_SYMBOLS.get(symbol).color, true, "p");
    }

    private static void normal(String symbol) {
        register(symbol, BLACK1, DEFAULT_SYMBOLS.get(symbol).color, false, symbol.toLowerCase(Locale.ROOT));
    }

    private static void hybrid(String symbol1, String symbol2) {
        Symbol color1 = DEFAULT_SYMBOLS.get(symbol1);
        Symbol color2 = DEFAULT_SYMBOLS.get(symbol2);

        register(symbol1 + "/" + symbol2, new Hybrid(
                symbol1,
                color1 == null ? GENERIC : color1.color,
                symbol2,
                color2 == null ? GENERIC : color2.color
        ));
    }

    private static void register(String symbol, int symbolColor, int backgroundColor, boolean bigCircle, String glyphs) {
        register(symbol, new Single(symbol, symbolColor, backgroundColor, bigCircle, glyphs));
    }

    private static void register(String symbol, int backgroundColor, String glyphs) {
        register(symbol, BLACK1, backgroundColor, false, glyphs);
    }

    private static void register(String symbol, int backgroundColor) {
        register(symbol, backgroundColor, symbol.toLowerCase(Locale.ROOT));
    }

    private static void register(String symbol) {
        register(symbol, GENERIC);
    }

    private static void register(String symbol, String glyphs) {
        register(symbol, GENERIC, glyphs);
    }

    private record Single(String symbol, int symbolColor, int backgroundColor, boolean bigCircle, String glyphs) implements Factory {
        @Override
        public List<TextComponent> apply(Template template, Style base, Context context) {
            ArrayList<TextComponent> text = new ArrayList<>(2);

            Style mods = template.getStyle(context.name + "." + this.symbol);
            base = base.merge(mods == null ? template.getStyle(this.symbol) : template.getStyle(this.symbol));

            text.add(
                    new TextComponent(
                            copyWithDefaultColor(base, this.backgroundColor),
                            this.bigCircle ? "Q" : "o"
                    )
            );

            text.add(
                    new TextComponent(
                            copyWithDefaultColor(base, this.symbolColor).shadow(null),
                            this.glyphs
                    )
            );

            return text;
        }
    }

    private record Hybrid(String symbol1, int backgroundColor1, String symbol2, int backgroundColor2) implements Factory {
        private static final Map<String, String> SYMBOLS_TO_GLYPHS_1 = new HashMap<>();
        private static final Map<String, String> SYMBOLS_TO_GLYPHS_2 = new HashMap<>();

        static {
            SYMBOLS_TO_GLYPHS_1.put("W", "L");
            SYMBOLS_TO_GLYPHS_1.put("U", "M");
            SYMBOLS_TO_GLYPHS_1.put("B", "N");
            SYMBOLS_TO_GLYPHS_1.put("R", "O");
            SYMBOLS_TO_GLYPHS_1.put("G", "P");
            SYMBOLS_TO_GLYPHS_1.put("2", "W");

            SYMBOLS_TO_GLYPHS_2.put("W", "R");
            SYMBOLS_TO_GLYPHS_2.put("U", "S");
            SYMBOLS_TO_GLYPHS_2.put("B", "T");
            SYMBOLS_TO_GLYPHS_2.put("R", "U");
            SYMBOLS_TO_GLYPHS_2.put("G", "V");
        }

        public Hybrid(String symbol1, int backgroundColor1, String symbol2, int backgroundColor2) {
            this.symbol1 = SYMBOLS_TO_GLYPHS_1.get(symbol1);
            this.backgroundColor1 = backgroundColor1;
            this.symbol2 = SYMBOLS_TO_GLYPHS_2.get(symbol2);
            this.backgroundColor2 = backgroundColor2;
        }

        @Override
        public List<TextComponent> apply(Template template, Style base, Context context) {
            ArrayList<TextComponent> text = new ArrayList<>(4);

            Style mods1 = template.getStyle(context.name + "." + this.symbol1);
            Style mods2 = template.getStyle(context.name + "." + this.symbol2);

            Style style1 = base.merge(mods1 == null ? template.getStyle(this.symbol1) : mods1);
            Style style2 = base.merge(mods2 == null ? template.getStyle(this.symbol2) : mods2);

            // Big circle
            text.add(new TextComponent(
                    copyWithDefaultColor(style1, backgroundColor2),
                    "Q"
            ));

            // Hybrid half
            text.add(new TextComponent(
                    copyWithDefaultColor(style2, backgroundColor1).shadow(null),
                    "q"
            ));

            text.add(new TextComponent(
                    copyWithDefaultColor(style1, BLACK1).shadow(null),
                    this.symbol1
            ));

            text.add(new TextComponent(
                    copyWithDefaultColor(style2, BLACK1).shadow(null),
                    this.symbol2
            ));

            return text;
        }
    }

    private static Style copyWithDefaultColor(Style base, int color) {
        return new Style(
                base.fontName(),
                base.italicFontName(),
                base.size(),
                base.color() == null ? color : base.color(),
                base.shadow(),
                base.outline()
        );
    }

    public interface Factory {
        List<TextComponent> apply(Template template, Style base, Context context);

        record Context(String name) {
        }
    }
}
