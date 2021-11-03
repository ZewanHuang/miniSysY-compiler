# Lab3 实验报告

> 学号：19373257 姓名：黄泽桓

## 简要介绍

本次实验换用Java实现编译任务，并参考教材结合自己的思考，对项目框架进行搭建。

项目在解析源码过程中，执行了四遍，分别是：

1. Scanner扫描器，输入字符串形式的源码，输出tokens存于ArrayList（词法分析）
2. Descender递归下降器，输入tokens，输出语法树AST（语法分析）
3. Analyzer语义分析器，输入语法树AST，记录符号表，分析语义（语义分析）
4. Generator中间代码生成器，输入语法树AST和符号表，输出中间代码

AST节点信息：

```java
public String name;		// 标识名称，如FuncDef、Number
public String value;	// 具体值，叶子节点存储token的value，如Number的数值，非叶子节点存储寄存器ID
public int regIdx;		// 寄存器序号
```

符号表设计：

含标识符的名称、所在区块、是否有效、标识符类型、标识符值类型、函数参数列表、寄存器ID。

Generator：

递归遍历树，涉及值运算时，将当前节点子节点计算结果存于寄存器序号，并将该序号记录于`regIdx`中，以便递归计算。

针对part5和part6，主要根据语法和语义对上述2、3和4进行完善。

## 参考

主要参考教材对编译过程的讲解，未参考其它具体的代码或教程。