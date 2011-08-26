package jd.controlling.packagecontroller;

public interface AbstractPackageChildrenNode<E> extends AbstractNode {

    E getParentNode();

    void setParentNode(E parent);
}
