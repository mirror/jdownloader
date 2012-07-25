package jd.controlling.packagecontroller;

import org.jdownloader.DomainInfo;

public interface AbstractPackageChildrenNode<E> extends AbstractNode {

    E getParentNode();

    void setParentNode(E parent);

    public DomainInfo getDomainInfo();
}
