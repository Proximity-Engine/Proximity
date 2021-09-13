package dev.hephaestus.proximity.templates;

import dev.hephaestus.proximity.text.TextComponent;
import dev.hephaestus.proximity.cards.TextParser;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.text.Symbol;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface TextFunction {
    Pattern COST_SYMBOLS = Pattern.compile("\\G\\{([^}]*)}");

    Map<String, TextFunction> FUNCTIONS = new HashMap<>() {{
        put("parse_cost", TextFunction::parse_cost);
        put("oracle_text", TextFunction::oracleText);
        put("flavor_text", TextFunction::flavorText);
    }};

    List<List<TextComponent>> apply(Map<String, Style> styles, Style base, String target);

    static List<List<TextComponent>> apply(Map<String, Style> styles, Style base, String function, String target) {
        if (function.isEmpty() || !FUNCTIONS.containsKey(function)) return Collections.singletonList(Collections.singletonList(new TextComponent(base, target)));

        return FUNCTIONS.get(function).apply(styles, base, target);
    }

    private static List<List<TextComponent>> parse_cost(Map<String, Style> styles, Style base, String target) {
        Matcher matcher = COST_SYMBOLS.matcher(target);

        List<TextComponent> result = new ArrayList<>();

        while (matcher.find()) {
            String symbol = matcher.group(1);
            result.addAll(Symbol.symbol(symbol, styles, base, new Symbol.Factory.Context("cost")));
        }

        return Collections.singletonList(result);
    }

    static List<List<TextComponent>> oracleText(Map<String, Style> styles, Style base, String target) {
        return new TextParser(target, styles, base, "\n\n").parseOracle().text();
    }
    static List<List<TextComponent>> flavorText(Map<String, Style> styles, Style base, String target) {
        return new TextParser(target, styles, base, "\n").parseFlavor().text();
    }
}
