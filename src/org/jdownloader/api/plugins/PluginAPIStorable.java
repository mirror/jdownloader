package org.jdownloader.api.plugins;

import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.bindings.AdvancedConfigEntryDataStorable;

public class PluginAPIStorable extends AdvancedConfigEntryDataStorable implements Storable {
    private String className;
    private String displayName;
    private String pattern;
    private String version;

    protected PluginAPIStorable() {
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
