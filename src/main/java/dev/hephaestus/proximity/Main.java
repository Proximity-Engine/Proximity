package dev.hephaestus.proximity;


import dev.hephaestus.proximity.json.JsonObject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

public class Main {
    public static void main(String[] argArray) {
        JsonObject args = parseArgs(argArray);

        if (args.has("debug")) {
            Configurator.setLevel(System.getProperty("log4j.logger"), Level.DEBUG);
        }

        Proximity proximity = new Proximity(args);

        proximity.run();
    }

    private static JsonObject parseArgs(String[] argArray) {
        JsonObject args = new JsonObject();

        for (String arg : argArray) {
            if (arg.startsWith("--")) {
                String[] split = arg.substring(2).split("=", 2);

                if (split.length == 1) {
                    args.addProperty(split[0], true);
                } else if (split[1].equalsIgnoreCase("true") || split[1].equalsIgnoreCase("false")) {
                    args.addProperty(split[0], Boolean.parseBoolean(split[1]));
                } else {
                    args.addProperty(split[0], split[1]);
                }
            }
        }

        return args;
    }
}
