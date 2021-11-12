package compiler.semantics.symtable;

import java.util.ArrayList;
import java.util.Stack;

public class SymTable {
    public ArrayList<Item> symTable;

    public SymTable() {
        symTable = new ArrayList<>();
        initTable();
    }

    /**
     * 初始化符号表，加入库函数
     */
    private void initTable() {
        symTable.add(Item.GETCH);
        symTable.add(Item.GETINT);
        symTable.add(Item.GETARRAY);
        symTable.add(Item.PUTINT);
        symTable.add(Item.PUTARRAY);
        symTable.add(Item.PUTCH);
    }

    /**
     * 符号表可视化格式
     *
     * @return 字符串
     */
    public String toString() {
        StringBuilder table = new StringBuilder();
        table.append(String.format("%15s %10s %10s %12s %10s %10s\n", "name", "iType", "vType", "hasCerVal", "blockId", "isValid"));
        for (Item item : symTable)
            table.append(item.toString()).append("\n");
        return table.toString();
    }

    /**
     * 向符号表插入元素 item
     *
     * @param item 元素
     */
    public void insert(Item item) {
        symTable.add(item);
    }

    /**
     * 向符号表插入新元素
     *
     * @param name 标识符名称
     * @param blockId 标识符所在区块
     * @param iType 标识符种类
     * @param vType 标识符值类型
     * @return 插入的新元素
     */
    public Item insert(String name, int blockId, Item.IdentType iType, Item.ValueType vType) {
        Item item = new Item(name, blockId, iType, vType);
        insert(item);
        return item;
    }

    /**
     * 获取符号表中指定 name 的有效标识符
     *
     * @param name 标识符属性
     * @return 符号表记录元素，未查询到时返回空
     */
    public Item getItem(String name) {
        Item item = null;
        for (int i=symTable.size()-1; i>=0; i--) {
            Item temp = symTable.get(i);
            if (temp.name.equals(name) && temp.isValid) {
                item = temp;
                break;
            }
        }
        return item;
    }

    public boolean isAvailDecl(String name, int curBlockId) {
        Item item = getItem(name);
        return item == null || item.blockId != curBlockId;
    }

    /**
     * 将作用域ID为 blockId 的变量定义设置为无效
     *
     * @param blockId 作用域ID
     */
    public void setBlockInvalid(int blockId) {
        for (Item item : symTable)
            if (item.blockId == blockId)
                item.isValid = false;
    }

    public void setBlockValid(int blockId) {
        for (Item item : symTable)
            if (item.blockId == blockId)
                item.isValid = true;
    }

    /**
     * 符号表长度
     *
     * @return 符号表记录数
     */
    public int size() {
        return symTable.size();
    }
}
