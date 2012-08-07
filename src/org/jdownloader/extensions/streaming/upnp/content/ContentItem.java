package org.jdownloader.extensions.streaming.upnp.content;

public abstract class ContentItem implements ItemNode {

    protected ContainerNode parent;
    private String          id;
    private RootContainer   root;

    public ContentItem(String id) {
        this.id = id;
    }

    @Override
    public RootContainer getRoot() {
        return root;
    }

    @Override
    public void setRoot(RootContainer root) {
        this.root = root;
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
