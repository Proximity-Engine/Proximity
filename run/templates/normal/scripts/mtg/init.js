const MAIN_TYPES = new Set();
const MANA_COLORS = new Set();
const LAND_TYPES = {
    "Plains": "W",
    "Island": "U",
    "Swamp": "B",
    "Mountain": "R",
    "Forest": "G"
};

MAIN_TYPES.add("enchantment");
MAIN_TYPES.add("artifact");
MAIN_TYPES.add("land");
MAIN_TYPES.add("creature");
MAIN_TYPES.add("conspiracy");
MAIN_TYPES.add("instant");
MAIN_TYPES.add("phenomenon");
MAIN_TYPES.add("plane");
MAIN_TYPES.add("planeswalker");
MAIN_TYPES.add("scheme");
MAIN_TYPES.add("sorcery");
MAIN_TYPES.add("tribal");
MAIN_TYPES.add("vanguard");

MANA_COLORS.add("W");
MANA_COLORS.add("U");
MANA_COLORS.add("B");
MANA_COLORS.add("G");
MANA_COLORS.add("R");

function processTypes(card) {
    var types = card.getAsJsonArray(["proximity", "types"]);
    var mainTypes = [];
    var typeLine = card.getAsString(["type_line"]);
    var JsonArray = Java.type("dev.hephaestus.proximity.json.JsonArray")
    var cardTypes = new JsonArray();

    if (typeLine.includes("\u2014")) {
        mainTypes.push(typeLine.split("\u2014")[1]);
    }

    for (const string of typeLine.split(" ")) {
        var type = string.toLowerCase();

        if (type !== "\u2014") {
            types.add(type);
        }

        if (MAIN_TYPES.has(type) && !typeLine.includes("\u2014")) {
            mainTypes.push(type);
        }

        if (MAIN_TYPES.has(type)) {
            cardTypes.add(type);
        }
    }

    card.add(["proximity", "main_types"], mainTypes.join(""));
    card.add(["proximity", "types"], cardTypes);
    card.add(["proximity", "type_count"], cardTypes.size());
}

function processColors(card) {
    var colors = new Set();
    var givenColors = card.getAsJsonArray(["colors"]);

    for (var i = 0; i < givenColors.size(); ++i) {
        colors.add(givenColors.get(i).getAsString());
    }

    if (card.getAsJsonArray(["proximity", "types"]).contains("land")) {
        var layout = card.get(["layout"]).getAsString();

        if (layout !== "transform" && layout != "modal_dfc") {
            for (const element of card.getAsJsonArray(["produced_mana"])) {
                if (element in MANA_COLORS) {
                    colors.add(element);
                }
            }
        }

        if (card.has(["oracle_text"])) {
            var oracle = card.getAsString(["oracle_text"]);

            for (const [key, value] of Object.entries(LAND_TYPES)) {
                if (oracle.includes(key)) {
                    colors.add(value);
                }
            }

            if (oracle.includes("of any color") || (oracle.includes("Search your library for a basic land card, put it onto the battlefield") && (card.getAsString("rarity") === "rare" || card.getAsString("rarity") === "mythic"))) {
                for (const color of MANA_COLORS) {
                    colors.add(color);
                }
            }

            for (const color of MANA_COLORS) {
                if (oracle.includes("Add ")) {
                    var string = oracle.substring(oracle.indexOf("Add "));
                    string = string.includes("\n") ? string.substring(0, string.indexOf("\n")) : string;


                    if (string.includes("{" + color + "}")) {
                        colors.add(color);
                    }
                }
            }
        }
    }

    var hybrid = false;

    if (colors.length == 2) {
        hybrid = true;

        if (card.has("mana_cost") && !card.getAsString(["mana_cost"]).isEmpty()) {
            var manaCost = card.getAsString(["mana_cost"]);

            if (manaCost.includes("{W}") || manaCost.includes("{U}") || manaCost.includes("{B}") || manaCost.includes("{R}") || manaCost.includes("{G}")) {
                hybrid = false;
            }
        }
    }

    card.add(["proximity", "hybrid"], hybrid);

    var colorList = Array.from(colors);

    colorList.sort(function (c1, c2) { switch (c1) {
        case "W": switch (c2) {
            case "U", "B":  return -1;
            case "R", "G":  return 1;
            default:        return 0;
        };
        case "U": switch (c2) {
            case "B", "R":  return -1;
            case "W", "G":  return 1;
            default:        return 0;
        };
        case "B": switch (c2) {
            case "R", "G":  return -1;
            case "W", "U":  return 1;
            default:        return 0;
        };
        case "G": switch (c2) {
            case "W", "U":  return -1;
            case "B", "R":  return 1;
            default:        return 0;
        };
        case "R": switch (c2) {
            case "G", "W":  return -1;
            case "B", "U":  return 1;
            default:        return 0;
        };
        default:            return 0;
    }});

    var JsonArray = Java.type("dev.hephaestus.proximity.json.JsonArray")
    var colorsArray = new JsonArray();

    for (const color of colorList) {
        colorsArray.add(color);
    }

    card.add(["colors"], colorsArray);

    card.add(["proximity", "color_count"], colorsArray.size());
}

function process(card, number, options, overrides) {
    processTypes(card);
    processColors(card);

    card.add(["proximity", "card_number"], number);
    card.add(["proximity", "double_sided"], card.has("card_faces"));
    card.getAsJsonObject(["proximity", "options"]).copyAll(options);

    if (card.getAsJsonArray("keywords").contains("mutate")) {
        var split = card.getAsString("oracle_text").split("\n", 2);
        card.addProperty(["oracle_text"], split[1]);
        card.add(["proximity", "util", "mutate_text"], split[0])
    }

    card.copyAll(overrides);

    return [card];
}

function parseFace(card, face) {
    var result = card.deepCopy();

    for (const [key, value] of face.entrySet()) {
        result.add(key, value.deepCopy());
    }

    return result;
}

function parseTwoSidedCard(card, number, options, overrides) {
    var faces = card.getAsJsonArray("card_faces");
    var front = parseFace(card, faces[0]);
    var back = parseFace(card, faces[1]);

    process(front, number, options, overrides);
    process(back, number, options, overrides);

    back.add(["proximity", "flipped"], front.deepCopy());
    back.add(["proximity", "front_face"], false);

    front.add(["proximity", "flipped"], back);
    front.add(["proximity", "front_face"], true);

    return [front, back];
}

function apply(context, card, number, options, overrides) {
    if (card.has("card_faces")) {
        return parseTwoSidedCard(card, number, options, overrides);
    } else {
        return process(card, number, options, overrides);
    }
}