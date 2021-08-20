package dev.hephaestus.deckbuilder;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import dev.hephaestus.deckbuilder.cards.Card;
import dev.hephaestus.deckbuilder.cards.Color;
import dev.hephaestus.deckbuilder.cards.Creature;
import dev.hephaestus.deckbuilder.cards.TypeContainer;
import dev.hephaestus.deckbuilder.templates.Template;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
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

        Template template = Template.parse(JsonParser.parseReader(templateReader).getAsJsonObject(), cache);

        Map<String, Pair<Integer, Card>> cards = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(args.get("cards")))) {
            Pattern pattern = Pattern.compile("([0-9]+)x (.+)");


            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                Matcher matcher = pattern.matcher(line);

                if (matcher.matches()) {
                    int count = matcher.groupCount() == 2 ? Integer.parseInt(matcher.group(1)) : 1;
                    String cardName = matcher.group(2);

                    cards.put(cardName, new Pair<>(count, null));
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
                for (JsonElement card : getCardInfo(cycleCards)) {
                    JsonObject object = card.getAsJsonObject();
                    String cardName = object.get("name").getAsString();

                    cards.computeIfPresent(cardName, (k, v) -> {
                        Card c;

                        String typeLine = object.get("type_line").getAsString();

                        List<Color> colors = new ArrayList<>();

                        for (JsonElement element : object.get(typeLine.contains("Land") ? "color_identity" : "colors").getAsJsonArray()) {
                            colors.add(Color.of(element.getAsString().charAt(0)));
                        }

                        if (colors.isEmpty()) {
                            colors.add(
                                    typeLine.contains("Land")
                                            ? Color.LAND
                                            : typeLine.contains("Artifact")
                                                ? Color.ARTIFACT
                                                : Color.NONE
                            );
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

                        if (typeLine.contains("Creature")) {
                            c = new Creature(cardName, colors, typeLine, types,image, object.get("mana_cost").getAsString(), object.get("power").getAsString(), object.get("toughness").getAsString());
                        } else {
                            c = new Card(cardName, typeLine, colors, image, types);
                        }

                        return new Pair<>(v.left(), c);
                    });

                    if (!cards.containsKey(cardName)) {
                        System.out.printf("Received unknown card '%s'%n", cardName);
                    }
                }
            } catch (IOException | InterruptedException exception) {
                exception.printStackTrace();
            }
        }

        for (Map.Entry<String, Pair<Integer, Card>> entry : cards.entrySet()) {
            Card card = entry.getValue().right();
            BufferedImage out = new BufferedImage(3288, 4488, BufferedImage.TYPE_INT_ARGB);

            template.draw(card, out);

            Path path = Path.of("images", entry.getKey().replaceAll("[^a-zA-Z0-9.\\-, ]", "_") + ".png");

            if (!Files.isDirectory(path.getParent())) {
                Files.createDirectories(path.getParent());
            }

            ImageIO.write(out, "png", Files.newOutputStream(path));
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
