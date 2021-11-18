package compiler;

import compiler.parser.ast.NodeData;
import compiler.parser.ast.TreeNode;
import compiler.semantics.Analyzer;
import compiler.semantics.symtable.Item;
import compiler.semantics.symtable.SymTable;


public class Generator {

    private TreeNode<NodeData> ast;
    private int regId;
    private String product;

    /**
     * 在生成器中，需要边遍历边填入新符号表
     */
    private SymTable symTable;
    private Analyzer analyzer;

    public Generator(TreeNode<NodeData> tree) {
        this.ast = tree;
        this.regId = 1;
        this.product = """
                declare i32 @getint()
                declare void @putint(i32)
                declare i32 @getch()
                declare void @putch(i32)
                """;
        this.symTable = new SymTable();
        this.analyzer = new Analyzer(ast, symTable);
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
        analyzer.handleBlock(node);
        analyzer.register(node);
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
            case "AddExpr", "MulExpr" -> genOperaExpr(node);
            case "UnaryExpr" -> genUnaryExpr(node);
            case "PrimaryExpr" -> genPrimExpr(node);
            case "FuncRParams" -> genFuncRParams(node);
            case "Lval" -> genRLval(node);
            case "Cond" -> genCond(node);
            case "RelExpr", "EqExpr" -> genCmpExpr(node);
            case "LAndExpr", "LOrExpr" -> genLogicExpr(node);
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
        for (TreeNode<NodeData> child : node.children)
            generate(child);
    }

    private void genConstDef(TreeNode<NodeData> node) {
        String declName = node.getChildAt(0).data.value;
        Item declItem = symTable.getItem(declName);
        // 全局变量求值，局部变量声明不求值
        if (declItem.blockId == 0) {
            generate(node.getChildAt(2));
            product += "@" + declName + " = dso_local global i32 "
                    + node.getChildAt(2).data.intValue + "\n";
        } else {
            declItem.regId = regId;
            String decl = "%" + (regId++);
            product += decl + " = alloca " + declItem.vType + "\n";
            generate(node.getChildAt(2));
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
        declItem.intValue = node.getChildAt(2).data.intValue;
    }

    private void genConstInitVal(TreeNode<NodeData> node) {
        generate(node.getChildAt(0));
        node.data.value = node.getChildAt(0).data.value;
        node.data.intValue = node.getChildAt(0).data.intValue;
    }

    private void genConstExp(TreeNode<NodeData> node) {
        generate(node.getChildAt(0));
        node.data.value = node.getChildAt(0).data.value;
        node.data.intValue = node.getChildAt(0).data.intValue;
    }

    private void genVarDef(TreeNode<NodeData> node) {
        String declName = node.getChildAt(0).data.value;
        Item declItem = symTable.getItem(declName);

        if (declItem.blockId == 0) {
            if (node.children.size() == 1) {
                product += "@" + declName + " = dso_local global i32 0\n";
                declItem.intValue = 0;
            } else {
                generate(node.getChildAt(2));
                System.out.println(node);
                product += "@" + declName + " = dso_local global i32 "
                        + node.getChildAt(2).data.intValue + "\n";
                declItem.intValue = node.getChildAt(2).data.intValue;
            }
        } else {
            declItem.regId = regId;
            String decl = "%" + (regId++);
            product += decl + " = alloca " + declItem.vType + "\n";

            if (node.children.size() >= 3) {
                generate(node.getChildAt(2));
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
    }

    private void genInitVal(TreeNode<NodeData> node) {
        generate(node.getChildAt(0));
        node.data.value = node.getChildAt(0).data.value;
        node.data.intValue = node.getChildAt(0).data.intValue;
    }

    private void genStmt(TreeNode<NodeData> node) {
        switch (node.children.size()) {
            case 1 -> {
                generate(node.getChildAt(0));
            }
            case 2 -> {
                generate(node.getChildAt(0));
                node.data.value = node.getChildAt(0).data.value;
                node.data.intValue = node.getChildAt(0).data.intValue;
            }
            case 3 -> {
                generate(node.getChildAt(1));
                product += "ret i32 " + node.getChildAt(1).data.value + "\n";
            }
            case 4 -> {
                String val = node.getLeaves().get(0).data.value;
                Item valItem = symTable.getItem(val);
                generate(node.getChildAt(2));
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
            case 5 -> {
                generate(node.getChildAt(2));
                product += "br i1 " + node.getChildAt(2).data.value
                        + ", label ";
                int len = product.length();
                int reg_1 = (regId++);
                product += "\n" + reg_1 + ":\n";
                generate(node.getChildAt(4));
                int reg_2 = (regId++);
                if (!hasRet())
                    product += "br label %" + reg_2 + "\n";
                product += "\n" + reg_2 + ":\n";
                StringBuilder buffer = new StringBuilder(product);
                product = buffer.insert(len,
                        "%" + reg_1 + ", label %" + reg_2 + "\n").toString();
            }
            case 7 -> {
                generate(node.getChildAt(2));
                product += "br i1 " + node.getChildAt(2).data.value + ", label ";
                int len_1 = product.length();
                int reg_1 = (regId++);
                product += "\n" + reg_1 + ":\n";
                generate(node.getChildAt(4));
                int len_2 = product.length();
                int reg_2 = (regId++);
                product += "\n" + reg_2 + ":\n";
                generate(node.getChildAt(6));
                int reg_3 = (regId++);
                if (!hasRet())
                    product += "br label %" + reg_3 + "\n";
                product += "\n" + reg_3 + ":\n";
                StringBuilder buffer = new StringBuilder(product);
                if (!hasRet(len_2))
                    buffer.insert(len_2,
                            "br label %" + reg_3 + "\n");
                buffer.insert(len_1,
                        "%" + reg_1 + ", label %" + reg_2 + "\n");
                product = buffer.toString();
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
        node.data.intValue = node.getChildAt(0).data.intValue;
    }

    private void genOperaExpr(TreeNode<NodeData> node) {
        if (analyzer.curBlockId == 0) {
            generate(node.getChildAt(0));
            // 计算实际值
            Integer v1 = node.getChildAt(0).data.intValue;
            Integer v2 = v1;
            if (v1 == null) return;
            for (int i = 2; i < node.children.size(); i+=2) {
                generate(node.getChildAt(i));
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
        } else {
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
    }

    private void genCmpExpr(TreeNode<NodeData> node) {
        generate(node.getChildAt(0));
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

    private void genLogicExpr(TreeNode<NodeData> node) {
        generate(node.getChildAt(0));
        String value_1 = node.getChildAt(0).data.value;
        String value_2 = node.getChildAt(0).data.value;
        for (int i = 2; i < node.children.size(); i+=2) {
            generate(node.getChildAt(i));
            value_2 = "%" + (regId++);
            product += value_2 + " = "
                    + flagOfOpera(node.getChildAt(i-1).data.value)
                    + " i1 " + value_1
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
                node.data.intValue = node.getChildAt(0).data.intValue;
            }
            case 2 -> {
                genUnaryExpr(node.getChildAt(1));
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
                node.data.intValue = Integer.parseInt(node.data.value);
            } else if (node.getChildAt(0).data.name.equals("Lval")) {
                generate(node.getChildAt(0));
                node.data.value = node.getChildAt(0).data.value;
                node.data.intValue = node.getChildAt(0).data.intValue;
            }
        } else {
            generate(node.getChildAt(1));
            node.data.value = node.getChildAt(1).data.value;
            node.data.intValue = node.getChildAt(1).data.intValue;
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
        if (analyzer.curBlockId == 0) {
            String val = node.getChildAt(0).data.value;
            Item valItem = symTable.getItem(val);
            // 计算值
            if (valItem.hasCerVal)
                node.data.intValue = valItem.intValue;
        } else {
            String rLval = "%" + (regId++);
            node.data.value = rLval;
            String val = node.getChildAt(0).data.value;
            Item valItem = symTable.getItem(val);
            // 能计算值则计算值
            if (valItem.hasCerVal)
                node.data.intValue = valItem.intValue;
            product += rLval + " = load " + valItem.vType
                    + ", " + valItem.vType + "* ";
            if (valItem.blockId == 0)
                product += "@" + valItem.name + "\n";
            else
                product += "%" + valItem.regId + "\n";
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

}
