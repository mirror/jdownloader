package org.jdownloader.jdserv;

import java.util.ArrayList;

import org.appwork.storage.Storable;

public class LogCollection implements Storable {
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private java.util.List<LogSession> list;

    public java.util.List<LogSession> getList() {
        return list;
    }

    public void setList(java.util.List<LogSession> list) {
        this.list = list;
    }

    public LogCollection(/* Storable */) {
        list = new ArrayList<LogSession>();
    }

    public void add(LogSession ls) {
        list.add(ls);
    }
}
