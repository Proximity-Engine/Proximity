package dev.hephaestus.proximity.scripting;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import dev.hephaestus.proximity.Proximity;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.TemplateSource;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.text.TextComponent;
import dev.hephaestus.proximity.util.Outline;
import dev.hephaestus.proximity.util.Shadow;
import dev.hephaestus.proximity.xml.RenderableCard;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.function.Function;

public final class ScriptingUtil {
    private static final ThreadLocal<ScriptEngine> ENGINE = ThreadLocal.withInitial(() -> GraalJSScriptEngine.create(null, Context.newBuilder("js")
            .allowHostAccess(
                    HostAccess.newBuilder(HostAccess.ALL).targetTypeMapping(Value.class, Object.class, Value::hasArrayElements, v -> new LinkedList<>(v.as(List.class))).build()
            )
    ));

    private ScriptingUtil() {
    }

    public static <T> T applyFunction(dev.hephaestus.proximity.scripting.Context context, TemplateSource source, String name, Function<Object, T> handler, T defaultValue, Object... args) {
        String sep = FileSystems.getDefault().getSeparator();
        String file = "scripts" + sep + name.replace(".", sep) + ".js";

        if (source.exists(file)) {
            try {
                ScriptEngine engine = ENGINE.get();

                Bindings bindings = engine.createBindings();
                bindings.put("polyglot.js.allowAllAccess", true);
                engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

                engine.eval(new InputStreamReader(source.getInputStream(file)));
                Function<Object[], Object> fn = (Function<Object[], Object>) engine.eval("apply");

                Object[] passedArgs = new Object[args.length + 1];

                System.arraycopy(args, 0, passedArgs, 1, args.length);

                passedArgs[0] = context;

                Object result = fn.apply(passedArgs);

                return handler.apply(result);
            } catch (IOException | ScriptException e) {
                e.printStackTrace();
            }
        } else if (!name.isEmpty()) {
            Proximity.LOG.warn("Tried to use missing function '{}'", name);
        }

        return defaultValue;
    }

    public static List<List<TextComponent>> applyTextFunction(dev.hephaestus.proximity.scripting.Context context, String name, String input, RenderableCard card, Map<String, Style> styles, Style base) {
        JsonObject stylesJson = new JsonObject();

        for (var entry : styles.entrySet()) {
            stylesJson.add(entry.getKey(), entry.getValue().toJson());
        }

        return applyFunction(context, card, name, ScriptingUtil::handleTextFunction, Collections.singletonList(Collections.singletonList(new TextComponent.Literal(base, input))),
                input, card, stylesJson, base.toJson()
        );
    }

    private static List<List<TextComponent>> handleTextFunction(Object jsResult) {
        List<List<TextComponent>> result = new ArrayList<>();

        for (var list : (List<List<Map<String, ?>>>) jsResult) {
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
}
