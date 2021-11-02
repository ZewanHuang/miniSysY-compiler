package compiler.parser.ast;

import java.util.LinkedList;
import java.util.List;

public class TreeNode<T> {
    /**
     * 树节点
     */
    public T data;

    /**
     * 父节点，根没有父节点
     */
    public TreeNode<T> parent;

    /**
     * 子节点，叶子节点没有子节点
     */
    public List<TreeNode<T>> children;

    /**
     * 构造函数
     *
     * @param data 节点数据
     */
    public TreeNode(T data) {
        this.data = data;
        this.children = new LinkedList<>();
    }

    /**
     * 判断是否为根：根没有父节点
     *
     * @return 是否为根节点
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * 判断是否为叶节点：叶节点没有子节点
     *
     * @return 是否为叶节点
     */
    public boolean isLeaf() {
        return children.size() == 0;
    }

    /**
     * 为当前节点添加子节点
     */
    public TreeNode<T> addChild(T child) {
        TreeNode<T> childNode = new TreeNode<>(child);
        childNode.parent = this;
        this.children.add(childNode);
        return childNode;
    }

    /**
     * 获取当前节点的层
     *
     * @return 节点所在层数
     */
    public int getLevel() {
        if (this.isRoot())  return 0;
        else return parent.getLevel()+1;
    }

    public TreeNode<T> getRoot() {
        TreeNode<T> node = this;
        while (!node.isRoot()) {
            node = node.parent;
        }
        return node;
    }

    public String getTree() {
        StringBuilder str = new StringBuilder();
        TreeNode<T> node = this;
        str.append("\t".repeat(node.getLevel())).append(node.toString()).append("\n");
        for (TreeNode<T> child : node.children) {
            str.append(child.getTree());
        }
        return str.toString();
    }

    @Override
    public String toString() {
        return data != null ? data.toString() : "[tree data null]";
    }
}
