package org.jdownloader.api.plugins;

import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.storage.Storable;
import org.appwork.storage.StorableDeprecatedSince;
import org.jdownloader.myjdownloader.client.bindings.AdvancedConfigEntryDataStorable;

@AllowNonStorableObjects
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

    @Deprecated
    @Override
    @StorableDeprecatedSince("2022-10-18T00:00+0200")
    /**
     * @Deprecated use #getAbstractType instead
     * @return
     */
    public String getType() {
        return super.getType();
    }

    @Deprecated
    @Override
    @StorableDeprecatedSince("2022-10-18T00:00+0200")
    public void setType(final String type) {
        super.setType(type);
    }
}
