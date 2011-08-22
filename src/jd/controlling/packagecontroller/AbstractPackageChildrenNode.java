package jd.controlling.packagecontroller;

public interface AbstractPackageChildrenNode<E> {

    E getParentNode();

    void setParentNode(E parent);
}
