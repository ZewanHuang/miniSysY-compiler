package parser;

import lexer.Scanner;
import lexer.Token;

import static exception.Exception.error;

public class Descender {

    public Token curToken;
    public Scanner scanner;
    public String result;

    public Descender(Scanner s) {
        this.result = "";
        this.scanner = s;
        nextToken();
    }
    private void nextToken() {
        scanner.getSym();
        curToken = scanner.token;
    }

    public void compUnit() {
        funcDef();
    }
    public void funcDef() {
        result += "define dso_local ";
        funcType();
        ident();
        if (curToken.equals("(")) {
            result += "(";
            nextToken();
            if (curToken.equals(")")) {
                result += ")";
                nextToken();
                block();
            } else error();
        } else error();
    }
    public void funcType() {
        if (curToken.equals("int")) {
            result += "i32 ";
            nextToken();
        } else error();
    }
    public void ident() {
        if (curToken.equals("main")) {
            result += "@main ";
            nextToken();
        } else error();
    }
    public void block() {
        if (curToken.equals("{")) {
            result += "{\n";
            nextToken();
            stmt();
            if (curToken.equals("}")) {
                result += "\n}";
                nextToken();
            } else error();
        }
    }
    public void stmt() {
        if (curToken.equals("return")) {
            result += "ret ";
            nextToken();
            if (curToken.isNumber()) {
                result += "i32 " + curToken.value + " ";
                nextToken();
                if (curToken.equals(";")) {
                    nextToken();
                } else error();
            }
        }
    }
}
