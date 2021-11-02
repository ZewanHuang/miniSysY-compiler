package compiler;

import compiler.lexer.Scanner;
import compiler.lexer.Token;
import compiler.parser.Descender;
import compiler.semantics.Analyzer;

import java.util.ArrayList;

public class Parser {

    public String src;      /* 待扫描文本 */

    /**
     * 构造函数
     * @param s 待扫描文本
     */
    public Parser(String s) {
        src = s;
    }

    /**
     * 生成 tokens
     *
     * @return tokens信息字符串
     */
    public String dumpTokens() {
        Scanner scanner = new Scanner(src);
        return scanner.getTokens();
    }

    /**
     * 生成 AST 语法树
     *
     * @return AST字符串形式
     */
    public String dumpAST() {
        Scanner scanner = new Scanner(src);
        ArrayList<Token> tokens = scanner.dumpTokens();
        tokens.add(new Token());
        Descender descender = new Descender(tokens);
        return descender.buildAST().getTree();
    }

    /**
     * 生成符号表
     *
     * @return 符号表字符串形式
     */
    public String dumpSymTable() {
        Scanner scanner = new Scanner(src);
        ArrayList<Token> tokens = scanner.dumpTokens();
        tokens.add(new Token());
        Descender descender = new Descender(tokens);
        Analyzer analyzer = new Analyzer(descender.buildAST());
        return analyzer.buildTable().toString();
    }

}
