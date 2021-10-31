import lexer.Scanner;
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
                    if (args.length >= 4 && args[2].equals("-o"))
                        FileUtils.writeFile(args[3], src);
                    else
                        FileUtils.writeFile("case.ll", src);
                    break;
                case "-dump-symbol-table":
                    break;
            }

        } else  System.out.println("Command error");
    }
}
