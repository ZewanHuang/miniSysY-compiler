import compiler.Parser;
import compiler.utils.FileUtils;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length >= 2) {
            String src = FileUtils.readFile(args[1]).trim() + "\0";
            Parser parser = new Parser(src);

            switch (args[0]) {
                case "-dump-tokens" -> {
                    if (args.length >= 4 && args[2].equals("-o"))
                        FileUtils.writeFile(args[3], parser.dumpTokens());
                    else
                        System.out.println(parser.dumpTokens());
                }
                case "-dump-ast" -> {
                    if (args.length >= 4 && args[2].equals("-o"))
                        FileUtils.writeFile(args[3], parser.dumpAST());
                    else
                        System.out.println(parser.dumpAST());
                }
                case "-dump-symbol-table" -> {
                    if (args.length >= 4 && args[2].equals("-o"))
                        FileUtils.writeFile(args[3], parser.dumpSymTable());
                    else
                        System.out.println(parser.dumpSymTable());
                }
                case "-llvm" -> {
                    System.out.println(src);
                    String outFile = (args.length >= 4 && args[2].equals("-o"))? args[3] : "debug/case.ll";
                    FileUtils.writeFile(outFile, parser.dumpLLVM());
                }
                case "-dump-answers" -> {
                    System.out.println(src);
                }
            }

        } else  System.out.println("Command error");
    }
}
