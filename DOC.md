# 短路求值挑战实验报告

> 学号：19373257 姓名：黄泽桓

短路求值实验是基于条件语句和循环拓展的，因此本次实验报告可能会简单谈及条件语句基本的实现，主要讲述为实现短路求值做出的改动。

## 简要介绍

为实现短路求值，我使用的仍然是 continue 和 break **回填**那一套。同样的，和 while 一样，if else 可能会嵌套，所以需要使用栈结构来维护回填。

此处的介绍主要以 visitIfElseStmt 为例：

```grammar
Stmt -> 'if' '(' Cond ')' Stmt 'else' Stmt
```

1. 进入 IfElseStmt，首先向 stk 栈中 push 记录器 Recorder；
2. 调用递归执行 visitCond 后，添加标识符号 mark1，以便后续添加 br 语句；
3. 调用递归执行 visitStmt[1] 后，添加标识符号 mark2，以便后续添加 br 语句；
4. 调用递归执行 visitStmt[2] 后，此时区块号全部已知，对栈顶的 Recorder 所有的 marks 进行替换回填。

上述主要是之前 If else 的实现，为实现短路求值，首先对 visitCond 中的 visitAndExpr 和 visitOrExpr 进行修改：

```grammar
LOrExp -> LAndExp | LOrExp '||' LAndExp
LAndExp -> EqExp | LAndExp '&&' EqExp
```

1. visit 第一个子节点
2. 若存在两个以上子节点，循环处理剩下子节点：
   - 添加跳转指令，并向 stk 栈顶 Recorder 记录 mark3/4（AND 对应 mark3，OR 对应 mark4）
   - visit 节点
   - 记录节点

需要注意的是，AND 和 OR 的 br 指令跳转的 label 位置是相反的，因为 `A && B` 是 A 满足时走 B，不满足时直接跳转 else；而 `A || B` 是 A 满足时直接进入，不满足时走 B。

然后在 visitIfElseStmt、visitIfStmt、visitWhileStmt 末端进行回填和弹栈即可。

伪代码如下：

```java
void visitIfElseStmt() {
    stk.push(new Recorder());
    
    visit(Cond);
    stk.peek().record(mark1);
    add_br(mark1);
    visit(Stmt1);
    stk.peek().record(mark2);
   	add_br(mark2);
    visit(Stmt2);
    
    for (var mark : stk.peek().marks)
        replace_with_record(mark, value);
    stk.pop();
}
```

```java
void visitOrExpr() {
    visit(AndExpr);
    for (i = 2; i < children.size; i += 2) {
        for (var mark : stk.peek().marks)
        	replace_with_record_AND_Expr(mark, value);
        
        stk.peek().record(mark3);
        add_br(mark3);
        new_br_block();
    }
}
```

```java
void visitAndExpr() {
    visit(EqExp);
    for (i = 2; i < children.size; i += 2) {
        stk.peek().record(mark3);
        add_br(mark3);
        new_br_block();
    }
}
```

需要注意的是，可能出现与、或同时出现，因此需要在VisitOrExpr中，当有多个AndExpr时对AND进行回填。

## 参考文献


主要参考教材和助教实验文档对编译过程的讲解，未参考其它具体的代码或教程。

