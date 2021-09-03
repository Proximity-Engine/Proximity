package dev.hephaestus.deckbuilder;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import dev.hephaestus.deckbuilder.cards.*;
import dev.hephaestus.deckbuilder.templates.Template;
import dev.hephaestus.deckbuilder.text.Symbol;

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
import java.util.concurrent.atomic.AtomicInteger;
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
    public static void main(String[] argArray) throws IOException {
        Map<String, String> args = parseArgs(argArray);

        if (!args.containsKey("cards")) {
            System.out.println("Please provide a card list.");
            return;
        }

        ImageCache cache = new ImageCache();

        // TODO: Allow loading templates from external zip files
        InputStream templateStream = Main.class.getResourceAsStream("/template.json5");

        if (templateStream == null) {
            System.out.println("template.json not found");
            return;
        }

        JsonReader templateReader = new JsonReader(new InputStreamReader(templateStream));
        templateReader.setLenient(true);

        Template.Parser parser = new Template.Parser(args);
        Template template = parser.parse(JsonParser.parseReader(templateReader).getAsJsonObject(), cache);

        Map<String, Pair<Integer, Card>> cards = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(args.get("cards")))) {
            Pattern pattern = Pattern.compile("([0-9]+)x (.+)");

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                Matcher matcher = pattern.matcher(line);

                if (matcher.matches()) {
                    int count = matcher.groupCount() == 2 ? Integer.parseInt(matcher.group(1)) : 1;
                    String cardName = matcher.group(2);

                    cards.put(cardName.contains("(") ? cardName.substring(0, cardName.indexOf('(') - 1) : cardName, new Pair<>(count, null));
                } else {
                    System.out.printf("Could not parse line '%s'%n", line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Deque<String> cardsList = new ArrayDeque<>(cards.keySet());

        long lastRequest = 0;

        while (!cardsList.isEmpty()) {
            List<String> cycleCards = new ArrayList<>();

            for (int i = 0; i < 5 && !cardsList.isEmpty(); ++i) {
                cycleCards.add(cardsList.pop());
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
                    String cardName = object.get("name").getAsString();

                    cards.computeIfPresent(cardName, (k, v) -> {
                        Card card;

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

                        if (typeLine.contains("Creature")) {
                            card = new Creature(template, cardName, colors, typeLine, types,image, object.get("mana_cost").getAsString(), object.get("power").getAsString(), object.get("toughness").getAsString(), text);
                        } else if (typeLine.contains("Land")) {
                            card = new Card(template, typeLine, colors, image, types, text, cardName);
                        } else {
                            card = new Spell(template, cardName, colors, typeLine, types,image, object.get("mana_cost").getAsString(), text);
                        }

                        return new Pair<>(v.left(), card);
                    });

                    if (!cards.containsKey(cardName)) {
                        System.out.printf("Received unknown card '%s'%n", cardName);
                    }
                }
            } catch (IOException | InterruptedException exception) {
                exception.printStackTrace();
            }
        }

        AtomicInteger progress = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(10);

        System.out.printf("|%100s|   0%c\r", "=".repeat(0), '%');

        Deque<String> finishedCards = new ConcurrentLinkedDeque<>();

        for (Map.Entry<String, Pair<Integer, Card>> entry : cards.entrySet()) {
            Card card = entry.getValue().right();
//            executor.submit(() -> {
                BufferedImage out = new BufferedImage(3288, 4488, BufferedImage.TYPE_INT_ARGB);

                template.draw(card, out);

                Path path = Path.of("images", entry.getKey().replaceAll("[^a-zA-Z0-9.\\-, ]", "_") + ".png");

                try {
                    if (!Files.isDirectory(path.getParent())) {
                        Files.createDirectories(path.getParent());
                    }

                    OutputStream stream = Files.newOutputStream(path);
                    ImageIO.write(out, "png", stream);
                    stream.close();

                    finishedCards.add(card.name());

                    System.out.printf("Saved '%s'%n", card.name());
                } catch (IOException exception) {
                    System.out.printf("Failed to save '%s'%n", card.name());
                }

                int p = (int) (100 * ((float) progress.getAndIncrement()) / cards.size());

                System.out.printf("|%s| %3d%c\r", "█".repeat(p) + " ".repeat(100 - p), p, '%');
//            });
        }

        try {
            executor.shutdown();
            executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.printf("|%s| 100%c\r", "█".repeat(100), '%');
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

    static JsonArray getCardInfo(Collection<String> cardNames) throws IOException, InterruptedException {
        JsonArray identifiers = new JsonArray();

        for (String cardName : cardNames) {
            JsonObject object = new JsonObject();

            object.add("name", new JsonPrimitive(cardName));

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
