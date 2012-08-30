package org.jdownloader.extensions.streaming.mediaarchive;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

public class MediaFolder implements MediaNode {

    private String          name;
    private List<MediaNode> children;
    private ImageIcon       icon;
    private String          id;
    private MediaRoot       root;

    public List<MediaNode> getChildren() {
        return children;
    }

    public MediaFolder(String id, String name) {
        this.id = id;
        this.name = name;

        this.children = new ArrayList<MediaNode>();
    }

    public MediaFolder(String id) {
        this(id, id);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public ImageIcon getIcon() {
        return icon;
    }

    private MediaFolder parent;

    public MediaFolder getParent() {
        return parent;
    }

    @Override
    public void setParent(MediaFolder mediaFolder) {
        this.parent = mediaFolder;
        if (getRoot() == null) {
            if (parent.getRoot() != null) {
                parent.getRoot().mount(this);
            }
        }
    }

    @Override
    public String getUniqueID() {
        return id;
    }

    public void addChild(MediaNode child) {
        children.add(child);
        child.setParent(this);
        if (getRoot() != null) getRoot().mount(child);

    }

    public MediaRoot getRoot() {
        return root;
    }

    public MediaNode addChildren(List<? extends MediaItem> list) {
        for (MediaItem mi : list) {
            addChild(mi);
        }
        return this;
    }

    @Override
    public void setRoot(MediaRoot root) {
        this.root = root;
        for (MediaNode mi : getChildren()) {
            getRoot().mount(mi);

        }
    }

}
