package org.jdownloader.api;

import java.util.HashMap;

import org.appwork.storage.Storable;

public class DialogTypeInfo implements Storable {
    private HashMap<String, String> in;

    public HashMap<String, String> getIn() {
        return in;
    }

    public void setIn(HashMap<String, String> in) {
        this.in = in;
    }

    public HashMap<String, String> getOut() {
        return out;
    }

    public void setOut(HashMap<String, String> out) {
        this.out = out;
    }

    private HashMap<String, String> out;

    public DialogTypeInfo(/* storable */) {

        in = new HashMap<String, String>();
        out = new HashMap<String, String>();
    }

    public void addIn(String key, String type) {
        in.put(key, type);
    }

    public void addOut(String key, String type) {
        out.put(key, type);
    }

}
