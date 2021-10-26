package dev.hephaestus.proximity.util;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import dev.hephaestus.proximity.Proximity;
import dev.hephaestus.proximity.templates.TemplateSource;
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
    private static final ThreadLocal<ScriptEngine> ENGINE = new ThreadLocal<>();

    private ScriptingUtil() {
    }

    private static ScriptEngine getEngine() {
        if (ENGINE.get() == null) {
            ScriptEngine engine = GraalJSScriptEngine.create(null, Context.newBuilder("js")
                    .allowHostAccess(
                            HostAccess.newBuilder(HostAccess.EXPLICIT).targetTypeMapping(Value.class, Object.class, Value::hasArrayElements, v -> new LinkedList<>(v.as(List.class))).build()
                    ));

            Bindings bindings = engine.createBindings();
            bindings.put("polyglot.js.allowAllAccess", true);
            engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

            try {
                engine.eval("const Proximity = Java.type(\"dev.hephaestus.proximity.Proximity\")");
                engine.eval("const JsonArray = Java.type(\"dev.hephaestus.proximity.api.json.JsonArray\")");
                engine.eval("const JsonObject = Java.type(\"dev.hephaestus.proximity.api.json.JsonArray\")");
                engine.eval("const JsonPrimitive = Java.type(\"dev.hephaestus.proximity.api.json.JsonArray\")");
                engine.eval("const JsonNull = Java.type(\"dev.hephaestus.proximity.api.json.JsonArray\")");
            } catch (ScriptException e) {
                e.printStackTrace();
            }

            ENGINE.set(engine);
        }

        return ENGINE.get();
    }

    public static Function<Object[], Object> getFunction(TemplateSource source, String name) {
        String sep = FileSystems.getDefault().getSeparator();
        String file = "scripts" + sep + name.replace(".", sep) + ".js";

        if (source.exists(file)) {
            try {
                ScriptEngine engine = getEngine();

                engine.eval(new InputStreamReader(source.getInputStream(file)));
                return (Function<Object[], Object>) engine.eval("apply");

            } catch (IOException | ScriptException e) {
                e.printStackTrace();
            }
        } else if (!name.isEmpty()) {
            Proximity.LOG.warn("Tried to use missing function '{}'", name);
        }

        return null;
    }
}
