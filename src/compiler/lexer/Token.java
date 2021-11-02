package compiler.lexer;

public class Token {

    public String symbol;
    public String value;

    public Token() {
        symbol = "";
        value = "";
    }

    public boolean equals(String s) {
        return value.equals(s);
    }

    public boolean isNumber() {
        return symbol.equals("Number");
    }
    public boolean isIdent() {
        return symbol.equals("Ident");
    }

    public String toString() {
        return symbol + "  " + value;
    }

}
