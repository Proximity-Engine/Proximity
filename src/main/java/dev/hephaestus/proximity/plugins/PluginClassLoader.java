package dev.hephaestus.proximity.plugins;

import java.net.URL;
import java.net.URLClassLoader;

public final class PluginClassLoader extends URLClassLoader {
    public PluginClassLoader(URL url) {
        super(new URL[] { url }, PluginClassLoader.class.getClassLoader());
    }
}
