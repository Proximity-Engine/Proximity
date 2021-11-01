package dev.hephaestus.proximity.plugins.util;

import dev.hephaestus.proximity.util.Either;
import dev.hephaestus.proximity.util.Result;
import org.w3c.dom.Element;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public interface TaskParser<T> {
    Result<T> parse(ClassLoader classLoader, Element element) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException;

    @SuppressWarnings("unchecked")
    static <T> Either<T, Method> parseHandler(ClassLoader classLoader, Element element) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String location = element.getAttribute("location");
        String className = location.contains("::") ? location.substring(0, location.indexOf("::")) : location;
        String methodName = location.contains("::") ? location.substring(location.indexOf("::") + 2) : location;
        Class<?> handlerClass = classLoader.loadClass(className);

        return location.contains("::")
                ? Either.right(getMethod(handlerClass, methodName))
                : Either.left((T) handlerClass.getDeclaredConstructor().newInstance());
    }

    static Method getMethod(Class<?> clazz, String name) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }

        return null;
    }
}
