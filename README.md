# Compilar

A compiler for miniSysY, implemented in Java.

北航软院大三编译大作业

指导文档：[https://buaa-se-compiling.github.io/miniSysY-tutorial/](https://buaa-se-compiling.github.io/miniSysY-tutorial/)

## 项目介绍

项目在 Jetbrains IDEA 编写，可借助该软件配置命令行参数运行项目。

命令行编译项目

```shell
cd src
javac -encoding UTF-8 -d ./output Main.java
cd output
```

运行项目，解析 `case.sy` 文件的源代码，生成 LLVM IR 中间代码

```shell
java Main -llvm case.sy [-o case.ll]
```

可查看解析中间产物：

tokens

```shell
java Main -dump-tokens case.sy [-o tokens.txt]
```

AST 抽象语法树

```shell
java Main -dump-ast case.sy [-o ast.txt]
```

符号表

```shell
java Main -dump-symbol-table case.sy [-o symtable.txt]
```