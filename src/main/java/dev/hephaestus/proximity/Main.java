package dev.hephaestus.proximity;


import dev.hephaestus.proximity.cards.CardPrototype;
import dev.hephaestus.proximity.json.JsonArray;
import dev.hephaestus.proximity.json.JsonElement;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.json.JsonPrimitive;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.util.Keys;
import dev.hephaestus.proximity.util.Result;
import org.quiltmc.json5.JsonReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    // TODO: Break this method up
    public static void main(String[] argArray) throws IOException {
        long startTime = System.currentTimeMillis();

        Map<String, String> args = parseArgs(argArray);

        if (!args.containsKey("cards")) {
            System.out.println("Please provide a card list.");
            return;
        }

        if (!args.containsKey("template")) {
            System.out.println("Please provide a template.");
            return;
        }

        TemplateFiles cache = new TemplateFiles.Implementation(Path.of("templates", args.get("template")));

        Result<Template> templateResult = parseTemplate(args, cache);

        if (templateResult.isError()) {
            System.out.println(templateResult.getError());
            return;
        }

        Template template = templateResult.get();

        Deque<CardPrototype> cardPrototypes = new ArrayDeque<>();

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

                    String scryfallName = cardName.contains("//")
                            ? cardName.substring(0, cardName.indexOf("//") - 1)
                            : cardName;

                    JsonObject options = template.getOptions().deepCopy();

                    options.addProperty("count", count);

                    if (line.contains("(") && line.contains(")")) {
                        options.addProperty("set_code", line.substring(line.indexOf("(") + 1, line.indexOf(")")).toUpperCase(Locale.ROOT));
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

                        if (value != null) {
                            if (value.startsWith("\"") && value.endsWith("\"")) {
                                value = value.substring(1, value.length() - 1);
                            }

                            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                                options.addProperty(split[0], Boolean.parseBoolean(value));
                            } else {
                                options.addProperty(split[0], value);
                            }
                        }

                        if (j < line.length()) {
                            String sub = line.substring(j + 1);
                            i = j + 1 + sub.indexOf("--");
                        } else {
                            i = -1;
                        }
                    }

                    cardPrototypes.add(new CardPrototype(scryfallName, cardName, number++, options));
                } else {
                    System.out.printf("Could not parse line '%s'%n", line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<JsonObject> cards = new ArrayList<>();

        long lastRequest = 0;

        long runTime = System.currentTimeMillis();
        System.out.printf("Getting card info for %d cards...%n", cardPrototypes.size());

        Map<String, Map<String, JsonObject>> cardInfo = new HashMap<>();

        Deque<CardPrototype> prototypesToFetch = new ArrayDeque<>(cardPrototypes);

        while (!prototypesToFetch.isEmpty()) {
            List<CardPrototype> cycleCards = new ArrayList<>();

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
                    cardInfo.computeIfAbsent(object.getAsString("name"), k -> new HashMap<>())
                            .put(object.getAsString("set").toUpperCase(Locale.ROOT), object);
                }
            } catch (IOException | InterruptedException exception) {
                exception.printStackTrace();
            }
        }

        for (CardPrototype prototype : cardPrototypes) {
            if (!cardInfo.containsKey(prototype.cardName())) continue;

            String setCode = prototype.options().has(Keys.SET_CODE) ?
                    prototype.options().getAsString(Keys.SET_CODE)
                    : null;

            if (setCode == null) {
                if (cardInfo.get(prototype.cardName()).isEmpty() || cardInfo.get(prototype.cardName()).size() > 1) {
                    throw new RuntimeException();
                }

                setCode = cardInfo.get(prototype.cardName()).keySet().iterator().next();
            }

            if (!cardInfo.containsKey(prototype.cardName()) || !cardInfo.get(prototype.cardName()).containsKey(setCode)) {
                System.out.printf("Failed to find card '%s' in set '%s'.%n", prototype.cardName(), setCode);
                continue;
            }

            JsonObject object = cardInfo.get(prototype.cardName()).get(setCode);


            cards.add(prototype.parse(object));
        }

        System.out.printf("Done! Took %dms%n", System.currentTimeMillis() - runTime);

        Deque<String> finishedCards = new ConcurrentLinkedDeque<>();

        runTime = System.currentTimeMillis();

        int cardCount = cards.stream().map(card -> card.getAsInt("proximity", "options", "count"))
                .reduce(0, Integer::sum);

        int countStrLen = Integer.toString(cardCount).length();

        int threadCount = Integer.min(args.containsKey("threads") ? Integer.parseInt(args.get("threads")) : 10, cardCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (JsonObject card : cards) {
            executor.submit(() -> {
                long cardTime = System.currentTimeMillis();
                BufferedImage frontImage = new BufferedImage(3288, 4488, BufferedImage.TYPE_INT_ARGB);
                BufferedImage backImage = new BufferedImage(3288, 4488, BufferedImage.TYPE_INT_ARGB);

                template.draw(card, frontImage);

                if (card.getAsBoolean(Keys.DOUBLE_SIDED)) {
                    template.draw(card.getAsJsonObject(Keys.FLIPPED), backImage);
                }

                for (int i = 0; i < card.getAsInt(Keys.COUNT); ++i) {
                    Path front = Path.of("images", "fronts", card.getAsInt(Keys.CARD_NUMBER) + i + " " + card.getAsString("name").replaceAll("[^a-zA-Z0-9.\\-, ]", "_") + ".png");

                    Path back = Path.of("images", "backs", card.getAsInt(Keys.CARD_NUMBER) + i + " " + (card.getAsBoolean(Keys.DOUBLE_SIDED)
                            ? card.getAsJsonObject(Keys.FLIPPED).getAsString("name")
                            : card.getAsString("name")
                    ).replaceAll("[^a-zA-Z0-9.\\-, ]", "_") + ".png");

                    try {
                        if (!Files.isDirectory(front.getParent())) {
                            Files.createDirectories(front.getParent());
                        }

                        OutputStream stream = Files.newOutputStream(front);

                        ImageIO.write(frontImage, "png", stream);
                        stream.close();

                        if (!Files.isDirectory(back.getParent())) {
                            Files.createDirectories(back.getParent());
                        }

                        if (!card.getAsBoolean(Keys.DOUBLE_SIDED)) {
                            Files.copy(args.containsKey("cardback")
                                    ? Files.newInputStream(Path.of(args.get("cardback")))
                                    : cache.getInputStream("back.png"), back, StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            stream = Files.newOutputStream(back);

                            ImageIO.write(backImage, "png", stream);
                            stream.close();
                        }

                        finishedCards.add(card.getAsString("name"));
                        System.out.printf("%" + countStrLen + "d/%" + countStrLen + "d  %5dms  %-45s \u001B[32mSAVED\u001B[0m%n", finishedCards.size(), cardCount, System.currentTimeMillis() - cardTime, card.getAsString("name"));
                    } catch (Throwable throwable) {
                        finishedCards.add(card.getAsString("name"));
                        System.out.printf("%" + countStrLen + "d/%" + countStrLen + "d  %5dms  %-45s \u001B[31mFAILED\u001B[0m%n", finishedCards.size(), cardCount, System.currentTimeMillis() - cardTime, card.getAsString("name"));
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

            System.out.printf("Done! Took %dms%n", System.currentTimeMillis() - startTime);
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
    private static Result<Template> parseTemplate(Map<String, String> args, TemplateFiles cache) throws IOException {
        InputStream templateStream;

        if (args.get("template").endsWith(".zip")) {
            FileSystem fileSystem = FileSystems.newFileSystem(Path.of("templates", args.get("template")));
            templateStream = Files.newInputStream(fileSystem.getPath("template.json5"));
        } else if (Files.isDirectory(Path.of("templates", args.get("template")))) {
            templateStream = Files.newInputStream(Path.of("templates", args.get("template"), "template.json5"));
        } else {
            throw new RuntimeException("Template must be either a zip file or a directory!");
        }

        Template.Parser parser = new Template.Parser(JsonObject.parseObject(JsonReader.json5(new InputStreamReader(templateStream))), cache, args);
        return Result.of(parser.parse());
    }

    static JsonArray getCardInfo(Collection<CardPrototype> cards) throws IOException, InterruptedException {
        JsonArray identifiers = new JsonArray();

        for (CardPrototype prototype : cards) {
            JsonObject object = new JsonObject();

            object.add("name", new JsonPrimitive(prototype.scryfallName()));

            JsonObject options = prototype.options();

            if (options.has("set_code")) {
                object.add("set", options.get("set_code"));
            }

            identifiers.add(object);
        }

        JsonObject object = new JsonObject();
        object.add("identifiers", identifiers);

        String body = object.toString();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.scryfall.com/cards/collection"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();

        String responseBody = client.send(request, HttpResponse.BodyHandlers.ofString()).body();

        JsonObject response = JsonObject.parseObject(JsonReader.json5(responseBody));

        if (response.has("not_found")) {
            for (JsonElement element : response.getAsJsonArray("not_found")) {
                System.out.printf("Could not find '%s'%n", element.getAsJsonObject().get("cardName"));
            }
        }

        return response.get("data", JsonArray::new);
    }
}
