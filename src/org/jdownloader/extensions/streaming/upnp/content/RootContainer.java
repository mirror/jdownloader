package org.jdownloader.extensions.streaming.upnp.content;

import java.util.HashMap;

import org.fourthline.cling.support.model.container.Container;

public class RootContainer extends FolderContainer {
    public RootContainer() {
        super("0", "Root");

        map.put(getID() + "", this);
    }

    @Override
    public RootContainer getRoot() {
        return this;
    }

    public HashMap<String, ContentNode> getMap() {
        return map;
    }

    private HashMap<String, ContentNode> map = new HashMap<String, ContentNode>();

    @Override
    public Container getImpl(String deviceID) {
        Container ret = super.getImpl(deviceID);
        ret.setParentID("-1");

        return ret;
    }
}
