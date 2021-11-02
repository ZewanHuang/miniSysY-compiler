package compiler.semantics.symtable;

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
        INT, REAL, CHAR, POINTER, VOID
    }

    public String name;                 /* 标识符名称 */
    public boolean isValid;             /* 符号在当前区块是否有效 */
    public boolean hasCerVal = false;   /* 编译时是否有确定值 */
    public int blockId;                 /* 所在区块序号 */
    public IdentType iType;             /* 标识符类型 */
    public ValueType vType;             /* 标识符值类型 */

    public Item(String name, int blockId , IdentType iType, ValueType vType) {
        this.name = name;
        this.blockId = blockId;
        this.iType = iType;
        this.vType = vType;
        this.isValid = true;
    }

    public String toString() {
        return String.format("%15s %10s %10s %12s %10d", name, iType, vType, hasCerVal, blockId);
    }
}
