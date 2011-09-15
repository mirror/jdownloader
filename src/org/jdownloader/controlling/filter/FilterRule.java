package org.jdownloader.controlling.filter;

import org.appwork.storage.Storable;

public class FilterRule implements Storable {

    private FilersizeFilter size;
    private RegexFilter     hoster;
    private RegexFilter     source;
    private FiletypeFilter  type;
    private RegexFilter     filename;

    public FilterRule() {
        // required by Storable
    }

    private boolean enabled;

    private String  name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
