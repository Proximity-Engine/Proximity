package dev.hephaestus.proximity.api.tasks;

import dev.hephaestus.proximity.Proximity;
import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.plugins.TaskDefinition;
import dev.hephaestus.proximity.plugins.util.TaskParser;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.xml.RenderableData;
import org.w3c.dom.Element;

import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

public interface Effect {
    TaskDefinition<Effect, Void> DEFINITION = new Effect.Definition();

    void apply(JsonObject card, BufferedImage image, RenderableData.XMLElement element);

    final class Definition extends TaskDefinition.Void<Effect> {
        public Definition() {
            super("Effect");
        }

        @Override
        public Effect from(Function<Object[], Object> fn) {
            return (card, image, element) -> fn.apply(new Object[]{ card, image, element });
        }

        @Override
        public Result<Effect> parse(ClassLoader classLoader, Element element) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
            return TaskParser.parseHandler(classLoader, element).join(
                    object -> Result.of((Effect) object),
                    handler -> Result.of((card, image, effect) -> {
                        try {
                            handler.invoke(null, card, image, effect);
                        } catch (Exception e) {
                            Throwable t = e;

                            if (e.getMessage() == null && e.getCause() != null) {
                                t = e.getCause();
                            }

                            Proximity.LOG.error(t.getMessage());
                        }
                    })
            );
        }
    }
}
