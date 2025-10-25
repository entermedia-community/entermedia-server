package org.entermediadb.markdown.text;

/**
 * Functions for finding characters in strings or checking characters.
 */
public class Characters {

    public static int find(char c, CharSequence s, int startIndex) {
        int length = s.length();
        for (int i = startIndex; i < length; i++) {
            if (s.charAt(i) == c) {
                return i;
            }
        }
        return -1;
    }

    public static int findLineBreak(CharSequence s, int startIndex) {
        int length = s.length();
        for (int i = startIndex; i < length; i++) {
            switch (s.charAt(i)) {
                case '\n':
                case '\r':
                    return i;
            }
        }
        return -1;
    }

    /**
     * @see <a href="https://spec.commonmark.org/0.31.2/#blank-line">blank line</a>
     */
    public static boolean isBlank(CharSequence s) {
        return skipSpaceTab(s, 0, s.length()) == s.length();
    }

    public static boolean hasNonSpace(CharSequence s) {
        int length = s.length();
        int skipped = skip(' ', s, 0, length);
        return skipped != length;
    }

    public static boolean isLetter(CharSequence s, int index) {
        int codePoint = Character.codePointAt(s, index);
        return Character.isLetter(codePoint);
    }

    public static boolean isSpaceOrTab(CharSequence s, int index) {
        if (index < s.length()) {
            switch (s.charAt(index)) {
                case ' ':
                case '\t':
                    return true;
            }
        }
        return false;
    }

    /**
     * @see <a href="https://spec.commonmark.org/0.31.2/#unicode-punctuation-character">Unicode punctuation character</a>
     */
    public static boolean isPunctuationCodePoint(int codePoint) {
        switch (Character.getType(codePoint)) {
            // General category "P" (punctuation)
            case Character.DASH_PUNCTUATION:
            case Character.START_PUNCTUATION:
            case Character.END_PUNCTUATION:
            case Character.CONNECTOR_PUNCTUATION:
            case Character.OTHER_PUNCTUATION:
            case Character.INITIAL_QUOTE_PUNCTUATION:
            case Character.FINAL_QUOTE_PUNCTUATION:
                // General category "S" (symbol)
            case Character.MATH_SYMBOL:
            case Character.CURRENCY_SYMBOL:
            case Character.MODIFIER_SYMBOL:
            case Character.OTHER_SYMBOL:
                return true;
            default:
                switch (codePoint) {
                    case '$':
                    case '+':
                    case '<':
                    case '=':
                    case '>':
                    case '^':
                    case '`':
                    case '|':
                    case '~':
                        return true;
                    default:
                        return false;
                }
        }
    }

    /**
     * Check whether the provided code point is a Unicode whitespace character as defined in the spec.
     *
     * @see <a href="https://spec.commonmark.org/0.31.2/#unicode-whitespace-character">Unicode whitespace character</a>
     */
    public static boolean isWhitespaceCodePoint(int codePoint) {
        switch (codePoint) {
            case ' ':
            case '\t':
            case '\n':
            case '\f':
            case '\r':
                return true;
            default:
                return Character.getType(codePoint) == Character.SPACE_SEPARATOR;
        }
    }

    public static int skip(char skip, CharSequence s, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            if (s.charAt(i) != skip) {
                return i;
            }
        }
        return endIndex;
    }

    public static int skipBackwards(char skip, CharSequence s, int startIndex, int lastIndex) {
        for (int i = startIndex; i >= lastIndex; i--) {
            if (s.charAt(i) != skip) {
                return i;
            }
        }
        return lastIndex - 1;
    }

    public static int skipSpaceTab(CharSequence s, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            switch (s.charAt(i)) {
                case ' ':
                case '\t':
                    break;
                default:
                    return i;
            }
        }
        return endIndex;
    }

    public static int skipSpaceTabBackwards(CharSequence s, int startIndex, int lastIndex) {
        for (int i = startIndex; i >= lastIndex; i--) {
            switch (s.charAt(i)) {
                case ' ':
                case '\t':
                    break;
                default:
                    return i;
            }
        }
        return lastIndex - 1;
    }
}
