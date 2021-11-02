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
                case "-llvm" -> {
                    System.out.println("llvm");
                }
                case "-dump-ast" -> {
                    System.out.println(parser.dumpAST());
                }
                case "-dump-symbol-table" -> {
                    System.out.println("symbol-table");
                }
                case "-dump-answers" -> {
                    System.out.println(FileUtils.readFile(args[1]));
                }
            }

        } else  System.out.println("Command error");
    }
}
