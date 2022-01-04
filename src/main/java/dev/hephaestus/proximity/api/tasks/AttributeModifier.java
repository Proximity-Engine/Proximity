package dev.hephaestus.proximity.api.tasks;

import dev.hephaestus.proximity.Proximity;
import dev.hephaestus.proximity.api.json.JsonElement;
import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.plugins.TaskDefinition;
import dev.hephaestus.proximity.plugins.util.TaskParser;
import dev.hephaestus.proximity.util.ExceptionUtil;
import dev.hephaestus.proximity.util.Result;
import org.w3c.dom.Element;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

public interface AttributeModifier {
    TaskDefinition<AttributeModifier, String> DEFINITION = new AttributeModifier.Definition();

    String apply(JsonElement input, JsonObject data);

    final class Definition extends TaskDefinition<AttributeModifier, String> {
        private Definition() {
            super("AttributeModifier");
        }

        @Override
        public AttributeModifier from(Function<Object[], Object> fn) {
            return (input, data) -> this.interpretScriptResult(fn.apply(new Object[] { input, data }));
        }

        @Override
        public String interpretScriptResult(Object object) {
            if (object instanceof String string) return string;

            throw new RuntimeException("Attribute functions must return a String! Got " + object);
        }

        @Override
        public Result<AttributeModifier> parse(ClassLoader classLoader, Element element) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
            return TaskParser.parseHandler(classLoader, element).join(
                    object -> Result.of((AttributeModifier) object),
                    handler -> Result.of((input, data) -> {
                        try {
                            return (String) handler.invoke(null, input, data);
                        } catch (Exception e) {
                            Proximity.LOG.error(ExceptionUtil.getErrorMessage(e));
                            return null;
                        }
                    })
            );
        }
    }
}
