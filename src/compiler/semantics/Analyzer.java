package compiler.semantics;

import compiler.parser.ast.NodeData;
import compiler.parser.ast.TreeNode;
import compiler.semantics.symtable.Item;
import compiler.semantics.symtable.Item.ValueType;
import compiler.semantics.symtable.Item.IdentType;
import compiler.semantics.symtable.SymTable;

import java.util.Stack;

import static compiler.exception.CompileException.error;

public class Analyzer {

    private final TreeNode<NodeData> ast;
    private final SymTable symTable;

    private int curBlockId;

    public Stack<Integer> blockStack;

    public Analyzer(TreeNode<NodeData> tree, SymTable symTable) {
        this.ast = tree.getRoot();
        this.symTable = symTable;
        this.curBlockId = 0;
        this.blockStack = new Stack<>();
        this.blockStack.push(0);
    }

    public Analyzer(TreeNode<NodeData> tree) {
        this(tree, new SymTable());
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
        if (node.data.name.equals("UnaryExpr") && node.children.size()>=3 && !isFuncCallValid(node))
            error();

        handleBlock(node);
        register(node);
        for (TreeNode<NodeData> child : node.children)
            traversal(child);
    }

    /**
     * 进入 block 时 blockId 加一，退出时符号置为无效，blockId 减一
     *
     * @param node 语法树的当前节点
     */
    public void handleBlock(TreeNode<NodeData> node) {
        if (node.parent != null && node.parent.data.name.equals("Block")) {
            if (node.data.value.equals("{")) {
                curBlockId++;
                blockStack.push(curBlockId);
            } else if (node.data.value.equals("}")) {
                symTable.setBlockInvalid(curBlockId);
                blockStack.pop();
                curBlockId = blockStack.peek();
            }
        }
    }

    /**
     * 登记标识符到符号表中
     *
     * @param node 语法树的当前节点
     */
    public void register(TreeNode<NodeData> node) {
        TreeNode<NodeData> ident;
        switch (node.data.name) {
            case "FuncDef" -> {
                ident = node.getChildAt(1);
                Item.ValueType vtype;
                if (node.getChildAt(0).getChildAt(0).data.name.equals("Int"))
                    vtype = Item.ValueType.INT;
                else
                    vtype = Item.ValueType.VOID;
                symTable.insert(ident.data.value, curBlockId, IdentType.FUNC, vtype);
            }
            case "ConstDef" -> {
                ident = node.getChildAt(0);
                if (!symTable.isAvailDecl(ident.data.value, curBlockId)) // 若同区块内该变量名被用，则报错
                    error();
                Item item = symTable.insert(ident.data.value, curBlockId, IdentType.CONST, Item.ValueType.INT);
                if (node.children.size() == 3 && hasCerVal(node.getChildAt(2))) // 查询其叶子节点判断是否有值
                    item.hasCerVal = true;
            }
            case "VarDef" -> {
                ident = node.getChildAt(0);
                if (!symTable.isAvailDecl(ident.data.value, curBlockId))
                    error();
                Item item = symTable.insert(ident.data.value, curBlockId, IdentType.VAL, Item.ValueType.INT);
                if (node.children.size() == 3 && hasCerVal(node.getChildAt(2)))
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
    public boolean hasCerVal(TreeNode<NodeData> node) {
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
    public boolean isConstInitVal(TreeNode<NodeData> node) {
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
    public boolean isLvalValid(TreeNode<NodeData> node) {
        String ident = node.getChildAt(0).data.value;
        Item item = symTable.getItem(ident);
        if (node.parent.data.name.equals("Stmt"))
            return item != null && item.iType == IdentType.VAL;
        else
            return item != null && (item.iType == IdentType.VAL || item.iType == IdentType.CONST);
    }

    /**
     * 判断函数调用是否合法，即函数是否定义，以及参数是否正确
     *
     * @param node UnaryExp节点
     * @return 是否合法
     */
    public boolean isFuncCallValid(TreeNode<NodeData> node) {
        String ident = node.getChildAt(0).data.value;
        // 判断函数是否定义
        Item item = symTable.getItem(ident);
        if (item == null || item.iType != IdentType.FUNC)
            return false;
        // 判断函数参数个数是否正确
        int paramsCnt = node.children.size() == 3? 0 : node.getChildAt(2).children.size()/2+1;
        if (item.funcParams.size() != paramsCnt)
            return false;
        // TODO:判断函数参数是否正确

        return true;
    }
}
