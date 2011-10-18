package org.jdownloader.plugins.controller.crawler;

import org.appwork.storage.Storable;

public class AbstractCrawlerPlugin implements Storable {
    public AbstractCrawlerPlugin(/* STorable */) {
    }

    private String  classname;
    private String  displayName;
    private long    version;
    private boolean hasConfig;

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

    public AbstractCrawlerPlugin(String className) {
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
