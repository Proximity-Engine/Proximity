package dev.hephaestus.proximity.cards;

import dev.hephaestus.proximity.TextComponent;
import dev.hephaestus.proximity.templates.Template;
import dev.hephaestus.proximity.text.Alignment;
import dev.hephaestus.proximity.text.Style;
import dev.hephaestus.proximity.text.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OracleParser {
    private final String oracle;
    private final Template template;
    private final Style style;

    private List<List<TextComponent>> text;
    private StringBuilder currentWord;
    private List<TextComponent> currentGroup;
    private boolean italic;
    private boolean allWordsItalic;

    public OracleParser(String oracle, Template template) {
        this.oracle = oracle;
        this.template = template;
        this.style = template.getStyle("oracle");
    }

    public OracleText parse() {
        this.text = new ArrayList<>();
        this.currentWord = new StringBuilder();
        this.currentGroup = new ArrayList<>();
        this.italic = false;
        this.allWordsItalic = true;

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

                    this.italic = false;
                }
                case '\n' -> {
                    completeWord();

                    this.text.add(Collections.singletonList(
                            new TextComponent(
                                    this.italic ? this.style.italic() : this.style,
                                    "\n"
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

                    List<TextComponent> group = Symbol.symbol(symbol, this.template, this.style.color(null).font("NDPMTG", null), new Symbol.Factory.Context("oracle"));

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

        return new OracleText(this.allWordsItalic ? Alignment.CENTER : Alignment.LEFT, text);
    }

    private void completeWord() {
        if (!this.currentWord.isEmpty() || !this.currentGroup.isEmpty()) {
            if (!this.currentWord.isEmpty()) {
                if (!this.italic) {
                    this.allWordsItalic = false;
                }

                this.currentGroup.add(new TextComponent(
                                this.italic ? this.style.italic() : this.style,
                                this.currentWord.toString()
                        )
                );
            }

            this.text.add(this.currentGroup);
            this.currentWord = new StringBuilder();
            this.currentGroup = new ArrayList<>();
        }
    }
}
