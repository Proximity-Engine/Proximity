package dev.hephaestus.proximity.app.impl.plugins;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import dev.hephaestus.proximity.app.impl.exceptions.PluginInstantiationException;
import dev.hephaestus.proximity.json.api.Json;
import dev.hephaestus.proximity.json.api.JsonArray;
import dev.hephaestus.proximity.json.api.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;

public final class Plugin {
    private final String id;
    private final ListMultimap<String, Object> entrypoints = LinkedListMultimap.create();

    private Plugin(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public <T> Iterator<T> getEntrypoints(String key) {
        // We make a new Iterator so that callers can't remove members
        return new Iterator<>() {
            final Iterator<Object> itr = Plugin.this.entrypoints.get(key).iterator();

            @Override
            public boolean hasNext() {
                return this.itr.hasNext();
            }

            @Override
            public T next() {
                //noinspection unchecked
                return (T) this.itr.next();
            }
        };
    }

    public static Plugin fromJar(URL url) throws PluginInstantiationException {
        ClassLoader classLoader = new URLClassLoader(new URL[] { url });

        try (InputStream is = classLoader.getResourceAsStream("manifest.json5")) {
            if (is == null) {
                throw new PluginInstantiationException("Jar '%s' does not contain a plugin manifest file.");
            }

            JsonObject manifest = Json.parseObject(is);

            return load(manifest, classLoader);
        } catch (IOException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            // TODO: Error logging and such
            throw new PluginInstantiationException(e);
        }
    }

    /**
     * @param inputStream an InputStream for a manifest.json5 file somewhere on the class path
     * @return a new plugin
     */
    public static Plugin fromManifest(InputStream inputStream) throws PluginInstantiationException {
        try {
            JsonObject manifest = Json.parseObject(inputStream);

            return load(manifest, Plugin.class.getClassLoader());
        } catch (IOException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            // TODO: Error logging and such
            throw new PluginInstantiationException(e);
        }
    }

    private static Plugin load(JsonObject manifest, ClassLoader classLoader) throws PluginInstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (!manifest.has("id")) {
            throw new PluginInstantiationException("Plugin '%s' does not have a valid ID");
        }

        String id = manifest.getString("id");

        Plugin plugin = new Plugin(id);

        if (manifest.has("entrypoints")) {
            for (var entry : manifest.getObject("entrypoints")) {
                if (entry.getValue() instanceof JsonArray array) {
                    for (var entrypoint : array) {
                        plugin.entrypoints.put(entry.getKey(), classLoader.loadClass(entrypoint.asString()).getConstructor().newInstance());
                    }
                } else if (entry.getValue().isString()) {
                    plugin.entrypoints.put(entry.getKey(), classLoader.loadClass(entry.getValue().asString()).getConstructor().newInstance());
                } else {
                    throw new PluginInstantiationException("Unexpected value for entrypoint '%s'. Entrypoint must be a string or array of strings.", entry.getKey());
                }
            }
        }

        return plugin;

    }
}