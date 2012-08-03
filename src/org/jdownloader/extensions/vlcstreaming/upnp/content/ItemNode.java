package org.jdownloader.extensions.vlcstreaming.upnp.content;

import org.fourthline.cling.support.model.item.Item;

public interface ItemNode extends ContentNode {
    Item getImpl();
}
