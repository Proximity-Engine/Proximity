package dev.hephaestus.proximity.templates;

import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.text.TextComponent;
import dev.hephaestus.proximity.cards.TextParser;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.text.Symbol;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface TextFunction {
    Pattern COST_SYMBOLS = Pattern.compile("\\G\\{([^}]*)}");

    Map<String, TextFunction> FUNCTIONS = new HashMap<>() {{
        put("parse_cost", TextFunction::parse_cost);
        put("oracle_text", TextFunction::oracleText);
        put("flavor_text", TextFunction::flavorText);
    }};

    List<List<TextComponent>> apply(Function<String, Style> styleGetter, Style base, String target, JsonObject options);

    static List<List<TextComponent>> apply(Function<String, Style> styleGetter, Style base, String function, String target, JsonObject options) {
        if (function.isEmpty() || !FUNCTIONS.containsKey(function)) return Collections.singletonList(Collections.singletonList(new TextComponent(base, target)));

        return FUNCTIONS.get(function).apply(styleGetter, base, target, options);
    }

    private static List<List<TextComponent>> parse_cost(Function<String, Style> styleGetter, Style base, String target, JsonObject options) {
        Matcher matcher = COST_SYMBOLS.matcher(target);

        List<TextComponent> result = new ArrayList<>();

        while (matcher.find()) {
            String symbol = matcher.group(1);
            result.addAll(Symbol.symbol(symbol, styleGetter, base, new Symbol.Factory.Context("cost")));
        }

        return Collections.singletonList(result);
    }

    static List<List<TextComponent>> oracleText(Function<String, Style> styleGetter, Style base, String target, JsonObject options) {
        return new TextParser(target, styleGetter, base, "\n\n", options).parseOracle().text();
    }


    static List<List<TextComponent>> flavorText(Function<String, Style> styleGetter, Style base, String target, JsonObject options) {
        return new TextParser(target, styleGetter, base, "\n", options).parseFlavor().text();
    }
}
