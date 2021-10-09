package dev.hephaestus.proximity.cards;


import dev.hephaestus.proximity.json.JsonArray;
import dev.hephaestus.proximity.json.JsonElement;
import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.json.JsonPrimitive;
import dev.hephaestus.proximity.templates.TemplateSource;
import dev.hephaestus.proximity.util.Keys;

import java.util.*;

public record CardPrototype(String cardName, int number, JsonObject options, TemplateSource.Compound source, JsonObject overrides) {
    private static final Set<String> MANA_COLORS = new HashSet<>();
    private static final Map<String, String> LAND_TYPES = new WeakHashMap<>();

    static {
        MANA_COLORS.addAll(Arrays.asList("W", "U", "B", "R", "G"));

        LAND_TYPES.put("Plains", "W");
        LAND_TYPES.put("Island", "U");
        LAND_TYPES.put("Swamp", "B");
        LAND_TYPES.put("Mountain", "R");
        LAND_TYPES.put("Forest", "G");
    }

    private static final Set<String> MAIN_TYPES = new HashSet<>() {{
        add("enchantment");
        add("artifact");
        add("land");
        add("creature");
        add("conspiracy");
        add("instant");
        add("phenomenon");
        add("plane");
        add("planeswalker");
        add("scheme");
        add("sorcery");
        add("tribal");
        add("vanguard");
    }};

    public JsonObject parse(JsonObject raw) {
        return raw.has("card_faces")
                ? parseTwoSidedCard(raw)
                : this.process(raw.deepCopy());
    }

    private JsonObject parseTwoSidedCard(JsonObject raw) {
        JsonArray faces = raw.getAsJsonArray("card_faces");
        JsonObject front = parseFace(raw, faces.get(0).getAsJsonObject());
        JsonObject back = parseFace(raw, faces.get(1).getAsJsonObject());

        this.process(front);
        this.process(back);

        back.add(Keys.FLIPPED, front.deepCopy());
        back.add(Keys.FRONT_FACE, false);

        front.add(Keys.FLIPPED, back);
        front.add(Keys.FRONT_FACE, true);

        return front;
    }

    private static JsonObject parseFace(JsonObject raw, JsonObject face) {
        JsonObject result = raw.deepCopy();

        for (var entry : face.entrySet()) {
            result.add(entry.getKey(), entry.getValue().deepCopy());
        }

        return result;
    }

    private JsonObject process(JsonObject card) {
        processTypes(card);
        processColors(card);

        card.add(Keys.CARD_NUMBER, this.number);
        card.add(Keys.DOUBLE_SIDED, card.has("card_faces"));
        card.getAsJsonObject(Keys.OPTIONS).copyAll(this.options);

        if (card.getAsJsonArray("keywords").contains("mutate")) {
            String[] split = card.getAsString("oracle_text").split("\n", 2);
            card.addProperty("oracle_text", split[1]);
            card.add(Keys.MUTATE_TEXT, split[0]);
        }

        card.copyAll(this.overrides);

        return card;
    }

    private static void processTypes(JsonObject card) {
        JsonArray types = card.getAsJsonArray(Keys.TYPES);
        StringBuilder mainTypes = new StringBuilder();
        String typeLine = card.getAsString("type_line");

        Set<String> cardTypes = new HashSet<>();

        if (typeLine.contains("\u2014")) {
            mainTypes.append(typeLine.split("\u2014")[1]);
        }

        for (String string : typeLine.split(" ")) {
            String type = string.toLowerCase(Locale.ROOT);

            if (!type.equals("\u2014")) {
                types.add(type);
            }

            if (MAIN_TYPES.contains(type) && !typeLine.contains("\u2014")) {
                mainTypes.append(string);
            }

            if (MAIN_TYPES.contains(type)) {
                cardTypes.add(type);
            }
        }

        card.add(Keys.MAIN_TYPES, mainTypes.toString());
        card.add(Keys.TYPE_COUNT, cardTypes.size());
    }

    private static void processColors(JsonObject card) {
        Set<JsonElement> colors = new HashSet<>(card.get("colors", JsonArray::new));

        if (card.getAsJsonArray(Keys.TYPES).contains("land")) {
            if (!card.get("layout").getAsString().equals("transform") && !card.get("layout").getAsString().equals("modal_dfc")) {
                for (JsonElement element : card.getAsJsonArray("produced_mana")) {
                    if (MANA_COLORS.contains(element.getAsString())) {
                        colors.add(element);
                    }
                }
            }

            card.getIfPresent("oracle_text")
                    .map(JsonElement::getAsString)
                    .ifPresent(oracle -> {
                        for (var entry : LAND_TYPES.entrySet()) {
                            if (oracle.contains(entry.getKey())) colors.add(new JsonPrimitive(entry.getValue()));
                        }

                        if (oracle.contains("of any color") || oracle.contains("Search your library for a basic land card, put it onto the battlefield") && (card.getAsString("rarity").equals("rare") || card.getAsString("rarity").equals("mythic"))) {
                            for (var color : MANA_COLORS) {
                                colors.add(new JsonPrimitive(color));
                            }
                        }

                        for (var color : MANA_COLORS) {
                            if (oracle.contains("Add ")) {
                                String string = oracle.substring(oracle.indexOf("Add "));
                                string = string.contains("\n") ? string.substring(0, string.indexOf("\n")) : string;

                                if (string.contains("{" + color + "}")) {
                                    colors.add(new JsonPrimitive(color));
                                }
                            }
                        }
                    });
        }

        boolean hybrid = false;

        if (colors.size() == 2) {
            hybrid = true;

            if (card.has("mana_cost") && !card.getAsString("mana_cost").isEmpty()) {
                String manaCost = card.getAsString("mana_cost");

                for (String string : manaCost.substring(1, manaCost.length() - 1).split("}\\{")) {
                    switch (string.toLowerCase(Locale.ROOT)) {
                        case "w", "u", "b", "r", "g" -> hybrid = false;
                    }

                    if (!hybrid) break;
                }
            }
        }

        card.add(Keys.HYBRID, hybrid);

        List<JsonElement> colorList = new ArrayList<>(colors);

        colorList.sort((c1, c2) -> switch (c1.getAsString()) {
            case "W" -> switch (c2.getAsString()) {
                case "U", "B" -> -1;
                case "R", "G" -> 1;
                default -> 0;
            };
            case "U" -> switch (c2.getAsString()) {
                case "B", "R" -> -1;
                case "W", "G" -> 1;
                default -> 0;
            };
            case "B" -> switch (c2.getAsString()) {
                case "R", "G" -> -1;
                case "W", "U" -> 1;
                default -> 0;
            };
            case "G" -> switch (c2.getAsString()) {
                case "W", "U" -> -1;
                case "B", "R" -> 1;
                default -> 0;
            };
            case "R" -> switch (c2.getAsString()) {
                case "G", "W" -> -1;
                case "B", "U" -> 1;
                default -> 0;
            };
            default -> 0;
        });

        card.add("colors", new JsonArray(colorList));
        card.add(Keys.COLOR_COUNT, colors.size());
    }
}
