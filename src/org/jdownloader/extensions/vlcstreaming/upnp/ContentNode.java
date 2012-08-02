package org.jdownloader.extensions.vlcstreaming.upnp;

import org.teleal.cling.support.model.DIDLObject;

public interface ContentNode {
    DIDLObject getImpl();

    void setParent(ContainerNode parent);

    int getID();
}
