package dev.hephaestus.proximity.cards;

import dev.hephaestus.proximity.text.TextComponent;
import dev.hephaestus.proximity.text.Alignment;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.text.Symbol;

import java.util.*;

public final class TextParser {
    private final String oracle;
    private final Map<String, Style> styles;
    private final Style style;
    private final String newLine;

    private List<List<TextComponent>> text;
    private StringBuilder currentWord;
    private List<TextComponent> currentGroup;
    private boolean italic;
    private boolean allWordsItalic;

    public TextParser(String oracle, Map<String, Style> styles, Style style, String newLine) {
        this.oracle = oracle;
        this.styles = styles;
        this.style = style;
        this.newLine = newLine;
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

        for (int i = 0; i < oracle.length(); ++i) {
            char c = oracle.charAt(i);

            switch (c) {
                case '(' -> {
                    this.italic = true;
                    this.currentWord.append(c);
                }
                case ')' -> {
                    this.currentWord.append(c);

                    completeWord();

                    this.italic = allWordsItalic;
                }
                case '*' -> {
                    completeWord();
                    this.italic = !italic;
                }
                case '\n' -> {
                    completeWord();

                    this.text.add(Collections.singletonList(
                            new TextComponent(
                                    this.italic ? this.style.italic() : this.style,
                                    this.newLine
                            )
                    ));
                }
                case '{' -> {
                    completeWord();

                    StringBuilder symbolBuilder = new StringBuilder();
                    ++i;
                    while (this.oracle.charAt(i) != '}') {
                        symbolBuilder.append(this.oracle.charAt(i++));
                    }
                    String symbol = symbolBuilder.toString();

                    List<TextComponent> group = Symbol.symbol(symbol, this.styles, this.style.color(null).font("NDPMTG", null), new Symbol.Factory.Context("oracle"));

                    this.currentGroup.addAll(group);
                }
                case ' ' -> {
                    this.currentWord.append(c);

                    this.completeWord();
                }
                default -> this.currentWord.append(c);
            }
        }

        this.completeWord();

        return new TextBody(this.allWordsItalic ? Alignment.CENTER : Alignment.LEFT, text);
    }

    private void completeWord() {
        String word = this.currentWord.toString();

        if (word.startsWith("\u2014")) {
            loop: for (int i = this.text.size() - 1; i > 0; --i) {
                List<TextComponent> list = this.text.get(i);

                for (ListIterator<TextComponent> iterator = list.listIterator(list.size()); iterator.hasPrevious(); ) {
                    TextComponent text = iterator.previous();
                    if (text.string().equals(this.newLine)) break loop;
                    iterator.set(new TextComponent(text.style().italic(), text.string()));
                }
            }
        }

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
    }
}
