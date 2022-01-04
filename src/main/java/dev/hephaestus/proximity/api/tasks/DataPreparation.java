package dev.hephaestus.proximity.api.tasks;

import dev.hephaestus.proximity.Proximity;
import dev.hephaestus.proximity.api.TaskScheduler;
import dev.hephaestus.proximity.api.json.JsonObject;
import dev.hephaestus.proximity.api.DataSet;
import dev.hephaestus.proximity.plugins.TaskDefinition;
import dev.hephaestus.proximity.plugins.util.TaskParser;
import dev.hephaestus.proximity.util.ExceptionUtil;
import dev.hephaestus.proximity.util.Result;
import org.w3c.dom.Element;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

public interface DataPreparation {
    TaskDefinition<DataPreparation, Void> DEFINITION = new DataPreparation.Definition();

    void apply(TaskScheduler scheduler, DataSet cards, JsonObject overrides);

    final class Definition extends TaskDefinition.Void<DataPreparation> {
        private Definition() {
            super("DataPreparation");
        }

        @Override
        public DataPreparation from(Function<Object[], Object> fn) {
            return (scheduler, cards, overrides) -> fn.apply(new Object[] { scheduler, cards, overrides });
        }

        @Override
        public Result<DataPreparation> parse(ClassLoader classLoader, Element element) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
            return TaskParser.parseHandler(classLoader, element).join(
                    object -> Result.of((DataPreparation) object),
                    handler -> Result.of((scheduler, cards, overrides) -> {
                        try {
                            handler.invoke(null, scheduler, cards, overrides);
                        } catch (Exception e) {
                            Proximity.LOG.error(ExceptionUtil.getErrorMessage(e));
                        }
                    })
            );
        }
    }
}