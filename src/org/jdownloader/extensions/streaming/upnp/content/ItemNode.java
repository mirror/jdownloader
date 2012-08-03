package org.jdownloader.extensions.streaming.upnp.content;

import org.fourthline.cling.support.model.item.Item;

public interface ItemNode extends ContentNode {
    Item getImpl();
}
