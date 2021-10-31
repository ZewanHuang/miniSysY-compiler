package lexer;

import java.util.Arrays;

public class Discriminator {

    private char aChar;

    public void setAChar(char aChar) {
        this.aChar = aChar;
    }

    public static final String[] REVERSER = {
            "if", "else", "while", "break", "continue", "return", "int", "void"
    };
    public boolean isReversed(String str) {
        return Arrays.asList(REVERSER).contains(str);
    }

    public boolean isSpace() {
        return aChar == ' ';
    }
    public boolean isNewline() {
        return aChar == '\n' || aChar == '\r';
    }
    public boolean isTab() {
        return aChar == '\t';
    }
    public boolean isLetter() {
        return Character.isLetter(aChar);
    }
    public boolean isNonZeroDigit() {
        return (aChar >= '1' && aChar <= '9');
    }
    public boolean isOctalDigit() {
        return (aChar >= '0' && aChar <= '7');
    }
    public boolean isHexDigit() {
        return (aChar >= '0' && aChar <= '9') || (aChar >= 'a' && aChar <= 'f')
                || (aChar >= 'A' && aChar <= 'F');
    }
    public boolean isDigit() {
        return Character.isDigit(aChar);
    }
    public boolean isNonDigit() {
        return isLetter() || (aChar == '_');
    }
    public boolean isDecimalConst() {
        return isNonZeroDigit();
    }
    public boolean isOctalConst() {
        return aChar == '0';
    }

}
