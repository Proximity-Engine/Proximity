package dev.hephaestus.proximity;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import dev.hephaestus.proximity.cards.*;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.text.Symbol;
import dev.hephaestus.proximity.util.Option;
import dev.hephaestus.proximity.util.OptionContainer;
import dev.hephaestus.proximity.util.Result;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static final Map<String, Symbol> LAND_TYPES = new HashMap<>();

    static {
        LAND_TYPES.put("Plains", Symbol.of("W"));
        LAND_TYPES.put("Island", Symbol.of("U"));
        LAND_TYPES.put("Swamp", Symbol.of("B"));
        LAND_TYPES.put("Mountain", Symbol.of("R"));
        LAND_TYPES.put("Forest", Symbol.of("G"));
    }

    private static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();

    // TODO: Break this method up
    public static void main(String[] argArray) {
        Map<String, String> args = parseArgs(argArray);

        if (!args.containsKey("cards")) {
            System.out.println("Please provide a card list.");
            return;
        }

        ImageCache cache = new ImageCache();

        Result<Template> templateResult = parseTemplate(args, cache);

        if (templateResult.isError()) {
            System.out.println(templateResult.getError());
            return;
        }

        Template template = templateResult.get();

        Deque<Card.Prototype> cardPrototypes = new ArrayDeque<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(args.get("cards")))) {
            Pattern pattern = Pattern.compile("([0-9]+)x (.+)");

            int number = 1;

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                Matcher matcher = pattern.matcher(line);

                if (matcher.matches()) {
                    int count = matcher.groupCount() == 2 ? Integer.parseInt(matcher.group(1)) : 1;
                    String cardName = matcher.group(2);

                    cardName = cardName.contains("(")
                            ? cardName.substring(0, cardName.indexOf('(') - 1)
                            : cardName.contains("--")
                                    ? cardName.substring(0, cardName.indexOf("--") - 1)
                                    : cardName;

                    Map<String, Object> options = new HashMap<>();

                    options.put(Option.COUNT, count);

                    if (line.contains("(") && line.contains(")")) {
                        options.put(Option.SET_CODE, line.substring(line.indexOf("(") + 1, line.indexOf(")")).toUpperCase(Locale.ROOT));
                    }

                    int i = line.indexOf("--");

                    while (i >= 0) {
                        int j;

                        for (j = i; j < line.length() && !Character.isWhitespace(line.charAt(j)); ++j) {
                            char c = line.charAt(j);

                            if (c == '"') {
                                do {
                                    ++j;
                                } while (j < line.length() && line.charAt(j) != '"');
                            }
                        }

                        String[] split = line.substring(i + 2, j).split("=", 2);
                        String value = split.length == 2 ? split[1] : null;

                        if (value != null && value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }

                        options.put(split[0], value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false") ? Boolean.parseBoolean(value) : value);

                        if (j < line.length()) {
                            String sub = line.substring(j + 1);
                            i = j + 1 + sub.indexOf("--");
                        } else {
                            i = -1;
                        }
                    }

                    cardPrototypes.add(new Card.Prototype(cardName, number, new OptionContainer.Implementation(template, options)));

                    number += count;
                } else {
                    System.out.printf("Could not parse line '%s'%n", line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Card> cards = new ArrayList<>();

        long lastRequest = 0;

        long runTime = System.currentTimeMillis();
        System.out.printf("Getting card info for %d cards...%n", cardPrototypes.size());

        Map<String, Map<String, JsonObject>> cardInfo = new HashMap<>();

        Deque<Card.Prototype> prototypesToFetch = new ArrayDeque<>(cardPrototypes);

        while (!prototypesToFetch.isEmpty()) {
            List<Card.Prototype> cycleCards = new ArrayList<>();

            for (int i = 0; i < 70 && !prototypesToFetch.isEmpty(); ++i) {
                cycleCards.add(prototypesToFetch.pop());
            }

            long time = System.currentTimeMillis();

            // We respect Scryfall's wishes and wait between 50-100 seconds between requests.
            if (time - lastRequest < 69) {
                try {
                    System.out.printf("Sleeping for %dms%n", time - lastRequest);
                    Thread.sleep(time - lastRequest);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            lastRequest = System.currentTimeMillis();

            try {
                for (JsonElement cardJson : getCardInfo(cycleCards)) {
                    JsonObject object = cardJson.getAsJsonObject();
                    cardInfo.computeIfAbsent(object.get("name").getAsString(), k -> new HashMap<>())
                            .put(object.get("set").getAsString().toUpperCase(Locale.ROOT), object);
                }
            } catch (IOException | InterruptedException exception) {
                exception.printStackTrace();
            }
        }

        for (Card.Prototype prototype : cardPrototypes) {
            if (!cardInfo.containsKey(prototype.name())) continue;

            String setCode = prototype.options().getOption(Option.SET_CODE);

            if (setCode == null) {
                if (cardInfo.get(prototype.name()).isEmpty() || cardInfo.get(prototype.name()).size() > 1) {
                    throw new RuntimeException();
                }

                setCode = cardInfo.get(prototype.name()).keySet().iterator().next();
            }

            if (!cardInfo.containsKey(prototype.name()) || !cardInfo.get(prototype.name()).containsKey(setCode)) {
                System.out.printf("Failed to find card '%s' in set '%s'.%n", prototype.name(), setCode);
                continue;
            }

            JsonObject object = cardInfo.get(prototype.name()).get(setCode);
            String typeLine = object.get("type_line").getAsString();
            String oracle = object.get("oracle_text").getAsString();

            Set<Symbol> colors = new HashSet<>();

            for (JsonElement element : object.get(typeLine.contains("Land") ? "color_identity" : "colors").getAsJsonArray()) {
                colors.add(Symbol.of(element.getAsString()));
            }

            if (typeLine.contains("Land")) {
                if (object.has("produced_mana")) {
                    for (JsonElement element : object.get("produced_mana").getAsJsonArray()) {
                        colors.add(Symbol.of(element.getAsString()));
                    }
                }

                Set<Symbol> landColors = new HashSet<>();

                for (var entry : LAND_TYPES.entrySet()) {
                    if (oracle.contains(entry.getKey())) landColors.add(entry.getValue());
                }

                if (landColors.size() < 3) {
                    colors.addAll(landColors);
                }
            }

            Collection<String> typeStrings = new LinkedList<>();

            for (String string : typeLine.split("—")[0].split(" ")) {
                typeStrings.add(string.toLowerCase(Locale.ROOT));
            }

            TypeContainer types = new TypeContainer(typeStrings);

            // TODO: Allow users to supply images in a folder
            URL image = null;

            try {
                image = object.has("image_uris") && object.getAsJsonObject("image_uris").has("art_crop")
                        ? new URL(object.getAsJsonObject("image_uris").get("art_crop").getAsString())
                        : null;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            OracleText text = new OracleParser(oracle, template).parse();

            Card card;

            Map<String, Object> optionMap = prototype.options().getMap();

            if ((boolean) prototype.getOption(Option.USE_OFFICIAL_ART)) {
                optionMap.put(Option.ARTIST, object.get("artist").getAsString());
            }

            OptionContainer options = new OptionContainer.Implementation(template, optionMap);

            Set<String> frameEffects = new HashSet<>();

            if (object.has("frame_effects")) {
                for (JsonElement effect : object.get("frame_effects").getAsJsonArray()) {
                    frameEffects.add(effect.getAsString());
                }
            }

            if (typeLine.contains("Creature") || typeLine.contains("Vehicle")) {
                card = new Creature(prototype.number(), prototype.name(), template, colors, typeLine, types,image, object.get("mana_cost").getAsString(), object.get("power").getAsString(), object.get("toughness").getAsString(), text, options, frameEffects);
            } else if (typeLine.contains("Land")) {
                card = new Card(prototype.number(), prototype.name(), colors, image, types, text, typeLine, options, frameEffects);
            } else {
                card = new Spell(prototype.number(), prototype.name(), template, colors, typeLine, types,image, object.get("mana_cost").getAsString(), text, options, frameEffects);
            }

            cards.add(card);
        }

        System.out.printf("Done! Took %dms%n", System.currentTimeMillis() - runTime);

        Deque<String> finishedCards = new ConcurrentLinkedDeque<>();

        runTime = System.currentTimeMillis();

        int cardCount = cards.stream().map(card -> (int) card.getOption(Option.COUNT)).reduce(0, Integer::sum);
        int countStrLen = Integer.toString(cardCount).length();

        int threadCount = Integer.min(args.containsKey("threads") ? Integer.parseInt(args.get("threads")) : 10, cardCount);

        System.out.printf("Rendering %d cards on %d threads...%n", cardCount, threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (Card card : cards) {
            executor.submit(() -> {
                long cardTime = System.currentTimeMillis();
                BufferedImage out = new BufferedImage(3288, 4488, BufferedImage.TYPE_INT_ARGB);

                template.draw(card, out);

                for (int i = 0; i < (int) card.getOption(Option.COUNT); ++i) {
                    Path path = Path.of("images", card.number() + i + " " + card.name().replaceAll("[^a-zA-Z0-9.\\-, ]", "_") + ".png");

                    try {
                        if (!Files.isDirectory(path.getParent())) {
                            Files.createDirectories(path.getParent());
                        }

                        OutputStream stream = Files.newOutputStream(path);

                        ImageIO.write(out, "png", stream);
                        stream.close();
                        finishedCards.add(card.name());
                        System.out.printf("%" + countStrLen + "d/%" + countStrLen + "d  %5dms  %-45s \u001B[32mSAVED\u001B[0m%n", finishedCards.size(), cardCount, System.currentTimeMillis() - cardTime, card.name());
                    } catch (Throwable throwable) {
                        finishedCards.add(card.name());
                        System.out.printf("%" + countStrLen + "d/%" + countStrLen + "d  %5dms  %-45s \u001B[31mFAILED\u001B[0m%n", finishedCards.size(), cardCount, System.currentTimeMillis() - cardTime, card.name());
                        System.out.println(throwable.getMessage());
                    }

                    cardTime = System.currentTimeMillis();
                }
            });
        }

        try {
            executor.shutdown();

            while(!executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.NANOSECONDS)) {

            }

            System.out.printf("Done! Took %dms%n", System.currentTimeMillis() - runTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> parseArgs(String[] argArray) {
        Map<String, String> args = new HashMap<>();

        for (String arg : argArray) {
            if (arg.startsWith("--")) {
                String[] split = arg.substring(2).split("=", 2);
                args.put(split[0], split.length == 2 ? split[1] : null);
            }
        }

        return args;
    }

    // TODO: Allow loading templates from external zip files
    private static Result<Template> parseTemplate(Map<String, String> args, ImageCache cache) {
        InputStream templateStream = Main.class.getResourceAsStream("/template.json5");

        if (templateStream == null) {
            return Result.error("template.json not found");
        }

        JsonReader templateReader = new JsonReader(new InputStreamReader(templateStream));
        templateReader.setLenient(true);

        Template.Parser parser = new Template.Parser(args);
        return Result.of(parser.parse(JsonParser.parseReader(templateReader).getAsJsonObject(), cache));
    }

    static JsonArray getCardInfo(Collection<Card.Prototype> cards) throws IOException, InterruptedException {
        JsonArray identifiers = new JsonArray();

        for (Card.Prototype prototype : cards) {
            JsonObject object = new JsonObject();

            object.add("name", new JsonPrimitive(prototype.name()));

            if (prototype.getOption(Option.SET_CODE) != null) {
                object.add("set", new JsonPrimitive((String) prototype.getOption(Option.SET_CODE)));
            }

            identifiers.add(object);
        }

        JsonObject object = new JsonObject();
        object.add("identifiers", identifiers);

        String body = GSON.toJson(object);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.scryfall.com/cards/collection"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();

        JsonObject response = GSON.fromJson(client.send(request, HttpResponse.BodyHandlers.ofString()).body(), JsonObject.class);

        for (JsonElement element : response.getAsJsonArray("not_found")) {
            System.out.printf("Could not find '%s'%n", element.getAsJsonObject().get("name"));
        }

        return response.getAsJsonArray("data");
    }
}
