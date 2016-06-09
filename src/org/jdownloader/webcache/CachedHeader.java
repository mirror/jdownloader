package org.jdownloader.webcache;

import java.util.List;

import org.appwork.storage.Storable;

public class CachedHeader implements Storable {
    private String       key;
    private List<String> values;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public CachedHeader(/* Storable */) {
    }

    public CachedHeader(String v, List<String> s) {
        this.key = v;
        this.values = s;
    }

}
