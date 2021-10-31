package lexer;

public class Token {
    public String symbol;
    public String value;

    public Token() {
        symbol = "";
        value = "";
    }

    public String toString() {
        return symbol + "  " + value;
    }
}
