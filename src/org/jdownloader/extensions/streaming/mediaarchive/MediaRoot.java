package org.jdownloader.extensions.streaming.mediaarchive;

import java.util.HashMap;

public class MediaRoot extends MediaFolder {

    private HashMap<String, MediaNode> map;

    public MediaRoot() {
        // some renderers, like the ps3 want the root to have a 0 id
        super("0");
        this.map = new HashMap<String, MediaNode>();
        setRoot(this);
    }

    public void mount(MediaNode child) {
        map.put(child.getUniqueID(), child);
        child.setRoot(this);
    }

    public MediaFolder getFolder(String objectID) {
        try {
            return (MediaFolder) map.get(objectID);
        } catch (Exception e) {
            return null;
        }
    }

    public MediaNode get(String objectID) {
        return map.get(objectID);
    }

}
