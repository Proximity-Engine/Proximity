package dev.hephaestus.proximity.cards;

import dev.hephaestus.proximity.json.JsonObject;
import dev.hephaestus.proximity.text.TextAlignment;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.text.Symbol;
import dev.hephaestus.proximity.text.TextComponent;

import java.util.*;
import java.util.function.Function;

public final class TextParser {
    private final String oracle;
    private final Function<String, Style> styleGetter;
    private final Style style;
    private final String newLine;
    private final JsonObject options;

    private List<List<TextComponent>> text;
    private StringBuilder currentWord;
    private List<TextComponent> currentGroup;
    private boolean italic;
    private boolean allWordsItalic;

    public TextParser(String oracle, Function<String, Style> styleGetter, Style style, String newLine, JsonObject options) {
        this.oracle = oracle;
        this.styleGetter = styleGetter;
        this.style = style;
        this.newLine = newLine;
        this.options = options;
    }

    public TextBody parseOracle() {
        return this.parse(false);
    }

    public TextBody parseFlavor() {
        return this.parse(true);
    }

    private TextBody parse(boolean allWordsItalic) {
        this.text = new ArrayList<>();
        this.currentWord = new StringBuilder();
        this.currentGroup = new ArrayList<>();
        this.italic = allWordsItalic;
        this.allWordsItalic = !allWordsItalic;

        int n = oracle.codePointCount(0, oracle.length());

        int i;
        for (i = 0; i < n; ++i) {
            int c = oracle.codePointAt(i);

            switch (c) {
                case '(' -> {
                    if (this.options.getAsBoolean("reminder_text")) {
                        this.italic = true;
                        this.currentWord.append(Character.toString(c));
                    } else {
                        while(c != ')') {
                            c = oracle.codePointAt(++i);
                        }
                    }
                }
                case ')' -> {
                    this.currentWord.append(Character.toString(c));

                    completeWord();

                    this.italic = allWordsItalic;
                }
                case '*' -> {
                    completeWord();
                    this.italic = !italic;
                }
                case '\n' -> {
                    String word = completeWord();

                    if (!word.isEmpty() || !this.text.isEmpty()) {
                        this.text.add(Collections.singletonList(
                                new TextComponent(
                                        this.italic ? this.style.italic() : this.style,
                                        this.newLine
                                )
                        ));
                    }
                }
                case '{' -> {
                    completeWord();

                    StringBuilder symbolBuilder = new StringBuilder();
                    ++i;
                    while (this.oracle.codePointAt(i) != '}') {
                        symbolBuilder.append(Character.toString(this.oracle.codePointAt(i++)));
                    }
                    String symbol = symbolBuilder.toString();

                    List<TextComponent> group = Symbol.symbol(symbol, this.styleGetter, this.style.color(null).font("NDPMTG", null), new Symbol.Factory.Context("oracle"));

                    this.currentGroup.addAll(group);
                }
                case ' ' -> {
                    this.currentWord.append(' ');

                    this.completeWord();
                }
                case '"' -> {
                    if (this.currentWord.isEmpty() || this.currentWord.toString().endsWith("(")) {
                        this.currentWord.append("\u201C");
                    } else {
                        this.currentWord.append("\u201D");

                    }
                }
                case 0x2014 /* em dash */ -> {
                    if (i > 0 && i < n - 1 && oracle.codePointAt(i - 1) == ' ' && oracle.codePointAt(i + 1) == ' ') {
                        boolean italic = true;

                        String s = oracle.substring(0, i);
                        int nl = s.lastIndexOf('\n');
                        int bp = s.lastIndexOf(Character.toString(0x2022));

                        if (!(nl < bp)) {
                            for (int j = i + 1; j < n; ++j) {
                                if (oracle.charAt(j) == '(') {
                                    italic = false;
                                    break;
                                } else if (oracle.charAt(j) == '\n') {
                                    break;
                                }
                            }
                        }


                        if (nl < bp) {
                            String word = oracle.substring(bp + 1, i).trim();

                            if (oracle.substring(0, nl).contains(word)) {
                                italic = false;
                            }
                        }

                        if (italic) {
                            loop: for (int j = this.text.size() - 1; j >= 0; --j) {
                                List<TextComponent> list = this.text.get(j);

                                for (int k = list.size() - 1; k >= 0; --k) {
                                    TextComponent component = list.get(k);

                                    if (component.string().equals(this.newLine)) {
                                        break loop;
                                    } else {
                                        list.set(k, new TextComponent(component.style().italic(), component.string()));
                                    }
                                }
                            }
                        }
                    }

                    this.currentWord.append(Character.toString(c));
                }
                default -> this.currentWord.append(Character.toString(c));
            }
        }

        this.completeWord();

        return new TextBody(this.allWordsItalic ? TextAlignment.CENTER : TextAlignment.LEFT, text);
    }

    private String completeWord() {
        String word = this.currentWord.toString();

        if (!this.currentWord.isEmpty() || !this.currentGroup.isEmpty()) {
            if (!this.currentWord.isEmpty()) {
                if (!this.italic) {
                    this.allWordsItalic = false;
                }

                this.currentGroup.add(new TextComponent(
                                this.italic ? this.style.italic() : this.style,
                                word
                        )
                );
            }

            this.text.add(this.currentGroup);
            this.currentWord = new StringBuilder();
            this.currentGroup = new ArrayList<>();
        }

        return word;
    }
}
