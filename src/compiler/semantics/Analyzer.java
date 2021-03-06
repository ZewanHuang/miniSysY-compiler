package compiler.semantics;

import compiler.parser.ast.NodeData;
import compiler.parser.ast.TreeNode;
import compiler.semantics.symtable.Item;
import compiler.semantics.symtable.Item.IdentType;
import compiler.semantics.symtable.Item.ValueType;
import compiler.semantics.symtable.SymTable;

import java.util.Stack;

import static compiler.exception.CompileException.error;

public class Analyzer {

    private final TreeNode<NodeData> ast;
    private final SymTable symTable;

    public int curBlockId;

    public Stack<Integer> blockStack;

    public Analyzer(TreeNode<NodeData> tree, SymTable symTable) {
        this.ast = tree.getRoot();
        this.symTable = symTable;
        this.curBlockId = 0;
        this.blockStack = new Stack<>();
        this.blockStack.push(0);
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
     * 登记函数定义到符号表
     *
     * @param node FuncDef节点
     */
    public Item filFuncDef(TreeNode<NodeData> node) {
        TreeNode<NodeData> ident;
        ident = node.getChildAt(1);
        if (!symTable.isDeclAvail(ident.data.value, curBlockId)) // 若同区块内该变量名被用，则报错
            error();
        Item.ValueType vtype;
        if (node.getChildAt(0).getChildAt(0).data.name.equals("Int"))
            vtype = Item.ValueType.INT;
        else
            vtype = Item.ValueType.VOID;
        return symTable.insert(ident.data.value, curBlockId, IdentType.FUNC, vtype);
    }

    /**
     * TODO: 判断函数调用传参的类型
     *
     * @param node Expr节点
     * @return ARRAY或INT
     */
    public ValueType funcRParamType(TreeNode<NodeData> node) {
        if (node.data.dimension > 0) return ValueType.ARRAY;
        return ValueType.INT;
    }

    /**
     * 登记函数参数到符号表
     *
     * @param node FuncFParam节点
     */
    public Item filFuncFParam(TreeNode<NodeData> node) {
        TreeNode<NodeData> _ident = node.getChildAt(1);
        if (node.children.size() == 2)
            return symTable.insert(_ident.data.value, curBlockId+1, IdentType.PARAM, ValueType.INT);
        else
            return symTable.insert(_ident.data.value, curBlockId+1, IdentType.PARAM, ValueType.ARRAY);
    }

    /**
     * 登记常量int变量到符号表
     *
     * @param node ConstDef节点
     */
    public Item filConstValDef(TreeNode<NodeData> node) {
        TreeNode<NodeData> ident = node.getChildAt(0);
        if (!symTable.isDeclAvail(ident.data.value, curBlockId)) error();
        Item item = symTable.insert(ident.data.value, curBlockId, IdentType.CONST, Item.ValueType.INT);
        // 查询其叶子节点判断是否有值，若为全局变量则一定有初始化值
        if (node.children.size() == 3 && hasCerVal(node.getChildAt(2)) || curBlockId == 0)
            item.hasCerVal = true;
        return item;
    }

    /**
     * 登记常量array变量到符号表
     *
     * @param node ConstDef节点
     */
    public Item filConstArrayDef(TreeNode<NodeData> node) {
        TreeNode<NodeData> ident = node.getChildAt(0);
        if (!symTable.isDeclAvail(ident.data.value, curBlockId)) error();
        return symTable.insert(ident.data.value, curBlockId, IdentType.CONST, ValueType.ARRAY);
    }

    /**
     * 登记变量array变量到符号表
     *
     * @param node ConstDef节点
     */
    public Item filVarArrayDef(TreeNode<NodeData> node) {
        TreeNode<NodeData> ident = node.getChildAt(0);
        if (!symTable.isDeclAvail(ident.data.value, curBlockId)) error();
        return symTable.insert(ident.data.value, curBlockId, IdentType.VAL, ValueType.ARRAY);
    }

    /**
     * 登记非数组变量定义到符号表
     *
     * @param node VarDef节点
     * @return 符号表记录
     */
    public Item filVarValDef(TreeNode<NodeData> node) {
        TreeNode<NodeData> ident = node.getChildAt(0);
        if (!symTable.isDeclAvail(ident.data.value, curBlockId)) error();
        Item item = symTable.insert(ident.data.value, curBlockId, IdentType.VAL, Item.ValueType.INT);
        if (node.children.size() == 3 && hasCerVal(node.getChildAt(2)) || curBlockId == 0)
            item.hasCerVal = true;
        return item;
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
     * 判断节点是否为合法的 NoArrayRLval
     *
     * @param node Lval节点
     * @return 是否符合语义
     */
    public boolean isNoArrayLvalValid(TreeNode<NodeData> node) {
        String ident = node.getChildAt(0).data.value;
        Item item = symTable.getItem(ident);
        if (node.parent.data.name.equals("Stmt"))
            return item != null && (item.iType == IdentType.VAL || item.iType == IdentType.PARAM);
        else
            return item != null &&
                    (item.iType == IdentType.VAL || item.iType == IdentType.CONST || item.iType == IdentType.PARAM);
    }

    /**
     * 判断某节点是否为函数右参数延申节点
     *
     * @param node Lval节点
     * @return 是或否
     */
    public boolean belFuncRParams(TreeNode<NodeData> node) {
        TreeNode<NodeData> _temp = node;
        while (!_temp.data.name.equals("CompUnit")) {
            if (_temp.data.name.equals("FuncRParams"))
                return true;
            _temp = _temp.parent;
        }
        return false;
    }

    /**
     * 判断节点是否为合法的 ArrayLval
     *
     * @param node Lval节点
     * @return 是否符合语义
     */
    public boolean isArrayLvalValid(TreeNode<NodeData> node) {
        boolean isValid;
        String ident = node.getChildAt(0).data.value;
        Item item = symTable.getItem(ident);
        if (node.parent.data.name.equals("Stmt"))
            isValid = (item != null && (item.iType == IdentType.VAL || item.iType == IdentType.PARAM)
                    && item.vType == ValueType.ARRAY);
        else
            isValid = (item != null &&
                    (item.iType == IdentType.VAL || item.iType == IdentType.CONST || item.iType == IdentType.PARAM)
                    && item.vType == ValueType.ARRAY);

        int cnt_exp = 0;
        for (TreeNode<NodeData> child : node.children)
            if (child.data.name.equals("Expr"))
                cnt_exp++;
        String identName = node.getChildAt(0).data.value;
        Item arrayItem = symTable.getItem(identName);
        if (!belFuncRParams(node) && arrayItem.arraySize.size() != cnt_exp) isValid = false;
        return isValid;
    }

    /**
     * 判断Lval节点作为数组被调用时，该变量为Int还是Array
     *
     * @param node Lval节点
     * @return Int或Array
     */
    public ValueType arrayLvalType(TreeNode<NodeData> node) {
        int _expCnt = 0;
        for (TreeNode<NodeData> child : node.children) {
            if (child.data.name.equals("Expr"))
                _expCnt += 1;
        }
        Item _arrayItem = symTable.getItem(node.getChildAt(0).data.value);
        if (_arrayItem.arraySize.size() == _expCnt) return ValueType.INT;
        else return ValueType.ARRAY;
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
        return item.funcParams.size() == paramsCnt;
    }

    public boolean isFuncParamValid(TreeNode<NodeData> node) {
        String ident = node.getChildAt(0).data.value;
        // 判断函数是否定义
        Item item = symTable.getItem(ident);
        if (item == null || item.iType != IdentType.FUNC)
            return false;
        // 判断函数参数个数是否正确
        int paramsCnt = node.children.size() == 3? 0 : node.getChildAt(2).children.size()/2+1;
        if (paramsCnt != item.funcParams.size())
            return false;
        // 判断函数参数是否正确：int、array、array几维
        if (paramsCnt > 0) {
            int i = 0;
            for (TreeNode<NodeData> param : node.getChildAt(2).children) {
                if (param.data.name.equals("Expr")) {
                    if (item.funcParams.get(i).vType != funcRParamType(param))
                        return false;
                    if (item.funcParams.get(i).vType == ValueType.ARRAY) {
                        // 若都是Array类型，还需判断维度是否对应相等
                        int _arraySize_call = param.data.dimension;
                        int _arraySize_funcParam = item.funcParams.get(i).arraySize.size();
                        if (_arraySize_call != _arraySize_funcParam) {
                            return false;
                        }
                    }
                    i++;
                }
            }
        }
        return true;
    }
}
