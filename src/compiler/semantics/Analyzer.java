package compiler.semantics;

import compiler.parser.ast.NodeData;
import compiler.parser.ast.TreeNode;
import compiler.semantics.symtable.Item;
import compiler.semantics.symtable.Item.ValueType;
import compiler.semantics.symtable.Item.IdentType;
import compiler.semantics.symtable.SymTable;

import static compiler.exception.CompileException.error;

public class Analyzer {

    private final TreeNode<NodeData> ast;
    private final SymTable symTable;

    private int curBlockId;

    public Analyzer(TreeNode<NodeData> tree) {
        this.ast = tree.getRoot();
        this.symTable = new SymTable();
        this.curBlockId = 0;
    }

    /**
     * 构建并返回符号表
     *
     * @return 符号表
     */
    public SymTable buildTable() {
        traversal(ast);
        return symTable;
    }

    /**
     * 递归遍历语法树，记录符号表，执行语义分析
     *
     * @param node 树节点
     */
    private void traversal(TreeNode<NodeData> node) {
        if (node.data.name.equals("ConstInitVal") && !isConstInitVal(node))
            error();
        if (node.data.name.equals("Lval") && !isLvalValid(node))
            error();
        if (node.data.name.equals("Block"))
            curBlockId++;
        register(node);
        for (TreeNode<NodeData> child : node.children)
            traversal(child);
    }

    /**
     * 登记标识符到符号表中
     *
     * @param node 语法树的当前节点
     */
    private void register(TreeNode<NodeData> node) {
        if (!node.data.name.equals("Ident"))
            return;
        TreeNode<NodeData> parent = node.parent;
        switch (parent.data.name) {
            case "FuncDef" -> {
                Item.ValueType vtype;
                if (parent.getChildAt(0).getChildAt(0).data.name.equals("Int"))
                    vtype = Item.ValueType.INT;
                else
                    vtype = Item.ValueType.VOID;
                symTable.insert(node.data.value, curBlockId, IdentType.FUNC, vtype);
            }
            case "ConstDef" -> {
                if (!symTable.isAvailDecl(node.data.value, curBlockId)) // 若同区块内该变量名被用，则报错
                    error();
                Item item = symTable.insert(node.data.value, curBlockId, IdentType.CONST, Item.ValueType.INT);
                if (parent.children.size() == 3 && hasCerVal(parent.getChildAt(2))) // 查询其叶子节点判断是否有值
                    item.hasCerVal = true;
            }
            case "VarDef" -> {
                if (!symTable.isAvailDecl(node.data.value, curBlockId))
                    error();
                Item item = symTable.insert(node.data.value, curBlockId, IdentType.VAL, Item.ValueType.INT);
                if (parent.children.size() == 3 && hasCerVal(parent.getChildAt(2)))
                    item.hasCerVal = true;
            }
        }
    }

    /**
     * 判断该节点对应的变量是否有确定的值
     *
     * @param node 节点
     * @return 是否有确定值
     */
    private boolean hasCerVal(TreeNode<NodeData> node) {
        for (TreeNode<NodeData> leaf : node.getLeaves())
            if (leaf.data.name.equals("Ident")) {
                Item item = symTable.getItem(leaf.data.value);
                if (item == null || !item.hasCerVal)
                    return false;
            }
        return true;
    }

    /**
     * 判断该 ConstInitVal 节点延申的叶节点，即变量表达式是否符合可求值且仅能含已定义常量的特性
     *
     * @param node ConstInitVal节点
     * @return 是否满足
     */
    private boolean isConstInitVal(TreeNode<NodeData> node) {
        for (TreeNode<NodeData> leaf : node.getLeaves())
            if (leaf.data.name.equals("Ident")) {
                Item item = symTable.getItem(leaf.data.value);
                if (item == null || item.iType != IdentType.CONST || !item.hasCerVal)
                    return false;
            }
        return true;
    }

    /**
     * 判断节点是否为合法的 Lval
     *
     * @param node Lval节点
     * @return 是否符合语义
     */
    private boolean isLvalValid(TreeNode<NodeData> node) {
        String ident = node.getChildAt(0).data.value;
        Item item = symTable.getItem(ident);
        if (node.parent.data.name.equals("Stmt"))
            return item != null && item.iType == IdentType.VAL;
        else
            return item != null && (item.iType == IdentType.VAL || item.iType == IdentType.CONST);
    }
}
