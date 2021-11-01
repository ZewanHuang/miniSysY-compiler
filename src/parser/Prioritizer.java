package parser;

import utils.StringUtils;

import java.util.ArrayList;
import java.util.Stack;

import static exception.Exception.error;

public class Prioritizer {

    public ArrayList<String> src;
    private String record;
    private String result;
    private int regIdx;

    public Prioritizer(ArrayList<String> s) {
        src = s;
        record = "";
        result = "";
        regIdx = 0;
    }
    public String getRecord() {
        return record;
    }
    public String getResult() {
        return result;
    }

    private static final char[][] OPM = {
            {'>','>','<','<','<','<','>','>'},
            {'>','>','<','<','<','<','>','>'},
            {'>','>','>','>','>','<','>','>'},
            {'>','>','>','>','>','<','>','>'},
            {'>','>','>','>','>','<','>','>'},
            {'<','<','<','<','<','<','=',' '},
            {'>','>','>','>','>',' ','>','>'},
            {'<','<','<','<','<','<',' ',' '}
    };

    private int indexOfOpera(char c) {
        return switch (c) {
            case '+' -> 0;
            case '-' -> 1;
            case '*' -> 2;
            case '/' -> 3;
            case '%' -> 4;
            case '(' -> 5;
            case ')' -> 6;
            case '#' -> 7;
            default -> -1;
        };
    }

    private String flagOfOpera(String opera) {
        return switch (opera) {
            case "-" -> "sub";
            case "+" -> "add";
            case "*" -> "mul";
            case "/" -> "sdiv";
            case "%" -> "srem";
            default -> "error";
        };
    }

    private char operaCmp(char a, char b) {
        return OPM[indexOfOpera(a)][indexOfOpera(b)];
    }
    private char operaCmp(String a, String b) {
        return operaCmp(a.charAt(0), b.charAt(0));
    }

    public void analysis() {
        Stack<String> operaObj = new Stack<>();
        Stack<String> operaSym = new Stack<>();
        operaSym.push("#");

        int len = src.size();
        src.add("#");
        for (int i=0; i<=len;) {
            String str = src.get(i);
            if (StringUtils.isNonNegInteger(str)) {
                operaObj.push(str);
                i++;
                continue;
            }

            if (operaSym.peek().equals("#") && str.equals("#"))
                break;

            switch (operaCmp(operaSym.peek(), str)) {
                case '<' -> {
                    operaSym.push(str);
                    i++;
                }
                case '=' -> {
                    operaSym.pop();
                    i++;
                }
                case '>' -> {
                    String a = operaObj.peek();
                    operaObj.pop();
                    String b = operaObj.peek();
                    operaObj.pop();
                    String reg = "%x0" + String.valueOf(regIdx++);
                    record += reg + " = " + flagOfOpera(operaSym.peek()) + " i32 " + b + ", " + a + "\n";
                    operaObj.push(reg);
                    operaSym.pop();
                }
                case ' ' -> error();
            }
        }
        if (!operaSym.peek().equals("#") || operaObj.size() != 1)
            error();

        result = operaObj.peek();
    }
}
