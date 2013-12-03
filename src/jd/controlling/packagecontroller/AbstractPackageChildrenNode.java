package jd.controlling.packagecontroller;

import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.UniqueAlltimeID;

public interface AbstractPackageChildrenNode<E> extends AbstractNode {

    E getParentNode();

    void setParentNode(E parent);

    UniqueAlltimeID getPreviousParentNodeID();

    public DomainInfo getDomainInfo();

    public boolean hasVariantSupport();
}
