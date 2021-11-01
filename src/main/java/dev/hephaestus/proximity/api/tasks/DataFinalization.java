package dev.hephaestus.proximity.api.tasks;

import dev.hephaestus.proximity.Proximity;
import dev.hephaestus.proximity.plugins.TaskDefinition;
import dev.hephaestus.proximity.plugins.util.TaskParser;
import dev.hephaestus.proximity.util.Result;
import org.w3c.dom.Element;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

public interface DataFinalization {
    TaskDefinition<DataFinalization, Void> DEFINITION = new DataFinalization.Definition();

    void apply();

    final class Definition extends TaskDefinition.Void<DataFinalization> {
        private Definition() {
            super("DataFinalization");
        }

        @Override
        public DataFinalization from(Function<Object[], Object> fn) {
            return () -> fn.apply(new Object[0]);
        }

        @Override
        public Result<DataFinalization> parse(ClassLoader classLoader, Element element) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
            return TaskParser.parseHandler(classLoader, element).join(
                    object -> Result.of((DataFinalization) object),
                    handler -> Result.of(() -> {
                        try {
                            handler.invoke(null);
                        } catch (Exception e) {
                            Proximity.LOG.error(e.getMessage());
                        }
                    })
            );
        }
    }
}