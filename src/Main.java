import lexer.Scanner;
import parser.Parser;
import utils.FileUtils;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length >= 2) {
            String src = FileUtils.readFile(args[1]).trim();

            switch (args[0]) {
                case "-dump-tokens":
                    Scanner lexer = new Scanner(src);
                    lexer.dumpTokens();
                    break;
                case "-llvm":
                    Parser parser = new Parser();
                    String res = parser.parse(src);
                    if (args.length >= 4 && args[2].equals("-o"))
                        FileUtils.writeFile(args[3], res);
                    else
                        FileUtils.writeFile("case.ll", res);
                    break;
                case "-dump-symbol-table":
                    break;
            }

        } else  System.out.println("Command error");
    }
}
