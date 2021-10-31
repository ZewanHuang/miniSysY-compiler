package lexer;

import static exception.Exception.error;
import utils.StringUtils;

import java.math.BigInteger;

public class Scanner {

    public String src;
    public int ptr;
    public char newChar;
    public Token token;
    public Discriminator discriminator;

    public Scanner(String src) {
        this.src = src;
        this.ptr = 0;
        this.token = new Token();
        this.discriminator = new Discriminator();
    }

    public void getChar() {
        newChar = src.charAt(ptr);
        discriminator.setAChar(newChar);
        ptr++;
    }
    public void getCharWithCheck() {
        if (ptr >= src.length())    error();
        getChar();
    }
    public void retract() {
        ptr--;
    }

    public void clearToken() {
        this.token = new Token();
    }
    public void catTokenByChar(char c) {
        this.token.value += String.valueOf(c);
    }
    public void catToken() {
        catTokenByChar(this.newChar);
    }

    public boolean isHexConst() {
        if (newChar == '0') {
            if ((src.charAt(ptr) == 'x' || src.charAt(ptr) == 'X') &&
                    ((src.charAt(ptr+1) >= '0' && src.charAt(ptr+1) <= '9') ||
                            (src.charAt(ptr+1) >= 'a' && src.charAt(ptr+1) <= 'f') ||
                    (src.charAt(ptr+1) >= 'A' && src.charAt(ptr+1) <= 'F'))) {
                getChar(); getChar(); return true;
            }
        }
        return false;
    }

    public void getSym() {
        if (ptr >= src.length())    return;

        clearToken();
        getChar();

        while (discriminator.isSpace() || discriminator.isNewline() ||
                discriminator.isTab())
            getChar();

        if (discriminator.isNonDigit()) {
            do {
                catToken(); getChar();
            } while (discriminator.isNonDigit() || discriminator.isDigit());
            retract();
            if (discriminator.isReversed(token.value)) {
                token.symbol = StringUtils.capitalize(token.value,0);
            } else {
                token.symbol = "Ident";
            }
        } else if (discriminator.isDigit()) {
            if (discriminator.isDecimalConst()) {
                while (discriminator.isDigit()) {
                    catToken(); getChar();
                }
                retract();
                token.symbol = "Number";
            } else if (isHexConst()) {
                while (discriminator.isHexDigit()) {
                    catToken(); getChar();
                }
                token.value = new BigInteger(token.value,16).toString();
                retract();
                token.symbol = "Number";
            } else if (discriminator.isOctalConst()) {
                while (discriminator.isOctalDigit()) {
                    catToken(); getChar();
                }
                token.value = new BigInteger(token.value,8).toString();
                retract();
                token.symbol = "Number";
            } else error();
        } else if (newChar == '=') {
            catToken();
            getChar();
            if (newChar == '=') {
                catToken(); token.symbol = "Eq";
            } else {
                token.symbol = "Assign"; retract();
            }
        } else if (newChar == ';') {
            catToken(); token.symbol = "Semicolon";
        } else if (newChar == ',') {
            catToken(); token.symbol = "Comma";
        } else if (newChar == '(') {
            catToken(); token.symbol = "LPar";
        } else if (newChar == ')') {
            catToken(); token.symbol = "RPar";
        } else if (newChar == '{') {
            catToken(); token.symbol = "LBrace";
        } else if (newChar == '}') {
            catToken(); token.symbol = "RBrace";
        } else if (newChar == '+') {
            catToken(); token.symbol = "Plus";
        } else if (newChar == '-') {
            catToken(); token.symbol = "Minus";
        } else if (newChar == '*') {
            catToken(); token.symbol = "Mult";
        } else if (newChar == '%') {
            catToken(); token.symbol = "Mod";
        } else if (newChar == '<') {
            catToken(); token.symbol = "Lt";
        } else if (newChar == '>') {
            catToken(); token.symbol = "Gt";
        } else if (newChar == '/') {
            getChar();
            if (newChar == '/') {
                do {
                    getChar();
                } while (!discriminator.isNewline());
                getSym();
            } else if (newChar == '*') {
                do {
                    do { getCharWithCheck(); } while (newChar != '*');
                    do {
                        getCharWithCheck();
                        if (newChar == '/') {
                            getSym();
                            return;
                        }
                    } while (newChar == '*');
                } while (true);
            } else {
                catTokenByChar('/');
                token.symbol = "Div";
                retract();
            }
        } else error();
    }

    public void dumpTokens() {
        while (ptr < src.length()) {
            getSym();
            System.out.println(token);
        }
    }
}
