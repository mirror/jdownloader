package org.jdownloader.extensions.streaming.upnp.content;

import org.fourthline.cling.support.model.container.Container;

public class RootContainer extends FolderContainer {
    public RootContainer() {
        super("0", "Root");
    }

    @Override
    public Container getImpl() {
        Container ret = super.getImpl();
        ret.setParentID("-1");

        return ret;
    }
}
