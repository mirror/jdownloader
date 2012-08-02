package org.jdownloader.extensions.vlcstreaming.upnp;

import java.util.ArrayList;
import java.util.List;

import org.teleal.cling.support.model.container.Container;

public class FolderContainer implements ContainerNode {

    private List<ContentNode> children = new ArrayList<ContentNode>();
    private String            title;
    private ContainerNode     parent;
    private int               id;

    public FolderContainer(int id, String title) {
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
        con.setClazz(new org.teleal.cling.support.model.DIDLObject.Class("object.container"));
        con.setRestricted(true);
        con.setSearchable(false);
        con.setTitle(title);
        return con;
    }

    @Override
    public int getID() {
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

}
