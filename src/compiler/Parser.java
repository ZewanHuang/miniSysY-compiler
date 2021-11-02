package compiler;

import compiler.lexer.Scanner;
import compiler.parser.Descender;

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
        return scanner.dumpTokens();
    }

    public String dumpAST() {
        Descender descender = new Descender(src);
        return descender.buildAST().getTree();
    }

}
