function apply(context, card, number, options, overrides) {
    card.add(["proximity", "card_number"], number);
    card.add(["proximity", "double_sided"], card.has("card_faces"));
    card.getAsJsonObject(["proximity", "options"]).copyAll(options);

    if (card.getAsJsonArray("keywords").contains("mutate")) {
        var split = card.getAsString("oracle_text").split("\n", 2);
        card.addProperty(["oracle_text"], split[1]);
        card.add(["proximity", "util", "mutate_text"], split[0])
    }

    card.copyAll(overrides);

    return card;
}