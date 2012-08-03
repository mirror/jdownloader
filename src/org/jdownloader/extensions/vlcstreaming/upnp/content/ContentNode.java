package org.jdownloader.extensions.vlcstreaming.upnp.content;

import org.fourthline.cling.support.model.DIDLObject;

public interface ContentNode {
    DIDLObject getImpl();

    void setParent(ContainerNode parent);

    public ContainerNode getParent();

    String getID();
}
