declare i32 @getint()
declare void @putint(i32)
declare i32 @getch()
declare void @putch(i32)
define dso_local i32 @main() {

%1 = alloca i32
%2 = call i32 @getch()
store i32 %2, i32* %1
%3 = alloca i32

%4 = call i32 @getch()
store i32 %4, i32* %3
%5 = load i32, i32* %1
call void @putch(i32 %5)
%6 = load i32, i32* %3
call void @putch(i32 %6)
call void @putch(i32 10)
%7 = load i32, i32* %1
%8 = sub i32 %7, 16
call void @putch(i32 %8)
%9 = load i32, i32* %3
%10 = add i32 %9, 6
call void @putch(i32 %10)
ret i32 0


}