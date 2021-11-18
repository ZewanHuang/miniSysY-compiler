package compiler;

public class Optimizer {
    
    private String product;

    public Optimizer(String src) {
        this.product = src;
    }

    public String optim() {
        optimBr();
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
            result += "\n\n";
        }
        product = result;
    }
    
}
