package org.jdownloader.jdserv;

import java.util.HashMap;

import org.appwork.storage.Storable;

public class LogSession implements Storable {
    private String                  folder;
    private HashMap<String, String> map;

    public HashMap<String, String> getMap() {
        return map;
    }

    public void setMap(HashMap<String, String> map) {
        this.map = map;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public LogSession(/* 'STorable */) {
        map = new HashMap<String, String>();
    }

    public LogSession(String folder) {
        this();
        this.folder = folder;
    }

    public void put(String key, String value) {
        map.put(key, value);
    }
}
