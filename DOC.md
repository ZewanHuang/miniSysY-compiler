# Lab6 实验报告

> 学号：19373257 姓名：黄泽桓

## 简要介绍

针对part10循环语句，与if跳转逻辑没什么差别，就是处理stmt多加一个while的处理，跳转block。

针对part11continue、break，我采用了助教给出的代码回填的方法，使用一个栈存储Recorder，遇到stmt时一开始创建一个recorder，遇到break与continue时，先向中间代码产物插入特定标识符，并在recorder中记录标识符，而后在stmt内的最后对recorder遍历替换标识符。

## 参考

主要参考教材和助教实验文档对编译过程的讲解，未参考其它具体的代码或教程。

