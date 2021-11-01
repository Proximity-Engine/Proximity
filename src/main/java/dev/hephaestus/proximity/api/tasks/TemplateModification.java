package dev.hephaestus.proximity.api.tasks;

import dev.hephaestus.proximity.Proximity;
import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.plugins.TaskDefinition;
import dev.hephaestus.proximity.plugins.util.TaskParser;
import dev.hephaestus.proximity.util.Result;
import dev.hephaestus.proximity.xml.RenderableData;
import org.w3c.dom.Element;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

public interface TemplateModification {
    TaskDefinition<TemplateModification, Void> DEFINITION = new TemplateModification.Definition();
    void apply(JsonObject data, RenderableData.XMLElement layers);

    final class Definition extends TaskDefinition.Void<TemplateModification> {
        public Definition() {
            super("TemplateModification");
        }

        @Override
        public TemplateModification from(Function<Object[], Object> fn) {
            return (data, layers) -> fn.apply(new Object[] { data, layers });
        }

        @Override
        public Result<TemplateModification> parse(ClassLoader classLoader, Element element) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
            return TaskParser.parseHandler(classLoader, element).join(
                    object -> Result.of((TemplateModification) object),
                    handler -> Result.of((data, layers) -> {
                        try {
                            handler.invoke(null, data, layers);
                        } catch (Exception e) {
                            Proximity.LOG.error(e.getMessage());
                        }
                    })
            );
        }
    }
}
