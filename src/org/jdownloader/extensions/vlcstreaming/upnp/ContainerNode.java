package org.jdownloader.extensions.vlcstreaming.upnp;

import java.util.List;

import org.teleal.cling.support.model.container.Container;

public interface ContainerNode extends ContentNode {

    List<ContentNode> getChildren();

    Container getImpl();

    public void addChildren(ContentNode child);

}
