package compiler;

import compiler.lexer.Scanner;

public class Parser {

    public String src;      /* 待扫描文本 */

    /**
     * 构造函数
     * @param s 待扫描文本
     */
    public Parser(String s) {
        src = s;
    }

    public String dumpTokens() {
        Scanner scanner = new Scanner(src);
        return scanner.dumpTokens();
    }
}
