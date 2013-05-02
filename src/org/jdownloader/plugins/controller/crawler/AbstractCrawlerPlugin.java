package org.jdownloader.plugins.controller.crawler;

import org.appwork.storage.Storable;

public class AbstractCrawlerPlugin implements Storable {
    public AbstractCrawlerPlugin(/* STorable */) {
    }

    public static final int CACHEVERSION           = 2;
    private String          classname;
    private String          displayName;
    private long            version;
    private boolean         hasConfig;
    private int             interfaceVersion       = 0;
    private int             maxConcurrentInstances = Integer.MAX_VALUE;
    private long            mainClassLastModified  = -1;
    private String          mainClassSHA256        = null;
    private String          mainClassFilename      = null;
    private int             cacheVersion           = -1;

    public int getCacheVersion() {
        return cacheVersion;
    }

    public void setCacheVersion(int cacheVersion) {
        this.cacheVersion = cacheVersion;
    }

    public long getMainClassLastModified() {
        return mainClassLastModified;
    }

    public void setMainClassLastModified(long mainClassLastModified) {
        this.mainClassLastModified = mainClassLastModified;
    }

    public String getMainClassSHA256() {
        return mainClassSHA256;
    }

    public void setMainClassSHA256(String mainClassSHA256) {
        this.mainClassSHA256 = mainClassSHA256;
    }

    public String getMainClassFilename() {
        return mainClassFilename;
    }

    public void setMainClassFilename(String mainClassFilename) {
        this.mainClassFilename = mainClassFilename;
    }

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

    /**
     * @return the interfaceVersion
     */
    public int getInterfaceVersion() {
        return interfaceVersion;
    }

    /**
     * @param interfaceVersion
     *            the interfaceVersion to set
     */
    public void setInterfaceVersion(int interfaceVersion) {
        this.interfaceVersion = interfaceVersion;
    }

    /**
     * @return the maxConcurrentInstances
     */
    public int getMaxConcurrentInstances() {
        return maxConcurrentInstances;
    }

    /**
     * @param maxConcurrentInstances
     *            the maxConcurrentInstances to set
     */
    public void setMaxConcurrentInstances(int maxConcurrentInstances) {
        this.maxConcurrentInstances = maxConcurrentInstances;
    }

}
