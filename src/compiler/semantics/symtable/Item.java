package compiler.semantics.symtable;

import java.util.ArrayList;
import java.util.Arrays;

public class Item {

    /**
     * 标识符类型
     */
    public enum IdentType {
        VAL, CONST, FUNC, ARRAY, PARAM
    }

    /**
     * 标识符存储值的类型
     */
    public enum ValueType {
        INT, REAL, CHAR, POINTER, VOID;

        public String toString() {
            return switch (this) {
                case INT -> "i32";
                case REAL -> "real";
                case CHAR -> "char";
                case POINTER -> "pointer";
                case VOID -> "void";
            };
        }
    }

    public String name;                     /* 标识符名称 */
    public boolean isValid;                 /* 符号在当前区块是否有效 */
    public boolean hasCerVal = false;       /* 编译时是否有确定值 */
    public int blockId;                     /* 所在区块序号 */
    public IdentType iType;                 /* 标识符类型 */
    public ValueType vType;                 /* 标识符值类型 */
    public ArrayList<String> funcParams;    /* 函数参数列表 */
    public int regId;                       /* 寄存器ID */

    public Item(String name, int blockId , IdentType iType, ValueType vType) {
        this.name = name;
        this.blockId = blockId;
        this.iType = iType;
        this.vType = vType;
        this.isValid = true;
        this.funcParams = new ArrayList<>();
    }

    public Item(String name, int blockId , IdentType iType, ValueType vType, ArrayList<String> funcParams) {
        this.name = name;
        this.blockId = blockId;
        this.iType = iType;
        this.vType = vType;
        this.isValid = true;
        this.funcParams = funcParams;
    }

    public static Item GETINT = new Item("getint",0, IdentType.FUNC, ValueType.INT, new ArrayList<>());
    public static Item GETCH = new Item("getch",0, IdentType.FUNC, ValueType.INT, new ArrayList<>());
    public static Item GETARRAY =
            new Item("getarray",0, IdentType.FUNC, ValueType.INT, new ArrayList<>(Arrays.asList("int[]")));
    public static Item PUTINT =
            new Item("putint",0, IdentType.FUNC, ValueType.VOID, new ArrayList<>(Arrays.asList("int")));
    public static Item PUTCH =
            new Item("putch",0, IdentType.FUNC, ValueType.VOID, new ArrayList<>(Arrays.asList("int")));
    public static Item PUTARRAY =
            new Item("putarray",0, IdentType.FUNC, ValueType.VOID, new ArrayList<>(Arrays.asList("int", "int[]")));

    public String toString() {
        return String.format("%15s %10s %10s %12s %10d", name, iType, vType, hasCerVal, blockId);
    }
}
