package parser;

import lexer.Scanner;
import lexer.Token;

import java.util.ArrayList;

import static exception.Exception.error;

public class Descender {

    public Token curToken;
    public Scanner scanner;
    private String record;

    private ArrayList<String> exprs;

    private int regIdx;

    public Descender(Scanner s) {
        this.record = "";
        this.scanner = s;
        this.regIdx = 0;
        nextToken();
        this.exprs = new ArrayList<>();
    }
    public String getRecord() {
        return record;
    }

    private void nextToken() {
        scanner.getSym();
        curToken = scanner.token;
    }

    public void compUnit() {
        funcDef();
    }
    public void funcDef() {
        record += "define dso_local ";
        funcType();
        ident();
        if (curToken.equals("(")) {
            record += "(";
            nextToken();
            if (curToken.equals(")")) {
                record += ")";
                nextToken();
                block();
            } else error();
        } else error();
    }
    public void funcType() {
        if (curToken.equals("int")) {
            record += "i32 ";
            nextToken();
        } else error();
    }
    public void ident() {
        if (curToken.equals("main")) {
            record += "@main ";
            nextToken();
        } else error();
    }
    public void block() {
        if (curToken.equals("{")) {
            record += "{\n";
            nextToken();
            stmt();
            if (curToken.equals("}")) {
                record += "}";
                nextToken();
            } else error();
        }
    }
    public void stmt() {
        if (curToken.equals("return")) {
//            record += "ret ";
            nextToken();

            dealExpr();

            if (curToken.equals(";")) {
                record += "\n";
                nextToken();
            } else error();
        }
    }
    public void dealExpr() {
        exprs = new ArrayList<>();
        expr();
        System.out.println(exprs);
        Prioritizer prior = new Prioritizer(exprs);
        prior.analysis();
        record += prior.getRecord();
        record += "ret i32 " + prior.getResult();
    }
    public void expr() {
        addExpr();
    }
    public void addExpr() {
        mulExpr();
        while (curToken.equals("+") || curToken.equals("-")) {
            exprs.add(curToken.value);
            nextToken();
            mulExpr();
        }
    }
    public void mulExpr() {
        unaryExpr();
        while (curToken.equals("*") || curToken.equals("/") || curToken.equals("%")) {
            exprs.add(curToken.value);
            nextToken();
            unaryExpr();
        }
    }
    public void unaryExpr() {
        if (curToken.equals("(") || curToken.isNumber()) {
            primaryExpr();
        } else if (curToken.equals("+") || curToken.equals("-")) {
            exprs.add("(");
            unaryOp();
            unaryExpr();
            exprs.add(")");
        } else error();
    }
    public void primaryExpr() {
        if (curToken.equals("(")) {
            exprs.add("(");
            nextToken();
            expr();
            if (curToken.equals(")")) {
                exprs.add(")");
                nextToken();
            } else error();
        } else if (curToken.isNumber()) {
            exprs.add(curToken.value);
            nextToken();
        } else error();
    }
    public void unaryOp() {
        if (curToken.equals("+") || curToken.equals("-")) {
            exprs.add("0");
            exprs.add(curToken.value);
            nextToken();
        } else error();
    }
}
