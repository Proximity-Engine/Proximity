
/**
 * Text functions allow templates to transform text in novel ways
 *
 * @param input the original string passed to the function
 * @param card an (immutable) JSON object representing card data
 * @param styles a map of style names to styles
 * @param base_style the style applied to or inherited by the element
 */
function apply(context, input, card, styles, base_style) {
    let TextParser = class {
        constructor(card, input, styles, base_style, italics) {
            this.card = card;
            this.inputText = input;
            this.styles = styles;
            this.style = base_style;
            this.text = [];
            this.currentWord = [];
            this.currentGroup = [];
            this.italic = italics;
            this.allWordsItalic = italics;

            this.italicStyle = {
                fontName: base_style.italicFontName,
                size: base_style.size,
                kerning: base_style.kerning,
                shadow: base_style.shadow,
                outline: base_style.outline,
                capitalization: base_style.capitalization,
                color: base_style.color
            }
        }

        parse() {
            for (var i = 0; i < this.inputText.length; ++i) {
                var c = this.inputText.charAt(i);

                switch (c) {
                    case '(':
                        if (this.card.proximity.options.reminder_text) {
                            this.italic = true;
                            this.currentWord.push(c);
                        } else {
                            while (c != ')') {
                                c = this.inputText.charAt(++i);
                            }
                        }

                        break;
                    case ')':
                        this.currentWord.push(c);
                        this.completeWord();
                        this.italic = this.allWordsItalic;
                        break;
                    case '*':
                        this.completeWord();
                        this.italic = !this.italic;
                        break;
                    case "\n":
                        var word = this.completeWord();

                        if (word.length > 0 || this.text.length > 0) {
                            this.text.push([{
                                style: this.italic ? this.italicStyle : this.style,
                                value: "\n"
                            }])
                        }

                        break;
                    case ' ':
                        this.currentWord.push(' ');
                        this.completeWord();
                        break;
                    case '"':
                        if (this.currentWord.length == 0 || this.currentWord[this.currentWord.length - 1].endsWith("(")) {
                            this.currentWord.push("\u201C");
                        } else {
                            this.currentWord.push("\u201D");
                        }

                        break;
                    case "\u2014":
                        if (i > 0 && i < this.inputText.length - 1 && this.inputText.charAt(i - 1) === ' ' && this.inputText.charAt(i + 1) == ' ') {
                            var italic = true;
                            var s = this.inputText.slice(0, i);
                            var nl = s.lastIndexOf('\n');
                            var bp = s.lastIndexOf('\u2022')

                            if (nl >= bp) {
                                for (var j = i + 1; j < this.inputText.length; ++j) {
                                    if (this.inputText.charAt(j) === '(') {
                                        italic = false;
                                        break;
                                    } else if (this.inputText.charAt(j) === '\n') {
                                        break;
                                    }
                                }
                            }

                            if (nl < bp) {
                                var word = this.inputText.substring(bp + 1, i).trim();

                                if (this.inputText.substring(0, nl).contains(word)) {
                                    italic = false;
                                }
                            }

                            if (italic) {
                                loop: for (var j = this.text.length - 1; j >= 0; --j) {
                                    var list = this.text[j];

                                    for (var k = list.length - 1; k >= 0; --k) {
                                        var component = list[k];

                                        if (component.value === "\n") {
                                            break loop;
                                        } else {
                                            list[k] = {
                                                style: this.italicStyle,
                                                value: component.value
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    default:
                        this.currentWord.push(c);
                }
            }

            this.completeWord();

            return this.text;
        }

        completeWord() {
            var word = this.currentWord.join("");

            if (word.length > 0 || this.currentGroup.length > 0) {
                if (word.length > 0) {
                    if (!this.italic) {
                        this.allWordsItalic = false;
                    }
                }

                this.currentGroup.push({
                    style: this.italic ? this.italicStyle : this.style,
                    value: word
                })

                this.text.push(this.currentGroup);

                this.currentWord = [];
                this.currentGroup = [];
            }

            return word;
        }
    }

    const parser = new TextParser(
        card, input, styles, base_style, true
    );

    return parser.parse();
}