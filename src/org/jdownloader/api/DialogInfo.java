package org.jdownloader.api;

import java.util.HashMap;

import org.appwork.storage.Storable;

public class DialogInfo implements Storable {
    public DialogInfo(/* Storable */) {

    }

    HashMap<String, String> properties = new HashMap<String, String>();
    private String          type;

    public HashMap<String, String> getProperties() {
        return properties;
    }

    public void setProperties(HashMap<String, String> properties) {
        this.properties = properties;
    }

    public void put(String key, Object value) {
        if (value == null) return;
        properties.put(key, value.toString());
    }

    public void setType(String name) {
        this.type = name;
    }

    public String getType() {
        return type;
    }

}
