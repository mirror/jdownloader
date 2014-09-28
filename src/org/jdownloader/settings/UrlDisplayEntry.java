package org.jdownloader.settings;

import org.appwork.storage.Storable;

public class UrlDisplayEntry implements Storable {
    public UrlDisplayEntry(/* storable */) {
    }

    public UrlDisplayEntry(String name, boolean b) {
        this.type = name;
        this.enabled = b;
    }

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private boolean enabled = true;
}
