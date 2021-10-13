const MAIN_TYPES = new Set([
    "enchantment",
    "artifact",
    "land",
    "creature",
    "conspiracy",
    "instant",
    "phenomenon",
    "plane",
    "planeswalker",
    "scheme",
    "sorcery",
    "tribal",
    "vanguard"
]);

function apply(context, card, number, options, overrides) {
    var types = card.getAsJsonArray(["proximity", "types"]);
    var mainTypes = [];
    var typeLine = card.getAsString(["type_line"]);
    var JsonArray = Java.type("dev.hephaestus.proximity.json.JsonArray")

    if (typeLine.includes("\u2014")) {
        mainTypes.push(typeLine.split("\u2014")[1]);
    }

    for (const string of typeLine.split(" ")) {
        var type = string.toLowerCase();

        if (type !== "\u2014") {
            types.add(type);
        }

        if (MAIN_TYPES.has(type) && !typeLine.includes("\u2014")) {
            mainTypes.push(string);
        }
    }

    card.add(["proximity", "main_types"], mainTypes.join(""));
    card.add(["proximity", "type_count"], types.size());

    return card;
}