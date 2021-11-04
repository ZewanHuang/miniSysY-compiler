package compiler;

import compiler.parser.ast.NodeData;
import compiler.parser.ast.TreeNode;
import compiler.semantics.symtable.Item;
import compiler.semantics.symtable.SymTable;

public class Generator {

    private TreeNode<NodeData> ast;
    private SymTable symTable;
    private int regId;
    private String product;

    public Generator(TreeNode<NodeData> tree, SymTable symTable) {
        this.ast = tree;
        this.symTable = symTable;
        this.regId = 1;
        this.product = """
                declare i32 @getint()
                declare void @putint(i32)
                declare i32 @getch()
                declare void @putch(i32)
                """;
    }

    /**
     * 生成 llvm 并返回生成代码
     *
     * @return llvm字符串形式
     */
    public String generate() {
        generate(ast);
        return product;
    }

    /**
     * 处理经语义分析后的语法树，生成 llvm 代码记录于 product 中
     *
     * @param node 节点
     */
    private void generate(TreeNode<NodeData> node) {
        switch (node.data.name) {
            case "FuncDef" -> genFuncDef(node);
            case "Block" -> genBlock(node);
            case "ConstDef" -> genConstDef(node);
            case "ConstInitVal" -> genConstInitVal(node);
            case "ConstExp" -> genConstExp(node);
            case "VarDef" -> genVarDef(node);
            case "InitVal" -> genInitVal(node);
            case "Stmt" -> genStmt(node);
            case "Expr" -> genExpr(node);
            case "AddExpr" -> genAddExpr(node);
            case "MulExpr" -> genMulExpr(node);
            case "UnaryExpr" -> genUnaryExpr(node);
            case "PrimaryExpr" -> genPrimExpr(node);
            case "FuncRParams" -> genFuncRParams(node);
            case "Lval" -> genRLval(node);
            case "Cond" -> genCond(node);
            case "RelExpr", "EqExpr", "LAndExpr", "LOrExpr" -> genCmpExpr(node);
            default -> {
                for (TreeNode<NodeData> child : node.children)
                    generate(child);
            }
        }
    }

    /**
     * 当前节点为 FuncDef 节点时，添加函数定义语句
     *
     * @param node FuncDef节点
     */
    private void genFuncDef(TreeNode<NodeData> node) {
        String funcName = node.getChildAt(1).data.value;
        Item funcItem = symTable.getItem(funcName);
        product += "define dso_local " + funcItem.vType + " @" + funcName + "() {\n";
        generate(node.getChildAt(4));
        product += "}";
    }

    private void genBlock(TreeNode<NodeData> node) {
        int i = 1;
        while (i <= node.children.size()-2)
            generate(node.getChildAt(i++));
    }

    private void genConstDef(TreeNode<NodeData> node) {
        String declName = node.getChildAt(0).data.value;
        Item declItem = symTable.getItem(declName);
        declItem.regId = regId;
        String decl = "%" + (regId++);
        product += decl + " = alloca " + declItem.vType + "\n";
        generate(node.getChildAt(2));
        product += "store " + declItem.vType + " "
                + node.getChildAt(2).data.value + ", "
                + declItem.vType + "* " + decl + "\n";
    }

    private void genConstInitVal(TreeNode<NodeData> node) {
        generate(node.getChildAt(0));
        node.data.value = node.getChildAt(0).data.value;
    }

    private void genConstExp(TreeNode<NodeData> node) {
        generate(node.getChildAt(0));
        node.data.value = node.getChildAt(0).data.value;
    }

    private void genVarDef(TreeNode<NodeData> node) {
        String declName = node.getChildAt(0).data.value;
        Item declItem = symTable.getItem(declName);
        declItem.regId = regId;
        String decl = "%" + (regId++);
        product += decl + " = alloca " + declItem.vType + "\n";

        if (node.children.size() >= 3) {
            generate(node.getChildAt(2));
            product += "store " + declItem.vType + " "
                    + node.getChildAt(2).data.value + ", "
                    + declItem.vType + "* " + decl + "\n";
        }
    }

    private void genInitVal(TreeNode<NodeData> node) {
        generate(node.getChildAt(0));
        node.data.value = node.getChildAt(0).data.value;
    }

    private void genStmt(TreeNode<NodeData> node) {
        switch (node.children.size()) {
            case 1 -> {
                generate(node.getChildAt(0));
            }
            case 2 -> {
                generate(node.getChildAt(0));
                node.data.value = node.getChildAt(0).data.value;
            }
            case 3 -> {
                generate(node.getChildAt(1));
                product += "ret i32 " + node.getChildAt(1).data.value + "\n";
            }
            case 4 -> {
                String val = node.getLeaves().get(0).data.value;
                Item valItem = symTable.getItem(val);
                generate(node.getChildAt(2));
                product += "store " + valItem.vType + " "
                        + node.getChildAt(2).data.value + ", "
                        + valItem.vType + "* " + "%" + valItem.regId + "\n";
            }
            case 5 -> {
                generate(node.getChildAt(2));
                product += "br i1 " + node.getChildAt(2).data.value
                        + ", label ";
                int len = product.length();
                int reg_1 = (regId++);
                product += reg_1 + ":\n";
                generate(node.getChildAt(4));
                int reg_2 = (regId++);
                product += "br label %" + reg_2 + "\n";
                product += reg_2 + ":\n";
                StringBuilder buffer = new StringBuilder(product);
                product = buffer.insert(len,
                        "%" + reg_1 + ", label %" + reg_2 + "\n").toString();
            }
        }
    }

    private void genCond(TreeNode<NodeData> node) {
        for (TreeNode<NodeData> child : node.children)
            generate(child);
        node.data.value = node.getChildAt(0).data.value;
    }

    private void genExpr(TreeNode<NodeData> node) {
        for (TreeNode<NodeData> child : node.children)
            generate(child);
        node.data.value = node.getChildAt(0).data.value;
    }

    private void genAddExpr(TreeNode<NodeData> node) {
        generate(node.getChildAt(0));
        String value_1 = node.getChildAt(0).data.value;
        String value_2 = node.getChildAt(0).data.value;
        for (int i = 2; i < node.children.size(); i+=2) {
            generate(node.getChildAt(i));
            value_2 = "%" + (regId++);
            product += value_2 + " = "
                    + flagOfOpera(node.getChildAt(i-1).data.value)
                    + " i32 " + value_1
                    + ", " + node.getChildAt(i).data.value + "\n";
            value_1 = value_2;
        }
        node.data.value = value_2;
    }

    private void genMulExpr(TreeNode<NodeData> node) {
        generate(node.getChildAt(0));
        String value_1 = node.getChildAt(0).data.value;
        String value_2 = node.getChildAt(0).data.value;
        for (int i = 2; i < node.children.size(); i+=2) {
            generate(node.getChildAt(i));
            value_2 = "%" + (regId++);
            product += value_2 + " = "
                    + flagOfOpera(node.getChildAt(i-1).data.value)
                    + " i32 " + value_1
                    + ", " + node.getChildAt(i).data.value + "\n";
            value_1 = value_2;
        }
        node.data.value = value_2;
    }

    private void genCmpExpr(TreeNode<NodeData> node) {
        generate(node.getChildAt(0));
        String value_1 = node.getChildAt(0).data.value;
        String value_2 = node.getChildAt(0).data.value;
        for (int i = 2; i < node.children.size(); i+=2) {
            generate(node.getChildAt(i));
            value_2 = "%" + (regId++);
            product += value_2 + " = icmp "
                    + flagOfOpera(node.getChildAt(i-1).data.value)
                    + " i32 " + value_1
                    + ", " + node.getChildAt(i).data.value + "\n";
            value_1 = value_2;
        }
        node.data.value = value_2;
    }

    private void genUnaryExpr(TreeNode<NodeData> node) {
        switch (node.children.size()) {
            case 1 -> {
                genPrimExpr(node.getChildAt(0));
                node.data.value = node.getChildAt(0).data.value;
            }
            case 2 -> {
                genUnaryExpr(node.getChildAt(1));
                node.data.value = "%" + (regId++);
                product += node.data.value + " = "
                        + flagOfOpera(node.getChildAt(0).getChildAt(0).data.value)
                        + " i32 0, "
                        + node.getChildAt(1).data.value + "\n";
            }
            case 3 -> {
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
            case 4 -> {
                String funcName = node.getChildAt(0).data.value;
                Item funcItem = symTable.getItem(funcName);
                if (funcItem.vType == Item.ValueType.INT) {
                    String reg = "%" + (regId++);
                    node.data.value = reg;
                    product += reg + " = ";
                }
                generate(node.getChildAt(2));
                product += "call " + funcItem.vType
                        + " @" + funcName
                        + "("
                        + node.getChildAt(2).data.value
                        + ")\n";
            }
        }
    }

    private void genPrimExpr(TreeNode<NodeData> node) {
        int childCnt = node.children.size();
        if (childCnt == 1) {
            if (node.getChildAt(0).data.name.equals("Number")) {
                node.data.value = node.getChildAt(0).data.value;
            } else if (node.getChildAt(0).data.name.equals("Lval")) {
                generate(node.getChildAt(0));
                node.data.value = node.getChildAt(0).data.value;
            }
        } else {
            generate(node.getChildAt(1));
            node.data.value = node.getChildAt(1).data.value;
        }
    }

    private void genFuncRParams(TreeNode<NodeData> node) {
        int childCnt = node.children.size();
        String value = "";
        for (int i = 0; i < childCnt; i += 2) {
            if (i >= 1) value += ",";
            generate(node.getChildAt(i));
            value += "i32 " + node.getChildAt(i).data.value;
        }
        node.data.value = value;
    }

    private void genRLval(TreeNode<NodeData> node) {
        String rLval = "%" + (regId++);
        node.data.value = rLval;
        String val = node.getChildAt(0).data.value;
        Item valItem = symTable.getItem(val);
        product += rLval + " = load " + valItem.vType
                + ", " + valItem.vType + "* "
                + "%" + valItem.regId + "\n";
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

}
