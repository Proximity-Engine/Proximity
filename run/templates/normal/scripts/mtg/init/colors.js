var MANA_COLORS = new Set(["W", "U", "B", "G", "R"]);
var LAND_TYPES = {
    "Plains": "W",
    "Island": "U",
    "Swamp": "B",
    "Mountain": "R",
    "Forest": "G"
};

function processLand(card, colors) {
    var layout = card.get(["layout"]).getAsString();

    if (layout !== "transform" && layout != "modal_dfc") {
        for (var element of JSON.parse(card.getAsJsonArray(["produced_mana"]).toString())) {
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
            oracle.split("\n").forEach((line, i) => {
                if (line.includes("Add ")) {
                    var string = line.substring(line.indexOf("Add "));

                    if (string.includes("{" + color + "}")) {
                        colors.add(color);
                    }
                }
            });
        }
    }
}

function processHybrid(card, colors) {
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
}

function apply(context, card, number, options, overrides) {
    var colors = new Set();
    var givenColors = card.getAsJsonArray(["colors"]);

    for (var i = 0; i < givenColors.size(); ++i) {
        colors.add(givenColors.get(i).getAsString());
    }

    if (card.getAsJsonArray(["proximity", "types"]).contains("land")) {
        processLand(card, colors);
    }

    processHybrid(card, colors);

    var colorList = Array.from(colors);

    colorList.sort(function (c1, c2) {
        if (c1 === "W" && (c2 === "U" || c2 === "B")) return -1;
        if (c1 === "W" && (c2 === "R" || c2 === "G")) return 1;
        if (c1 === "U" && (c2 === "B" || c2 === "R")) return -1;
        if (c1 === "U" && (c2 === "W" || c2 === "G")) return 1;
        if (c1 === "B" && (c2 === "R" || c2 === "G")) return -1;
        if (c1 === "B" && (c2 === "W" || c2 === "U")) return 1;
        if (c1 === "G" && (c2 === "W" || c2 === "U")) return -1;
        if (c1 === "G" && (c2 === "B" || c2 === "R")) return 1;
        if (c1 === "R" && (c2 === "G" || c2 === "W")) return -1;
        if (c1 === "R" && (c2 === "B" || c2 === "U")) return 1;

        return 0;
    });

    var colorsArray = new JsonArray();

    for (const color of colorList) {
        colorsArray.add(color);
    }

    card.add(["colors"], colorsArray);
    card.add(["proximity", "color_count"], colorsArray.size());

    return card;
}