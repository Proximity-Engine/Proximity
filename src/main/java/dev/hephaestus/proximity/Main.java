package dev.hephaestus.proximity;


import dev.hephaestus.proximity.cards.CardPrototype;
import dev.hephaestus.proximity.json.JsonElement;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.json.JsonPrimitive;
import dev.hephaestus.proximity.templates.FileSystemTemplateLoader;
import dev.hephaestus.proximity.templates.TemplateLoader;
import dev.hephaestus.proximity.templates.TemplateSource;
import dev.hephaestus.proximity.util.Pair;
import dev.hephaestus.proximity.util.ParsingUtil;
import dev.hephaestus.proximity.util.Result;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static Pattern LINE = Pattern.compile("^(?:(?<count>\\d[xX]?) )?(?<name>.+?)(?: \\((?<set>.+)\\)(?: (?<collector>[a-zA-Z0-9]+?))?)?(?: (?<options>--.+))*?$");

    public static void main(String[] argArray) {
        Pair<JsonObject, JsonObject> args = parseArgs(argArray);

        JsonObject options = args.left();
        JsonObject overrides = args.right();

        if (args.left().has("debug") && args.left().get("debug").getAsBoolean()) {
            Configurator.setLevel(System.getProperty("log4j.logger"), Level.DEBUG);
        }

        Result<Deque<CardPrototype>> prototypes = getDefaultTemplateName(options).then(defaultTemplateName ->
                loadCardsFromFile(options, overrides, defaultTemplateName, new FileSystemTemplateLoader(Path.of("templates")))
        );

        Proximity proximity = new Proximity(options);

        prototypes.ifPresent(proximity::run)
                .ifError(e -> Proximity.LOG.error(e));
    }

    private static Result<Deque<CardPrototype>> loadCardsFromFile(JsonObject options, JsonObject overrides, String defaultTemplate, TemplateLoader... templateLoaders) {
        Deque<CardPrototype> result = new ArrayDeque<>();

        Path path = Path.of(options.getAsString("cards"));

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            int cardNumber = 1;

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                Matcher matcher = LINE.matcher(line);

                if (matcher.matches()) {
                    JsonObject cardOptions = options.deepCopy();
                    JsonObject cardOverrides = overrides.deepCopy();
                    String cardName = parseCardName(matcher.group("name"));

                    if (matcher.group("count") != null) {
                        String count = matcher.group("count");
                        cardOptions.addProperty("count", Integer.decode(count.endsWith("x") ? count.substring(0, count.length() - 1) : count));
                    } else {
                        cardOptions.addProperty("count", 1);
                    }

                    if (matcher.group("set") != null) {
                        cardOptions.addProperty("set_code", matcher.group("set"));
                    }

                    if (matcher.group("collector") != null) {
                        cardOptions.addProperty("collector_number", matcher.group("collector"));
                    }

                    if (matcher.group("options") != null) {
                        String overridesString = matcher.group("options");

                        String[] overridesArray = overridesString.substring(2).split(" --");

                        for (String override : overridesArray) {
                            String[] split = override.split("=", 2);
                            String key = split[0];
                            String value = split.length == 2 ? split[1] : null;

                            if (key.equals("override")) {
                                if (value != null) {
                                    split = value.split(":", 2);
                                    String[] overrideKey = split[0].split("\\.");
                                    value = split.length == 2 ? split[1] : null;

                                    if (value != null && value.length() > 1 && value.startsWith("\"") && value.endsWith("\"")) {
                                        value = value.substring(1, value.length() - 1);
                                    }

                                    Result<JsonElement> v = ParsingUtil.parseStringValue(value);

                                    if (v.isOk()) {
                                        cardOverrides.add(overrideKey, v.get());
                                    } else {
                                        Proximity.LOG.warn("Error parsing value:\n\t{}", v.getError());
                                    }
                                }
                            } else {
                                Result<JsonElement> v = ParsingUtil.parseStringValue(value);

                                if (v.isOk()) {
                                    cardOptions.add(key, v.get());
                                } else {
                                    Proximity.LOG.warn("Error parsing value:\n\t{}", v.getError());
                                }
                            }
                        }
                    }

                    String template = defaultTemplate;

                    if (cardOptions.has("template")) {
                        template = cardOptions.getAsString("template");
                    }

                    TemplateSource source = null;

                    for (TemplateLoader loader : templateLoaders) {
                        Result<TemplateSource> r = loader.getTemplateFiles(template);

                        if (r.isError()) {
                            return r.unwrap();
                        } else {
                            source = r.get();
                            break;
                        }
                    }

                    result.add(new CardPrototype(cardName, cardNumber, cardOptions, source, cardOverrides));
                    cardNumber += cardOptions.getAsInt("count");
                } else if (!line.trim().isEmpty()) {
                    return Result.error("Could not parse line '%s'", line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Result.of(result);
    }

    private static String parseCardName(String line) {
        return line.contains("(") // if the line contains a set code, ignore that
                ? line.substring(0, line.indexOf('(') - 1)
                : line.contains("--") // only extend to the beginning of the option flags
                ? line.substring(0, line.indexOf("--") - 1)
                : line;
    }

    private static Result<String> getDefaultTemplateName(JsonObject options) {
        if (!options.has("template")) {
            return Result.error("Default template not provided");
        }

        if (!(options.get("template") instanceof JsonPrimitive primitive) || !primitive.isString()) {
            return Result.error(String.format("Default template must be a string. Found: %s", options.get("template")));
        }

        return Result.of(options.getAsString("template"));
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

                        Result<JsonElement> v = ParsingUtil.parseStringValue(value);

                        if (v.isOk()) {
                            overrides.add(key, v.get());
                        } else {
                            Proximity.LOG.warn("Error parsing value:\n\t{}", v.getError());
                        }
                    } else {
                        Result<JsonElement> v = ParsingUtil.parseStringValue(value);

                        if (v.isOk()) {
                            options.add(key, v.get());
                        } else {
                            Proximity.LOG.warn("Error parsing value:\n\t{}", v.getError());
                        }
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
