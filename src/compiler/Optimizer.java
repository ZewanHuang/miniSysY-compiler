package compiler;

public class Optimizer {
    
    private String product;

    public Optimizer(String src) {
        this.product = src;
    }

    public String optim() {
        optimBr();
        optimBlock();
        return product;
    }


    private void optimBr() {
        String result = "";
        String[] blocks = product.split("\n\n");
        for (var block : blocks) {
            String[] lines = block.split("\n");
            int cnt = 0;
            for (var line : lines) {
                if (cnt == 1) {
                    break;
                }
                result += line + "\n";
                if (line.startsWith("br"))
                    cnt++;
            }
            result += "\n";
        }
        product = result;
    }

    private void optimBlock() {
        String result = "";
        String[] lines = product.split("\n");
        for (int i = 0; i < lines.length-1; i++) {
            if (lines[i].endsWith(":") && lines[i+1].equals("}"))
                continue;
            result += lines[i] + "\n";
        }
        result += lines[lines.length-1];
        product = result;
    }
    
}
