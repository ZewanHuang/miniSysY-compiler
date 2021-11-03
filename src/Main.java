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
                    System.out.println(parser.dumpTokens());
                }
                case "-dump-ast" -> {
                    System.out.println(parser.dumpAST());
                }
                case "-dump-symbol-table" -> {
                    System.out.println(parser.dumpSymTable());
                }
                case "-llvm" -> {
                    String outFile = (args.length >= 4 && args[2].equals("-o"))? args[3] : "case.ll";
                    FileUtils.writeFile(outFile, parser.dumpLLVM());
                }
                case "-dump-answers" -> {
                    System.out.println(FileUtils.readFile(args[1]));
                }
            }

        } else  System.out.println("Command error");
    }
}
