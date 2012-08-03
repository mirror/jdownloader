package org.jdownloader.extensions.streaming.upnp.content;

import java.util.ArrayList;
import java.util.List;

import org.fourthline.cling.support.model.container.Container;

public class FolderContainer implements ContainerNode {

    private List<ContentNode> children = new ArrayList<ContentNode>();
    private String            title;
    private ContainerNode     parent;
    private String            id;

    public FolderContainer(String id, String title) {
        this.title = title;
        this.id = id;
    }

    public List<ContentNode> getChildren() {

        return children;
    }

    @Override
    public Container getImpl() {
        Container con = new Container();
        if (parent != null) con.setParentID(parent.getID() + "");
        con.setId(getID() + "");
        con.setChildCount(children.size());
        con.setClazz(new org.fourthline.cling.support.model.DIDLObject.Class("object.container"));
        con.setRestricted(true);
        con.setSearchable(false);
        con.setTitle(title);
        return con;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public void setParent(ContainerNode parent) {
        this.parent = parent;
    }

    @Override
    public void addChildren(ContentNode child) {
        children.add(child);
        child.setParent(this);
    }

    @Override
    public ContainerNode getParent() {
        return parent;
    }

    @Override
    public String getTitle() {
        return title;
    }

}
