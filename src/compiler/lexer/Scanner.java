package compiler.lexer;

import compiler.utils.StringUtils;

import java.math.BigInteger;

import static compiler.exception.CompileException.error;

public class Scanner {

    public String src;              /* 扫描的文本内容 */
    private int ptr;                /* 读文件头指针 */
    private char newChar;           /* 当前读取的字符 */
    private Token token;            /* 当前识别的 token */
    private final Judger judger;    /* 字符判别器 */

    /**
     * 构造函数
     *
     * @param s 扫描文本
     */
    public Scanner(String s) {
        src = s;
        ptr = 0;
        token = new Token();
        judger = new Judger();
    }

    /**
     * 获取下一个字符并设置为字符判断器的属性
     */
    private void getChar() {
        newChar = src.charAt(ptr++);
        judger.setAChar(newChar);
    }
    /**
     * 获取下一个字符，超出字符串长度则报错
     */
    private void getCharWithCheck() {
        if (ptr >= src.length()-1)    error();
        getChar();
    }

    /**
     * 回退读字符头指针
     */
    private void retract() {
        ptr--;
    }

    /**
     * 清除存储的 token 信息
     */
    private void clearToken() {
        this.token = new Token();
    }

    /**
     * 将指定字符与当前 token 连接
     * @param c 字符
     */
    private void catTokenByChar(char c) {
        this.token.value += String.valueOf(c);
    }

    /**
     * 将当前字符与当前 token 连接
     */
    private void catToken() {
        catTokenByChar(this.newChar);
    }

    /**
     * 判断当前是否读取到十六进制数字
     * @return 是或否
     */
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

    /**
     * 获取下一个 token
     * @return token.value
     */
    public String getToken() {
        if (ptr >= src.length()-1)    return token.value;

        clearToken();
        getChar();

        while (judger.isSpace() || judger.isNewline() ||
                judger.isTab())
            getChar();

        if (judger.isNonDigit()) {
            do {
                catToken();
                getChar();
            } while (judger.isNonDigit() || judger.isDigit());
            retract();
            if (judger.isReversed(token.value)) {
                token.symbol = StringUtils.capitalize(token.value,0);
            } else {
                token.symbol = "Ident";
            }
        } else if (judger.isDigit()) {
            if (judger.isDecimalConst()) {
                while (judger.isDigit()) {
                    catToken(); getChar();
                }
                retract();
                token.symbol = "Number";
            } else if (isHexConst()) {
                while (judger.isHexDigit()) {
                    catToken(); getChar();
                }
                token.value = new BigInteger(token.value,16).toString();
                retract();
                token.symbol = "Number";
            } else if (judger.isOctalConst()) {
                while (judger.isOctalDigit()) {
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
                } while (!judger.isNewline());
                getToken();
            } else if (newChar == '*') {
                do {
                    do { getCharWithCheck(); } while (newChar != '*');
                    do {
                        getCharWithCheck();
                        if (newChar == '/') {
                            getToken();
                            return token.value;
                        }
                    } while (newChar == '*');
                } while (true);
            } else {
                catTokenByChar('/');
                token.symbol = "Div";
                retract();
            }
        } else error();

        return token.toString();
    }

    public String dumpTokens() {
        StringBuilder str = new StringBuilder();
        while (ptr < src.length()-1) {
            str.append(getToken()).append("\n");
        }
        return str.toString();
    }

}
