package dev.hephaestus.proximity.api.tasks;

import dev.hephaestus.proximity.Proximity;
import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.plugins.TaskDefinition;
import dev.hephaestus.proximity.plugins.util.TaskParser;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.text.TextComponent;
import dev.hephaestus.proximity.util.Outline;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.util.Shadow;
import org.w3c.dom.Element;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public interface TextFunction {
    TaskDefinition<TextFunction, List<List<TextComponent>>> DEFINITION = new TextFunction.Definition();

    List<List<TextComponent>> apply(String input, JsonObject data, Function<String, Style> styles, Style baseStyle);

    final class Definition extends TaskDefinition<TextFunction, List<List<TextComponent>>> {
        private Definition() {
            super("TextFunction");
        }

        @Override
        public TextFunction from(Function<Object[], Object> fn) {
            return (input, data, styles, baseStyle) -> this.interpretScriptResult(
                    fn.apply(new Object[] { input, data, styles, baseStyle })
            );
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public List<List<TextComponent>> interpretScriptResult(Object object) {
            List<List<TextComponent>> result = new ArrayList<>();

            for (var list : (List<List<Map<String, ?>>>) object) {
                List<TextComponent> group = new ArrayList<>(list.size());

                for (var components : list) {
                    Object styleObject = components.get("style");
                    JsonObject styleJson;

                    if (styleObject instanceof Map) {
                        styleJson = JsonObject.interpret((Map) components.get("style"));
                    } else if (styleObject instanceof JsonObject) {
                        styleJson = (JsonObject) components.get("style");
                    } else {
                        throw new RuntimeException();
                    }

                    Style.Builder style = new Style.Builder();

                    if (styleJson.has("fontName")) style.font(styleJson.getAsString("fontName"));
                    if (styleJson.has("italicFontName")) style.italics(styleJson.getAsString("italicFontName"));
                    if (styleJson.has("size")) style.size(styleJson.getAsInt("size"));
                    if (styleJson.has("kerning")) style.kerning(styleJson.getAsFloat("kerning"));

                    if (styleJson.has("shadow")) {
                        style.shadow(new Shadow(
                                styleJson.getAsInt("shadow", "color"),
                                styleJson.getAsInt("shadow", "dX"),
                                styleJson.getAsInt("shadow", "dY")
                        ));
                    }

                    if (styleJson.has("outline")) {
                        style.outline(new Outline(
                                styleJson.getAsInt("outline", "color"),
                                styleJson.getAsFloat("outline", "weight")
                        ));
                    }

                    if (styleJson.has("capitalization"))
                        style.capitalization(Style.Capitalization.valueOf(styleJson.getAsString("capitalization").toUpperCase(Locale.ROOT)));
                    if (styleJson.has("color")) style.color(styleJson.getAsInt("color"));

                    group.add(new TextComponent.Literal(style.build(), (String) components.get("value")));
                }

                result.add(group);
            }

            return result;
        }

        @Override
        public Result<TextFunction> parse(ClassLoader classLoader, Element element) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
            return TaskParser.parseHandler(classLoader, element).join(
                    object -> Result.of((TextFunction) object),
                    handler -> Result.of((input, data, styles, baseStyle) -> {
                        try {
                            //noinspection unchecked
                            return (List<List<TextComponent>>) handler.invoke(null, input, data, styles, baseStyle);
                        } catch (Exception e) {
                            Proximity.LOG.error(e.getMessage());
                            return null;
                        }
                    })
            );
        }
    }
}
