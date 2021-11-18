package compiler.parser;

import compiler.lexer.Token;
import compiler.parser.ast.NodeData;
import compiler.parser.ast.TreeNode;

import java.util.ArrayList;

import static compiler.exception.CompileException.error;

public class Descender {

    public ArrayList<Token> tokens;     /* 递归下降处理的 tokens */
    private int tokenId;                /* 当前 token 的序号 */
    private Token curToken;             /* 当前 token */
    private TreeNode<NodeData> ast;     /* 语法树 */

    /**
     * 构造函数
     *
     * @param tokens 递归下降处理的 tokens
     */
    public Descender(ArrayList<Token> tokens) {
        this.tokens = tokens;
        this.tokenId = 0;
        nextToken();
    }

    public TreeNode<NodeData> buildAST() {
        ast = new TreeNode<>(new NodeData("CompUnit"));
        compUnit();
        if (!curToken.isEmpty()) error();
        return ast.getRoot();
    }

    /**
     * 获取下一个 token 存储于 curToken
     */
    private void nextToken() {
        curToken = tokens.get(tokenId++);
    }

    /**
     * 以下函数为下降递归子程序，在下降中构建语法树
     */

    private void compUnit() {
        TreeNode<NodeData> node = ast;
        while (!curToken.isEmpty()) {
            if (curToken.equals("const")) {
                ast = node.addChild(new NodeData("Decl"));
                decl();
            } else if (curToken.equals("void")) {
                ast = node.addChild(new NodeData("FuncDef"));
                funcDef();
            } else if (curToken.equals("int")) {
                try {
                    if (tokens.get(tokenId+1).equals("(")) {
                        ast = node.addChild(new NodeData("FuncDef"));
                        funcDef();
                    } else {
                        ast = node.addChild(new NodeData("Decl"));
                        decl();
                    }
                } catch (Exception e) {
                    error();
                }
            }
        }
    }

    private void funcDef() {
        TreeNode<NodeData> node = ast;
        ast = node.addChild(new NodeData("FuncType"));
        funcType();
        if (curToken.isIdent()) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            if (curToken.equals("(")) {
                ast = node.addChild(new NodeData(curToken));
                nextToken();
                if (curToken.equals(")")) {
                    ast = node.addChild(new NodeData(curToken));
                    nextToken();
                    ast = node.addChild(new NodeData("Block"));
                    block();
                } else {
                    ast = node.addChild(new NodeData("FuncFParams"));
                    funcFParams();
                    if (curToken.equals(")")) {
                        ast = node.addChild(new NodeData(curToken));
                        nextToken();
                        ast = node.addChild(new NodeData("Block"));
                        block();
                    } else error();
                }
            } else error();
        }
    }

    private void funcFParams() {
        TreeNode<NodeData> node = ast;
        ast = node.addChild(new NodeData("FuncFParam"));
        funcFParam();
        while (curToken.equals(",")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            ast = node.addChild(new NodeData("FuncFParam"));
            funcFParam();
        }
    }

    private void funcFParam() {
        TreeNode<NodeData> node = ast;
        ast = node.addChild(new NodeData("BType"));
        btype();
        if (curToken.isIdent()) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            if (curToken.equals("[")) {
                ast = node.addChild(new NodeData(curToken));
                nextToken();
                if (curToken.equals("]")) {
                    ast = node.addChild(new NodeData(curToken));
                    nextToken();
                    while (curToken.equals("[")) {
                        ast = node.addChild(new NodeData(curToken));
                        nextToken();
                        ast = node.addChild(new NodeData("Expr"));
                        expr();
                        if (curToken.equals("]")) {
                            ast = node.addChild(new NodeData(curToken));
                            nextToken();
                        } else error();
                    }
                } else error();
            }
        } else error();
    }

    private void funcType() {
        TreeNode<NodeData> node = ast;
        if (curToken.equals("int")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
        } else if (curToken.equals("void")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
        } else error();
    }

    private void block() {
        TreeNode<NodeData> node = ast;
        if (curToken.equals("{")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();

            while (!curToken.equals("}")) {
                ast = node.addChild(new NodeData("BlockItem"));
                blockItem();
            }

            if (curToken.equals("}")) {
                ast = node.addChild(new NodeData(curToken));
                nextToken();
            } else error();
        }
    }

    private void blockItem() {
        TreeNode<NodeData> node = ast;
        if (curToken.equals("const") || curToken.equals("int")) {
            ast = node.addChild(new NodeData("Decl"));
            decl();
        } else {
            ast = node.addChild(new NodeData("Stmt"));
            stmt();
        }
    }

    private void decl() {
        TreeNode<NodeData> node = ast;
        if (curToken.equals("const")) {
            ast = node.addChild(new NodeData("ConstDecl"));
            constDecl();
        } else {
            ast = node.addChild(new NodeData("VarDecl"));
            varDecl();
        }
    }

    private void constDecl() {
        TreeNode<NodeData> node = ast;
        if (curToken.equals("const")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            ast = node.addChild(new NodeData("BType"));
            btype();
            ast = node.addChild(new NodeData("ConstDef"));
            constDef();
            while (!curToken.equals(";")) {
                if (curToken.equals(",")) {
                    ast = node.addChild(new NodeData(curToken));
                    nextToken();
                    ast = node.addChild(new NodeData("ConstDef"));
                    constDef();
                } else error();
            }
            if (curToken.equals(";")) {
                ast = node.addChild(new NodeData(curToken));
                nextToken();
            } else error();
        } else error();
    }

    private void varDecl() {
        TreeNode<NodeData> node = ast;
        ast = node.addChild(new NodeData("BType"));
        btype();
        ast = node.addChild(new NodeData("VarDef"));
        varDef();
        while (!curToken.equals(";")) {
            if (curToken.equals(",")) {
                ast = node.addChild(new NodeData(curToken));
                nextToken();
                ast = node.addChild(new NodeData("VarDef"));
                varDef();
            } else error();
        }
        if (curToken.equals(";")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
        } else error();
    }

    private void varDef() {
        TreeNode<NodeData> node = ast;
        if (curToken.isIdent()) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            while (curToken.equals("[")) {
                ast = node.addChild(new NodeData(curToken));
                nextToken();
                ast = node.addChild(new NodeData("ConstExpr"));
                constExp();
                if (curToken.equals("]")) {
                    ast = node.addChild(new NodeData(curToken));
                    nextToken();
                } else error();
            }
            if (curToken.equals("=")) {
                ast = node.addChild(new NodeData(curToken));
                nextToken();
                ast = node.addChild(new NodeData("InitVal"));
                initVal();
            }
        } else error();
    }

    private void btype() {
        TreeNode<NodeData> node = ast;
        if (curToken.equals("int")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
        } else error();
    }

    private void constDef() {
        TreeNode<NodeData> node = ast;
        if (curToken.isIdent()) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            while (curToken.equals("[")) {
                ast = node.addChild(new NodeData(curToken));
                nextToken();
                ast = node.addChild(new NodeData("ConstExpr"));
                constExp();
                if (curToken.equals("]")) {
                    ast = node.addChild(new NodeData(curToken));
                    nextToken();
                } else error();
            }
            if (curToken.equals("=")) {
                ast = node.addChild(new NodeData(curToken));
                nextToken();
                ast = node.addChild(new NodeData("ConstInitVal"));
                constInitVal();
            } else error();
        } else error();
    }

    private void constInitVal() {
        TreeNode<NodeData> node = ast;
        if (curToken.equals("{")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            if (curToken.equals("}")) {
                ast = node.addChild(new NodeData(curToken));
                nextToken();
            } else {
                ast = node.addChild(new NodeData("ConstInitVal"));
                constInitVal();
                while (curToken.equals(",")) {
                    ast = node.addChild(new NodeData(curToken));
                    nextToken();
                    ast = node.addChild(new NodeData("ConstInitVal"));
                    constInitVal();
                }
                if (curToken.equals("}")) {
                    ast = node.addChild(new NodeData(curToken));
                    nextToken();
                } else error();
            }
        } else {
            ast = node.addChild(new NodeData("ConstExp"));
            constExp();
        }
    }

    private void constExp() {
        TreeNode<NodeData> node = ast;
        ast = node.addChild(new NodeData("AddExpr"));
        addExpr();
    }

    private void initVal() {
        TreeNode<NodeData> node = ast;
        if (curToken.equals("{")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            if (curToken.equals("}")) {
                ast = node.addChild(new NodeData(curToken));
                nextToken();
            } else {
                ast = node.addChild(new NodeData("InitVal"));
                initVal();
                while (curToken.equals(",")) {
                    ast = node.addChild(new NodeData(curToken));
                    nextToken();
                    ast = node.addChild(new NodeData("InitVal"));
                    initVal();
                }
                if (curToken.equals("}")) {
                    ast = node.addChild(new NodeData(curToken));
                    nextToken();
                } else error();
            }
        } else {
            ast = node.addChild(new NodeData("Expr"));
            expr();
        }
    }

    private void stmt() {
        TreeNode<NodeData> node = ast;
        if (curToken.equals("{")) {
            ast = node.addChild(new NodeData("Block"));
            block();
        } else if (curToken.isIdent() && tokens.get(tokenId).equals("=")) {
            ast = node.addChild(new NodeData("Lval"));
            lval();
            if (curToken.equals("=")) {
                ast = node.addChild(new NodeData(curToken));
                nextToken();
                ast = node.addChild(new NodeData("Expr"));
                expr();
                if (curToken.equals(";")) {
                    ast = node.addChild(new NodeData(curToken));
                    nextToken();
                } else error();
            } else error();
        } else if (curToken.equals(";")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
        } else if (curToken.equals("return")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            if (curToken.equals(";")) {
                ast = node.addChild(new NodeData(curToken));
                nextToken();
            } else {
                ast = node.addChild(new NodeData("Expr"));
                expr();
                if (curToken.equals(";")) {
                    ast = node.addChild(new NodeData(curToken));
                    nextToken();
                } else error();
            }
        } else if (curToken.equals("if")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            if (curToken.equals("(")) {
                ast = node.addChild(new NodeData(curToken));
                nextToken();
                ast = node.addChild(new NodeData("Cond"));
                cond();
                if (curToken.equals(")")) {
                    ast = node.addChild(new NodeData(curToken));
                    nextToken();
                    ast = node.addChild(new NodeData("Stmt"));
                    stmt();
                    if (curToken.equals("else")) {
                        ast = node.addChild(new NodeData(curToken));
                        nextToken();
                        ast = node.addChild(new NodeData("Stmt"));
                        stmt();
                    }
                } else error();
            } else error();
        } else if (curToken.equals("while")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            if (curToken.equals("(")) {
                ast = node.addChild(new NodeData(curToken));
                nextToken();
                ast = node.addChild(new NodeData("Cond"));
                cond();
                if (curToken.equals(")")) {
                    ast = node.addChild(new NodeData(curToken));
                    nextToken();
                    ast = node.addChild(new NodeData("Stmt"));
                    stmt();
                } else error();
            } else error();
        } else if (curToken.equals("break")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            if (curToken.equals(";")) {
                ast = node.addChild(new NodeData(curToken));
                nextToken();
            } else error();
        } else if (curToken.equals("continue")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            if (curToken.equals(";")) {
                ast = node.addChild(new NodeData(curToken));
                nextToken();
            } else error();
        } else {
            ast = node.addChild(new NodeData("Expr"));
            expr();
            if (curToken.equals(";")) {
                ast = node.addChild(new NodeData(";"));
                nextToken();
            }
        }
    }

    private void expr() {
        TreeNode<NodeData> node = ast;
        ast = node.addChild(new NodeData("AddExpr"));
        addExpr();
    }

    private void addExpr() {
        TreeNode<NodeData> node = ast;
        ast = node.addChild(new NodeData("MulExpr"));
        mulExpr();
        while (curToken.equals("+") || curToken.equals("-")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            ast = node.addChild(new NodeData("MulExpr"));
            mulExpr();
        }
    }

    private void mulExpr() {
        TreeNode<NodeData> node = ast;
        ast = node.addChild(new NodeData("UnaryExpr"));
        unaryExpr();
        while (curToken.equals("*") || curToken.equals("/") || curToken.equals("%")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            ast = node.addChild(new NodeData("UnaryExpr"));
            unaryExpr();
        }
    }

    private void unaryExpr() {
        TreeNode<NodeData> node = ast;
        if (curToken.isIdent() && tokens.get(tokenId).equals("(")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            if (curToken.equals("(")) {
                ast = node.addChild(new NodeData(curToken));
                nextToken();
                if (curToken.equals(")")) {
                    ast = node.addChild(new NodeData(curToken));
                    nextToken();
                } else {
                    ast = node.addChild(new NodeData("FuncRParams"));
                    funcRParams();
                    if (curToken.equals(")")) {
                        ast = node.addChild(new NodeData(curToken));
                        nextToken();
                    } else error();
                }
            } else error();
        } else if (curToken.equals("(") || curToken.isNumber() || curToken.isIdent()) {
            ast = node.addChild(new NodeData("PrimaryExpr"));
            primaryExpr();
        } else if (curToken.equals("+") || curToken.equals("-") || curToken.equals("!")) {
            ast = node.addChild(new NodeData("UnaryOp"));
            unaryOp();
            ast = node.addChild(new NodeData("UnaryExpr"));
            unaryExpr();
        } else error();
    }

    private void primaryExpr() {
        TreeNode<NodeData> node = ast;
        if (curToken.equals("(")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            ast = node.addChild(new NodeData("Expr"));
            expr();
            if (curToken.equals(")")) {
                ast = node.addChild(new NodeData(curToken));
                nextToken();
            } else error();
        } else if (curToken.isNumber()) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
        } else if (curToken.isIdent()) {
            ast = node.addChild(new NodeData("Lval"));
            lval();
        } else error();
    }

    private void unaryOp() {
        TreeNode<NodeData> node = ast;
        if (curToken.equals("+") || curToken.equals("-") || curToken.equals("!")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
        } else error();
    }

    private void funcRParams() {
        TreeNode<NodeData> node = ast;
        ast = node.addChild(new NodeData("Expr"));
        expr();
        while (curToken.equals(",")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            ast = node.addChild(new NodeData("Expr"));
            expr();
        }
    }

    private void lval() {
        TreeNode<NodeData> node = ast;
        if (curToken.isIdent()) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            while (curToken.equals("[")) {
                ast = node.addChild(new NodeData(curToken));
                nextToken();
                expr();
                if (curToken.equals("]")) {
                    ast = node.addChild(new NodeData(curToken));
                    nextToken();
                } else error();
            }
        } else error();
    }

    private void cond() {
        TreeNode<NodeData> node = ast;
        ast = node.addChild(new NodeData("LOrExpr"));
        lOrExpr();
    }

    private void relExpr() {
        TreeNode<NodeData> node = ast;
        ast = node.addChild(new NodeData("AddExpr"));
        addExpr();
        while (curToken.equals(">") || curToken.equals("<") || curToken.equals("<=") || curToken.equals(">=")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            ast = node.addChild(new NodeData("AddExpr"));
            addExpr();
        }
    }

    private void eqExpr() {
        TreeNode<NodeData> node = ast;
        ast = node.addChild(new NodeData("RelExpr"));
        relExpr();
        while (curToken.equals("==") || curToken.equals("!=")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            ast = node.addChild(new NodeData("RelExpr"));
            relExpr();
        }
    }

    private void lAndExpr() {
        TreeNode<NodeData> node = ast;
        ast = node.addChild(new NodeData("EqExpr"));
        eqExpr();
        while (curToken.equals("&&")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            ast = node.addChild(new NodeData("EqExpr"));
            eqExpr();
        }
    }

    private void lOrExpr() {
        TreeNode<NodeData> node = ast;
        ast = node.addChild(new NodeData("LAndExpr"));
        lAndExpr();
        while (curToken.equals("||")) {
            ast = node.addChild(new NodeData(curToken));
            nextToken();
            ast = node.addChild(new NodeData("LAndExpr"));
            lAndExpr();
        }
    }
}
