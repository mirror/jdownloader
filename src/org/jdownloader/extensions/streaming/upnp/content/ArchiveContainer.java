package org.jdownloader.extensions.streaming.upnp.content;

import org.fourthline.cling.support.model.container.Container;
import org.jdownloader.extensions.extraction.Archive;

public class ArchiveContainer extends FolderContainer {

    public ArchiveContainer(String id, Archive archive) {
        super(id, "[ARCHIVE] " + archive.getName());

    }

    @Override
    public Container getImpl() {
        Container con = new Container();
        con.setParentID(getParent().getID());
        con.setId(getID());
        con.setChildCount(1);
        con.setClazz(new org.fourthline.cling.support.model.DIDLObject.Class("object.container.storageFolder"));
        con.setRestricted(true);
        con.setSearchable(false);
        con.setTitle(getTitle());
        return con;
    }
}
