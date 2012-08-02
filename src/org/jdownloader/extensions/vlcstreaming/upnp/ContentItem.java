package org.jdownloader.extensions.vlcstreaming.upnp;

public abstract class ContentItem implements ItemNode {

    protected ContainerNode parent;
    private int             id;

    public ContentItem(int id) {
        this.id = id;
    }

    @Override
    public void setParent(ContainerNode parent) {
        this.parent = parent;
    }

    @Override
    public int getID() {
        return id;
    }

}
