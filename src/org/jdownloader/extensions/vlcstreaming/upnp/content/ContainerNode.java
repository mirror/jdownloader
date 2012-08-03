package org.jdownloader.extensions.vlcstreaming.upnp.content;

import java.util.List;

import org.fourthline.cling.support.model.container.Container;

public interface ContainerNode extends ContentNode {

    List<ContentNode> getChildren();

    Container getImpl();

    public String getTitle();

    public void addChildren(ContentNode child);

}
