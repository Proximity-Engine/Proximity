function apply(context, card, input) {
    var result = "";

    for (var i = 0; i < input.size(); ++i) {
        result += input.get(i).getAsString();
    }

    return result;
}