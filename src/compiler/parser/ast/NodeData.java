package compiler.parser.ast;

import compiler.lexer.Token;

public class NodeData {

    public enum SymType {
        VN, VT
    }
    public SymType symType;
    public int regIdx;
    public String name;
    public String value;
    public Integer intValue;
    public Integer dimension;

    public NodeData() {
        this.name = "";
        this.value = "";
        this.dimension = 0;
    }
    public NodeData(String name) {
        this.name = name;
        this.value = "";
        this.symType = SymType.VN;
        this.dimension = 0;
    }
    public NodeData(Token token) {
        this.name = token.symbol;
        this.value = token.value;
        this.symType = SymType.VT;
        this.dimension = 0;
    }

    @Override
    public String toString() {
        return name + " " + value + " " + symType;
    }

}
