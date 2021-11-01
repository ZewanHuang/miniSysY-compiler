package parser;

import lexer.Scanner;

public class Parser {

    public String parse(String src) {
        Scanner scanner = new Scanner(src);
        Descender desc = new Descender(scanner);
        desc.compUnit();
        return desc.getRecord();
    }
}
