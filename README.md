# miniSysY-compiler

## 项目介绍

Front-end part of compiler for miniSysY, implemented in Java.

北航软院大三编译大作业。本项目未借助antlr等自动化工具，采用状态图进行词法分析，递归下降进行语法分析，借助符号表管理等实现中间代码生成。

指导文档：[https://buaa-se-compiling.github.io/miniSysY-tutorial/](https://buaa-se-compiling.github.io/miniSysY-tutorial/)

## 项目运行

项目在 Jetbrains IDEA 编写，可借助该软件配置命令行参数运行项目。以下介绍命令行运行方式：

### 命令行编译

```shell
cd src
javac -encoding UTF-8 -d ./output Main.java
```

项目编译结果在 `./output` 目录下。

### 生成中间代码

> `-o` 后的参数指定输出文件

```shell
java Main -llvm case.sy -o case.ll
```

运行项目，解析 `case.sy` 文件的源代码，生成 LLVM IR 中间代码。

### 查看中间产物

**tokens**

```shell
java Main -dump-tokens case.sy -o tokens.txt
```

**AST 抽象语法树**

```shell
java Main -dump-ast case.sy -o ast.txt
```

**符号表**

```shell
java Main -dump-symbol-table case.sy -o symtable.txt
```
