package org.jdownloader.plugins.controller.container;

import org.appwork.storage.Storable;

public class AbstractContainerPlugin implements Storable {
    public AbstractContainerPlugin(/* storable */) {
    }

    private String  classname;
    private String  displayName;
    private boolean hasConfig;
    private long    version;

    public boolean isHasConfig() {
        return hasConfig;
    }

    public void setHasConfig(boolean hasConfig) {
        this.hasConfig = hasConfig;
    }

    public String getClassname() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
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

    private String pattern;

    public AbstractContainerPlugin(String className) {
        this.classname = className;
    }

    /**
     * @param version
     *            the version to set
     */
    public void setVersion(long version) {
        this.version = version;
    }

    /**
     * @return the version
     */
    public long getVersion() {
        return version;
    }

}
