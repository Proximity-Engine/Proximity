package dev.hephaestus.proximity;


import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.templates.FileSystemTemplateLoader;
import dev.hephaestus.proximity.util.Pair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.nio.file.Path;

import static dev.hephaestus.proximity.util.ParsingUtil.parseStringValue;

public class Main {
    public static void main(String[] argArray) {
        Pair<JsonObject, JsonObject> args = parseArgs(argArray);

        if (args.left().has("debug") && args.left().get("debug").getAsBoolean()) {
            Configurator.setLevel(System.getProperty("log4j.logger"), Level.DEBUG);
        }

        Proximity proximity = new Proximity(args.left(), args.right(), new FileSystemTemplateLoader(Path.of("templates")));

        proximity.run();
    }

    private static Pair<JsonObject, JsonObject> parseArgs(String[] argArray) {
        JsonObject options = new JsonObject();
        JsonObject overrides = new JsonObject();

        for (String arg : argArray) {
            if (arg.startsWith("--")) {
                String[] split = arg.substring(2).split("=", 2);

                if (split.length == 1) {
                    options.addProperty(split[0], true);
                } else {
                    String key = split[0];
                    String value = split[1];

                    if (key.equals("override")) {
                        if (value != null) {
                            split = value.split(":", 2);
                            key = split[0];
                            value = split.length == 2 ? split[1] : null;
                        }

                        overrides.add(key, parseStringValue(value));
                    } else {
                        options.add(key, parseStringValue(value));
                    }

                    if (split[1].equalsIgnoreCase("true") || split[1].equalsIgnoreCase("false")) {
                        options.addProperty(split[0], Boolean.parseBoolean(split[1]));
                    } else {
                        options.addProperty(split[0], split[1]);
                    }
                }
            }
        }

        return new Pair<>(options, overrides);
    }
}
