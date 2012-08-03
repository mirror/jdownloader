package org.jdownloader.extensions.vlcstreaming.upnp.content;

public abstract class ContentItem implements ItemNode {

    protected ContainerNode parent;
    private String          id;

    public ContentItem(String id) {
        this.id = id;
    }

    @Override
    public ContainerNode getParent() {
        return parent;
    }

    @Override
    public void setParent(ContainerNode parent) {
        this.parent = parent;
    }

    @Override
    public String getID() {
        return id;
    }

}
