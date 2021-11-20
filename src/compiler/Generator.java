package compiler;

import compiler.parser.ast.NodeData;
import compiler.parser.ast.TreeNode;
import compiler.semantics.Analyzer;
import compiler.semantics.symtable.Item;
import compiler.semantics.symtable.SymTable;
import compiler.utils.StringUtils;

import java.util.ArrayList;
import java.util.Stack;

import static compiler.exception.CompileException.error;


public class Generator {

    private TreeNode<NodeData> ast;
    private int regId;
    private String product;

    /**
     * 在生成器中，需要边遍历边填入新符号表
     */
    private SymTable symTable;
    private Analyzer analyzer;

    /**
     * 为循环而设立的 tag
     */
    private Stack<Recorder> stk = new Stack<Recorder>();

    private class Mark {
        String tag;

        public Mark(String t) {
            this.tag = t;
        }
    }

    private class Recorder {
        ArrayList<Mark> marks;

        public Recorder() {
            this.marks = new ArrayList<>();
        }

        public void record(Mark m) {
            this.marks.add(m);
        }
    }

    public Generator(TreeNode<NodeData> tree) {
        this.ast = tree;
        this.regId = 1;
        this.product = """
                declare i32 @getint()
                declare void @putint(i32)
                declare i32 @getch()
                declare void @putch(i32)
                declare i32 @getarray(i32*)
                declare void @putarray(i32, i32*)
                declare void @memset(i32*, i32, i32)
                """;
        this.symTable = new SymTable();
        this.analyzer = new Analyzer(ast, symTable);
    }

    public SymTable getSymTable() {
        return this.symTable;
    }

    /**
     * 生成 llvm 并返回生成代码
     *
     * @return llvm字符串形式
     */
    public String generate() {
        visit(ast);
        return product;
    }

    /**
     * 处理经语义分析后的语法树，生成 llvm 代码记录于 product 中
     *
     * @param node 节点
     */
    private void visit(TreeNode<NodeData> node) {
        analyzer.handleBlock(node);
        switch (node.data.name) {
            case "FuncDef" -> visitFuncDef(node);
            case "Block" -> visitBlock(node);
            case "ConstDef" -> visitConstDef(node);
            case "ConstInitVal" -> visitConstInitVal(node);
            case "ConstExpr" -> visitConstExp(node);
            case "VarDef" -> visitVarDef(node);
            case "InitVal" -> visitInitVal(node);
            case "Stmt" -> visitStmt(node);
            case "Expr" -> visitExpr(node);
            case "AddExpr", "MulExpr" -> visitOperaExpr(node);
            case "UnaryExpr" -> visitUnaryExpr(node);
            case "PrimaryExpr" -> visitPrimExpr(node);
            case "FuncRParams" -> visitFuncRParams(node);
            case "Lval" -> visitLval(node);
            case "Cond" -> visitCond(node);
            case "RelExpr", "EqExpr" -> visitCmpExpr(node);
            case "LAndExpr", "LOrExpr" -> visitLogicExpr(node);
            default -> {
                for (TreeNode<NodeData> child : node.children)
                    visit(child);
            }
        }
    }

    /**
     * 当前节点为 FuncDef 节点时，添加函数定义语句
     *
     * @param node FuncDef节点
     */
    private void visitFuncDef(TreeNode<NodeData> node) {
        analyzer.filFuncDef(node);
        String funcName = node.getChildAt(1).data.value;
        Item funcItem = symTable.getItem(funcName);
        product += "define dso_local " + funcItem.vType + " @" + funcName + "() {\n";
        visit(node.getChildAt(4));
        product += "}";
    }

    private void visitBlock(TreeNode<NodeData> node) {
        for (TreeNode<NodeData> child : node.children)
            visit(child);
    }

    private void visitConstValDef(TreeNode<NodeData> node) {
        Item declItem = analyzer.filConstValDef(node);
        String declName = node.getChildAt(0).data.value;

        if (declItem.blockId == 0) {
            visit(node.getChildAt(2));
            product += "@" + declName + " = dso_local global i32 "
                    + node.getChildAt(2).data.intValue + "\n";
        } else {
            declItem.regId = regId;
            String decl = "%" + (regId++);
            product += decl + " = alloca " + declItem.vType + "\n";
            visit(node.getChildAt(2));
            if (declItem.blockId == 0) {
                product += "store " + declItem.vType + " "
                        + node.getChildAt(2).data.value + ", "
                        + declItem.vType + "* @" + declName + "\n";
            } else {
                product += "store " + declItem.vType + " "
                        + node.getChildAt(2).data.value + ", "
                        + declItem.vType + "* " + decl + "\n";
            }
        }
        node.data.intValue = node.getChildAt(2).data.intValue;
        declItem.intValue = node.getChildAt(2).data.intValue;
    }

    private boolean hasOnlyPart(TreeNode<NodeData> node) {
        for (TreeNode<NodeData> leaf : node.getLeaves())
            if (!leaf.data.value.equals("{") && !leaf.data.value.equals("}"))
                return false;
        return true;
    }

    private ArrayList<Integer> arrayShape;

    private void visitConstArrayDef(TreeNode<NodeData> node) {
        Item declItem = analyzer.filConstArrayDef(node);
        String declName = node.getChildAt(0).data.value;

        boolean hasConstInit = false;
        int size = 1;
        for (TreeNode<NodeData> child : node.children) {
            if (child.data.name.equals("ConstExpr")) {
                visit(child);
                if (child.data.intValue < 0) error();
                declItem.arraySize.add(child.data.intValue);
                size *= child.data.intValue;
            }
            if (child.data.name.equals("ConstInitVal"))
                hasConstInit = true;
        }
        this.arrayShape = declItem.arraySize;

        TreeNode<NodeData> constInitVal = node.getChildAt(node.children.size()-1);

        // ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
        if (analyzer.curBlockId == 0) {
            product += "@" + declName + " = dso_local constant "
                    + "[" + size + " x i32] ";
            if (hasConstInit) {
                visit(constInitVal);
                product += "[" + constInitVal.data.value + "]\n";
            } else {
                product += "zeroinitializer\n";
            }
        } else {
            declItem.regId = regId;
            String decl = "%" + (regId++);
            product += decl + " = alloca [" + size + " x i32]\n";
            String _reg = "%" + (regId++);
            product += _reg + " = getelementptr [" + size + " x i32], ["
                    + size + " x i32]* " + decl + ", i32 0, i32 0\n"
                    + "call void @memset(i32* " + _reg + ", i32 0, i32 " + 4 * size + ")\n";
            if (hasConstInit) {
                visit(constInitVal);
            }
        }
    }

    private void visitConstDef(TreeNode<NodeData> node) {
        TreeNode<NodeData> ident = node.getChildAt(0);
        if (!symTable.isDeclAvail(ident.data.value, analyzer.curBlockId)) // 若同区块内该变量名被用，则报错
            error();

        if (node.children.size() == 3) visitConstValDef(node);
        else visitConstArrayDef(node);
    }

    private void visitConstInitValNonArray(TreeNode<NodeData> node) {
        if (!analyzer.isConstInitVal(node))
            error();
        visit(node.getChildAt(0));
        node.data.value = node.getChildAt(0).data.value;
        node.data.intValue = node.getChildAt(0).data.intValue;
    }

    private int initDepth(TreeNode<NodeData> node) {
        int depth = 0;
        TreeNode<NodeData> temp = node;
        while (!temp.parent.getChildAt(0).data.name.equals("Ident")) {
            temp = temp.parent;
            depth++;
        }
        return depth;
    }

    private int arrayRegId(TreeNode<NodeData> node) {
        TreeNode<NodeData> temp = node;
        while (!temp.parent.getChildAt(0).data.name.equals("Ident")) {
            temp = temp.parent;
        }
        temp = temp.parent.getChildAt(0);
        return symTable.getItem(temp.data.value).regId;
    }

    private int fillEmptySize(TreeNode<NodeData> node, int cnt) {
        int depth = initDepth(node);
        int temp = 1;
        for (int i = depth+1; i < this.arrayShape.size(); i++)
            temp *= this.arrayShape.get(i);
        try {
            return (this.arrayShape.get(depth)-cnt) * temp;
        } catch (Exception e) {
            error();
            return 0;
        }
    }

    private void visitConstInitArray(TreeNode<NodeData> node) {
        /*
         * TODO: 初始化
         * 1. 只有花括号则全部初始化为 0
         * 2. 初始化和维度完全对应则一一对应
         * 3. 初始化值少于维度个数，其余隐式初始化为 0
         */
        if (analyzer.curBlockId == 0) {
            if (!analyzer.isConstInitVal(node)) error();
            if (node.getChildAt(0).data.name.equals("ConstExpr")) {
                if (initDepth(node) != this.arrayShape.size()) error();
                visit(node.getChildAt(0));
                node.data.value = "i32 " + node.getChildAt(0).data.intValue;
            } else {
                int cnt = 0;
                for (TreeNode<NodeData> child : node.children) {
                    if (child.data.name.equals("ConstInitVal")) {
                        cnt++;
                        visitConstInitArray(child);
                        node.data.value += child.data.value + ",";
                    }
                }
                int _fill_size = fillEmptySize(node, cnt);
                if (_fill_size >= 0)
                    node.data.value += "i32 0,".repeat(fillEmptySize(node, cnt));
                else error();
                node.data.value = StringUtils.chop(node.data.value);
            }
        } else {
            if (!analyzer.hasCerVal(node)) error();
        }
    }

    private void visitConstInitVal(TreeNode<NodeData> node) {
        if (node.children.size() == 1)
            visitConstInitValNonArray(node);
        else
            visitConstInitArray(node);
    }

    private void visitConstExp(TreeNode<NodeData> node) {
        visit(node.getChildAt(0));
        node.data.value = node.getChildAt(0).data.value;
        node.data.intValue = node.getChildAt(0).data.intValue;
    }

    private void visitVarValDef(TreeNode<NodeData> node) {
        String declName = node.getChildAt(0).data.value;
        Item declItem = analyzer.filVarValDef(node);

        if (declItem.blockId == 0) {
            if (node.children.size() == 1) {
                product += "@" + declName + " = dso_local global i32 0\n";
                declItem.intValue = 0;
            } else {
                visit(node.getChildAt(2));
                product += "@" + declName + " = dso_local global i32 "
                        + node.getChildAt(2).data.intValue + "\n";
                declItem.intValue = node.getChildAt(2).data.intValue;
            }
        } else {
            declItem.regId = regId;
            String decl = "%" + (regId++);
            product += decl + " = alloca " + declItem.vType + "\n";

            if (node.children.size() >= 3) {
                visit(node.getChildAt(2));
                if (declItem.blockId == 0) {
                    product += "store " + declItem.vType + " "
                            + node.getChildAt(2).data.value + ", "
                            + declItem.vType + "* @" + declName + "\n";
                } else {
                    product += "store " + declItem.vType + " "
                            + node.getChildAt(2).data.value + ", "
                            + declItem.vType + "* " + decl + "\n";
                }
                declItem.intValue = node.getChildAt(2).data.intValue;
            }
        }
        node.data.intValue = declItem.intValue;
    }

    private void visitVarArrayDef(TreeNode<NodeData> node) {
        Item declItem = analyzer.filVarArrayDef(node);
        String declName = node.getChildAt(0).data.value;

        boolean hasInitVal = false;
        int size = 1;
        for (TreeNode<NodeData> child : node.children) {
            if (child.data.name.equals("ConstExpr")) {
                visit(child);
                if (child.data.intValue < 0) error();
                declItem.arraySize.add(child.data.intValue);
                size *= child.data.intValue;
            }
            if (child.data.name.equals("InitVal"))
                hasInitVal = true;
        }
        this.arrayShape = declItem.arraySize;

        TreeNode<NodeData> initVal = node.getChildAt(node.children.size()-1);

        if (analyzer.curBlockId == 0) {
            product += "@" + declName + " = dso_local global "
                    + "[" + size + " x i32] ";
            if (hasInitVal) {
                visit(initVal);
                product += "[" + initVal.data.value + "]\n";
            } else {
                product += "zeroinitializer\n";
            }
        } else {
            // 局部数组初始化
            declItem.regId = regId;
            String decl = "%" + (regId++);
            String init_reg = "%" + (regId++);
            product += decl + " = alloca [" + size + " x i32]\n"
                    + init_reg + " = getelementptr [" + size + " x i32], ["
                    + size + " x i32]* " + decl + ", i32 0, i32 0\n"
                    + "call void @memset(i32* " + init_reg + ", i32 0, i32 "
                    + size * 4 + ")\n";

            if (hasInitVal) {
                visit(initVal);
//                product += "store i32 "
//                        + node.getChildAt(2).data.value + ", "
//                        + declItem.vType + "* " + decl + "\n";
//                declItem.intValue = node.getChildAt(2).data.intValue;
            }
        }
    }

    private void visitVarDef(TreeNode<NodeData> node) {
        boolean isArray = false;
        for (TreeNode<NodeData> child : node.children)
            if (child.data.value.equals("[")) {
                isArray = true;
                break;
            }
        if (isArray) visitVarArrayDef(node);
        else visitVarValDef(node);
    }

    private void visitInitVarVal(TreeNode<NodeData> node) {
        visit(node.getChildAt(0));
        node.data.value = node.getChildAt(0).data.value;
        node.data.intValue = node.getChildAt(0).data.intValue;
    }

    private int getInitValIndex(TreeNode<NodeData> node) {
        int idx = 0;
        for (TreeNode<NodeData> child : node.parent.children) {
            if (child.data.name.equals("InitVal")) {
                if (child == node)
                    break;
                idx++;
            }
        }
        return idx;
    }

    private int getElePos(TreeNode<NodeData> node) {
        ArrayList<Integer> idxs = new ArrayList<>();
        TreeNode<NodeData> temp = node;
        while (!temp.parent.getChildAt(0).data.name.equals("Ident")) {
            idxs.add(getInitValIndex(temp));
            temp = temp.parent;
        }
        int length = idxs.size();
        int res = idxs.get(length-1);
        for (int i = length-2; i >= 0; i--) {
            res = res * this.arrayShape.get(length-i-1) + idxs.get(i);
        }
        return res;
    }

    private int getAllSize(ArrayList<Integer> arrayShape) {
        int size = 1;
        for (var v : arrayShape) {
            size *= v;
        }
        return size;
    }

    private void visitInitArrayVal(TreeNode<NodeData> node) {
        if (analyzer.curBlockId == 0) {
            if (node.getChildAt(0).data.name.equals("Expr")) {
                if (initDepth(node) != this.arrayShape.size()) error();
                visit(node.getChildAt(0));
                node.data.value = "i32 " + node.getChildAt(0).data.intValue;
            } else {
                int cnt = 0;
                for (TreeNode<NodeData> child : node.children) {
                    if (child.data.name.equals("InitVal")) {
                        cnt++;
                        visitInitArrayVal(child);
                        node.data.value += child.data.value + ",";
                    }
                }
                int _fill_size = fillEmptySize(node, cnt);
                if (_fill_size >= 0)
                    node.data.value += "i32 0,".repeat(fillEmptySize(node, cnt));
                else error();
                node.data.value = StringUtils.chop(node.data.value);
            }
        } else {
            if (node.getChildAt(0).data.name.equals("Expr")) {
                if (initDepth(node) != this.arrayShape.size()) error();
                visit(node.getChildAt(0));

                String reg = "%" + (regId++);
                int size = getAllSize(this.arrayShape);
                product += reg + " = getelementptr [" + size
                        + " x i32],[" + size + " x i32]* %" + arrayRegId(node)
                        + ", i32 0, i32 " + getElePos(node) + "\n"
                        + "store i32 " + node.getChildAt(0).data.value
                        + ", i32* " + reg + "\n";
            } else {
                int cnt = 0;
                for (TreeNode<NodeData> child : node.children) {
                    if (child.data.name.equals("InitVal")) {
                        cnt++;
                        visitInitArrayVal(child);
                    }
                }
                int _fill_size = fillEmptySize(node, cnt);
                if (_fill_size < 0) error();
            }
        }
    }

    private void visitInitVal(TreeNode<NodeData> node) {
        if (analyzer.curBlockId == 0 && !analyzer.isConstInitVal(node))
            error();

        if (node.children.size() == 1) visitInitVarVal(node);
        else visitInitArrayVal(node);
    }

    /**
     * 处理 Stmt 节点的候选式 Block
     *
     * @param node Stmt节点
     */
    private void visitBlockStmt(TreeNode<NodeData> node) {
        visit(node.getChildAt(0));
    }

    /**
     * 处理 Stmt 节点的候选式 'break' ';'
     *
     * @param node Stmt节点
     */
    private void visitBreakStmt(TreeNode<NodeData> node) {
        int markId = stk.peek().marks.size();
        stk.peek().record(new Mark("break" + markId));
        product += "break" + markId;
    }

    /**
     * 处理 Stmt 节点的候选式 'continue' ';'
     *
     * @param node Stmt节点
     */
    private void visitContinueStmt(TreeNode<NodeData> node) {
        int markId = stk.peek().marks.size();
        stk.peek().record(new Mark("continue" + markId));
        product += "continue" + markId;
    }

    /**
     * 处理 Stmt 节点的候选式 [Exp] ';'
     *
     * @param node Stmt节点
     */
    private void visitExprStmt(TreeNode<NodeData> node) {
        visit(node.getChildAt(0));
        node.data.value = node.getChildAt(0).data.value;
        node.data.intValue = node.getChildAt(0).data.intValue;
    }

    /**
     * 处理 Stmt 节点的候选式 'return' Exp ';'
     *
     * @param node Stmt节点
     */
    private void visitRetExpr(TreeNode<NodeData> node) {
        visit(node.getChildAt(1));
        product += "ret i32 " + node.getChildAt(1).data.value + "\n";
    }

    /**
     * 处理 Stmt 节点的候选式 LVal '=' Exp ';'
     *
     * @param node Stmt节点
     */
    private void visitAlignExpr(TreeNode<NodeData> node) {
        visit(node.getChildAt(0));
        String val = node.getChildAt(0).data.value;
        Item valItem = symTable.getItem(val);
        visit(node.getChildAt(2));
        // 全局变量和局部变量的赋值不同，前者为 @，后者为 %
        if (valItem.blockId == 0) {
            product += "store " + valItem.vType + " "
                    + node.getChildAt(2).data.value + ", "
                    + valItem.vType + "* @" + val + "\n";
        } else {
            product += "store " + valItem.vType + " "
                    + node.getChildAt(2).data.value + ", "
                    + valItem.vType + "* " + "%" + valItem.regId + "\n";
        }
    }

    /**
     * 处理 Stmt 节点的候选式 'if' '(' Cond ')' Stmt
     *
     * @param node Stmt节点
     */
    private void visitIfStmt(TreeNode<NodeData> node) {
        visit(node.getChildAt(2));
        product += "br i1 " + node.getChildAt(2).data.value
                + ", label ";
        int len = product.length();
        int reg_1 = (regId++);
        product += "\n" + reg_1 + ":\n";
        visit(node.getChildAt(4));
        int reg_2 = (regId++);
        if (!hasRet())
            product += "br label %" + reg_2 + "\n";
        product += "\n" + reg_2 + ":\n";
        insRecord(len, "%" + reg_1 + ", label %" + reg_2 + "\n");
    }

    /**
     * 处理 Stmt 节点的候选式 'if' '(' Cond ')' Stmt 'else' Stmt
     *
     * @param node Stmt节点
     */
    private void visitIfElseStmt(TreeNode<NodeData> node) {
        visit(node.getChildAt(2));
        product += "br i1 " + node.getChildAt(2).data.value + ", label ";
        int len_1 = product.length();
        int reg_1 = (regId++);
        product += "\n" + reg_1 + ":\n";
        visit(node.getChildAt(4));
        int len_2 = product.length();
        int reg_2 = (regId++);
        product += "\n" + reg_2 + ":\n";
        visit(node.getChildAt(6));
        int reg_3 = (regId++);
        if (!hasRet())
            product += "br label %" + reg_3 + "\n";
        product += "\n" + reg_3 + ":\n";
        if (!hasRet(len_2))
            insRecord(len_2, "br label %" + reg_3 + "\n");
        insRecord(len_1, "%" + reg_1 + ", label %" + reg_2 + "\n");
    }

    /**
     * 处理 Stmt 节点的候选式 'while' '(' Cond ')' Stmt
     *
     * @param node Stmt节点
     */
    private void visitWhileStmt(TreeNode<NodeData> node) {
        stk.push(new Recorder());

        int reg_1 = (regId++);
        product += "br label %" + reg_1 + "\n";
        product += "\n" + reg_1 + ":\n";
        visit(node.getChildAt(2));
        product += "br i1 " + node.getChildAt(2).data.value
                + ", label ";
        int len = product.length();
        int reg_2 = (regId++);
        product += "\n" + reg_2 + ":\n";
        visit(node.getChildAt(4));

        if (!hasRet())
            product += "br label %" + reg_1 + "\n";
        int reg_3 = (regId++);
        product += "\n" + reg_3 + ":\n";
        insRecord(len, "%" + reg_2 + ", label %" + reg_3 + "\n");

        for (var mark : stk.peek().marks) {
            if (mark.tag.startsWith("break")) {
                repRecord(mark.tag, "br label %" + reg_3 + "\n");
            } else if (mark.tag.startsWith("continue")) {
                repRecord(mark.tag, "br label %" + reg_1 + "\n");
            }
        }
        stk.pop();
    }

    private void visitStmt(TreeNode<NodeData> node) {
        switch (node.children.size()) {
            case 1 -> visitBlockStmt(node);
            case 2 -> {
                switch (node.getChildAt(0).data.name) {
                    case "Break" -> visitBreakStmt(node);
                    case "Continue" -> visitContinueStmt(node);
                    case "Expr" -> visitExprStmt(node);
                }
            }
            case 3 -> {
                if (node.getChildAt(0).data.name.equals("Return"))
                    visitRetExpr(node);
            }
            case 4 -> visitAlignExpr(node);
            case 5 -> {
                switch (node.getChildAt(0).data.name) {
                    case "If" -> visitIfStmt(node);
                    case "While" -> visitWhileStmt(node);
                }
            }
            case 7 -> visitIfElseStmt(node);
        }
    }

    private void visitCond(TreeNode<NodeData> node) {
        for (TreeNode<NodeData> child : node.children)
            visit(child);
        node.data.value = node.getChildAt(0).data.value;
    }

    private void visitExpr(TreeNode<NodeData> node) {
        for (TreeNode<NodeData> child : node.children)
            visit(child);
        node.data.value = node.getChildAt(0).data.value;
        node.data.intValue = node.getChildAt(0).data.intValue;
    }

    private void visitOperaExpr(TreeNode<NodeData> node) {
        boolean computed = false;
        if (analyzer.curBlockId == 0 || analyzer.hasCerVal(node)) {
            computed = true;
            visit(node.getChildAt(0));
            // 计算实际值
            Integer v1 = node.getChildAt(0).data.intValue;
            Integer v2 = v1;
            if (v1 == null) return;
            for (int i = 2; i < node.children.size(); i+=2) {
                visit(node.getChildAt(i));
                Integer v_temp = node.getChildAt(i).data.intValue;
                if (v_temp == null) return;
                switch (node.getChildAt(i-1).data.value) {
                    case "+" -> v2 = v1 + v_temp;
                    case "-" -> v2 = v1 - v_temp;
                    case "*" -> v2 = v1 * v_temp;
                    case "/" -> v2 = v1 / v_temp;
                    case "%" -> v2 = v1 % v_temp;
                }
                v1 = v2;
            }
            node.data.intValue = v2;
        }
        if (analyzer.curBlockId > 0) {
            if (!computed) visit(node.getChildAt(0));
            String value_1 = node.getChildAt(0).data.value;
            String value_2 = node.getChildAt(0).data.value;
            for (int i = 2; i < node.children.size(); i+=2) {
                if (!computed) visit(node.getChildAt(i));
                value_2 = "%" + (regId++);
                product += value_2 + " = "
                        + flagOfOpera(node.getChildAt(i-1).data.value)
                        + " i32 " + value_1
                        + ", " + node.getChildAt(i).data.value + "\n";
                value_1 = value_2;
            }
            node.data.value = value_2;
        }
    }

    private void visitCmpExpr(TreeNode<NodeData> node) {
        visit(node.getChildAt(0));
        String value_1 = node.getChildAt(0).data.value;
        String value_2 = node.getChildAt(0).data.value;
        int childCnt = node.children.size();
        if (childCnt == 1 && node.data.name.equals("RelExpr") && node.parent.children.size() == 1) {
            value_2 = "%" + (regId++);
            product += value_2 + " = icmp ne i32 "
                    + value_1
                    + ", 0\n";
        }
        for (int i = 2; i < childCnt; i+=2) {
            visit(node.getChildAt(i));
            value_2 = "%" + (regId++);
            product += value_2 + " = icmp "
                    + flagOfOpera(node.getChildAt(i-1).data.value)
                    + " i32 " + value_1
                    + ", " + node.getChildAt(i).data.value + "\n";
            value_1 = value_2;
        }
        node.data.value = value_2;
    }

    private void visitLogicExpr(TreeNode<NodeData> node) {
        visit(node.getChildAt(0));
        String value_1 = node.getChildAt(0).data.value;
        String value_2 = node.getChildAt(0).data.value;
        for (int i = 2; i < node.children.size(); i+=2) {
            visit(node.getChildAt(i));
            value_2 = "%" + (regId++);
            product += value_2 + " = "
                    + flagOfOpera(node.getChildAt(i-1).data.value)
                    + " i1 " + value_1
                    + ", " + node.getChildAt(i).data.value + "\n";
            value_1 = value_2;
        }
        node.data.value = value_2;
    }

    /**
     * 处理 UnaryExpr 节点的候选式 PrimaryExp
     *
     * @param node UnaryExpr节点
     */
    private void visitPrimUE(TreeNode<NodeData> node) {
        visit(node.getChildAt(0));
        node.data.value = node.getChildAt(0).data.value;
        node.data.intValue = node.getChildAt(0).data.intValue;
    }

    /**
     * 处理 UnaryExpr 节点的候选式 Ident '(' [FuncRParams] ')'
     *
     * @param node UnaryExpr节点
     */
    private void visitFuncUE(TreeNode<NodeData> node) {
        String funcName = node.getChildAt(0).data.value;
        Item funcItem = symTable.getItem(funcName);
        if (funcItem.vType == Item.ValueType.INT) {
            String reg = "%" + (regId++);
            node.data.value = reg;
            product += reg + " = ";
        }
        visit(node.getChildAt(2));
        product += "call " + funcItem.vType
                + " @" + funcName
                + "("
                + node.getChildAt(2).data.value
                + ")\n";
    }

    /**
     * 处理 UnaryExpr 节点的候选式 Ident '(' ')'
     *
     * @param node UnaryExpr节点
     */
    private void visitNoParamFuncUE(TreeNode<NodeData> node) {
        String funcName = node.getChildAt(0).data.value;
        Item funcItem = symTable.getItem(funcName);
        if (funcItem.vType == Item.ValueType.INT) {
            String reg = "%" + (regId++);
            node.data.value = reg;
            product += reg + " = ";
        }
        product += "call " + funcItem.vType.toString()
                + " @" + funcName
                + "("
                + ")\n";
    }

    /**
     * 处理 UnaryExpr 节点的候选式 UnaryOp UnaryExp
     *
     * @param node UnaryExpr节点
     */
    private void visitSignValUE(TreeNode<NodeData> node) {
        visitUnaryExpr(node.getChildAt(1));
        if (analyzer.curBlockId > 0)
            node.data.value = "%" + (regId++);
        String opera = node.getChildAt(0).getChildAt(0).data.value;
        switch (opera) {
            case "+", "-" -> {
                if (analyzer.curBlockId > 0)
                    product += node.data.value + " = "
                            + flagOfOpera(opera)
                            + " i32 0, "
                            + node.getChildAt(1).data.value + "\n";
                // 计算实际值
                Integer v = node.getChildAt(1).data.intValue;
                if (v != null) {
                    if (opera.equals("+"))
                        node.data.intValue = node.getChildAt(1).data.intValue;
                    else node.data.intValue = - node.getChildAt(1).data.intValue;
                }

            }
            case "!" -> {
                product += node.data.value + " = icmp eq i32 0, "
                        + node.getChildAt(1).data.value + "\n";
                String newValue = "%" + (regId++);
                product += newValue + " = zext i1 "
                        + node.data.value
                        + " to i32\n";
                node.data.value = newValue;
            }
        }
    }

    private void visitUnaryExpr(TreeNode<NodeData> node) {
        if (node.children.size() >= 3 && !analyzer.isFuncCallValid(node))
            error();
        switch (node.children.size()) {
            case 1 -> visitPrimUE(node);
            case 2 -> visitSignValUE(node);
            case 3 -> visitNoParamFuncUE(node);
            case 4 -> visitFuncUE(node);
        }
    }

    private void visitPrimExpr(TreeNode<NodeData> node) {
        int childCnt = node.children.size();
        if (childCnt == 1) {
            if (node.getChildAt(0).data.name.equals("Number")) {
                node.data.value = node.getChildAt(0).data.value;
                node.data.intValue = Integer.parseInt(node.data.value);
            } else if (node.getChildAt(0).data.name.equals("Lval")) {
                visit(node.getChildAt(0));
                node.data.value = node.getChildAt(0).data.value;
                node.data.intValue = node.getChildAt(0).data.intValue;
            }
        } else {
            visit(node.getChildAt(1));
            node.data.value = node.getChildAt(1).data.value;
            node.data.intValue = node.getChildAt(1).data.intValue;
        }
    }

    private void visitFuncRParams(TreeNode<NodeData> node) {
        int childCnt = node.children.size();
        String value = "";
        for (int i = 0; i < childCnt; i += 2) {
            if (i >= 1) value += ",";
            visit(node.getChildAt(i));
            value += "i32 " + node.getChildAt(i).data.value;
        }
        node.data.value = value;
    }

    private void visitNoArrayRLval(TreeNode<NodeData> node) {
        String val = node.getChildAt(0).data.value;
        Item valItem = symTable.getItem(val);
        // 计算值
        if (analyzer.curBlockId == 0 || valItem.hasCerVal)
            node.data.intValue = valItem.intValue;
        if (analyzer.curBlockId > 0) {
            String rLval = "%" + (regId++);
            node.data.value = rLval;
            product += rLval + " = load " + valItem.vType
                    + ", " + valItem.vType + "* ";
            if (valItem.blockId == 0)
                product += "@" + valItem.name + "\n";
            else
                product += "%" + valItem.regId + "\n";
        }
    }

    private void visitArrayRLval(TreeNode<NodeData> node) {

    }

    private void visitLval(TreeNode<NodeData> node) {
        if (node.children.size() == 1) {
            if (!analyzer.isNoArrayLvalValid(node)) error();
            if (node.parent.data.name.equals("Stmt")) {
                // Stmt -> Lval = Exp ; --- Lval -> Ident
                node.data.value = node.getChildAt(0).data.value;
            } else {
                visitNoArrayRLval(node);
            }
        } else {
            if (!analyzer.isArrayLvalValid(node))
                error();
            if (node.parent.data.name.equals("Stmt")) {
                // Stmt -> Lval = Exp ; --- Lval -> Ident {'[' Exp ']'}
            } else {
                visitArrayRLval(node);
            }
        }
    }


    /**
     * 将运算符转换为llvm标识
     *
     * @param opera 运算符
     * @return llvm运算符
     */
    private String flagOfOpera(String opera) {
        return switch (opera) {
            case "-" -> "sub";
            case "+" -> "add";
            case "*" -> "mul";
            case "/" -> "sdiv";
            case "%" -> "srem";
            case "<" -> "slt";
            case ">" -> "sgt";
            case "<=" -> "sle";
            case ">=" -> "sge";
            case "==" -> "eq";
            case "!=" -> "ne";
            case "&&" -> "and";
            case "||" -> "or";
            default -> "error";
        };
    }

    /**
     * 判断product字符串前length个字符的子串是否以ret结束
     *
     * @param length 前length个字符
     * @return 是否以ret结束
     */
    private boolean hasRet(int length) {
        String[] array = product.substring(0,length).split("\n");
        return array[array.length-1].startsWith("ret");
    }

    /**
     * 判断product字符串是否以ret结束
     *
     * @return 是否以ret结束
     */
    private boolean hasRet() {
        String[] array = product.split("\n");
        return array[array.length-1].startsWith("ret");
    }

    /**
     * 向 product 的 pos 位置插入 content
     *
     * @param pos product位置
     * @param content 字符串内容
     */
    private void insRecord(int pos, String content) {
        StringBuilder buffer = new StringBuilder(product);
        product = buffer.insert(pos, content).toString();
    }

    private void repRecord(String src, String target) {
        product = product.replace(src, target);
    }

}
