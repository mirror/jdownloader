package org.jdownloader.plugins.controller.host;

import org.appwork.storage.Storable;

public class AbstractHostPlugin implements Storable {
    public AbstractHostPlugin(/* STorable */) {
    }

    public static final int CACHEVERSION    = 3;
    private boolean         premium;
    private boolean         hasConfig;
    private boolean         hasLinkRewrite  = false;
    private String          configInterface = null;

    public String getConfigInterface() {
        return configInterface;
    }

    public void setConfigInterface(String configInterface) {
        this.configInterface = configInterface;
    }

    public boolean isHasLinkRewrite() {
        return hasLinkRewrite;
    }

    public void setHasLinkRewrite(boolean hasLinkRewrite) {
        this.hasLinkRewrite = hasLinkRewrite;
    }

    public boolean isHasAccountRewrite() {
        return hasAccountRewrite;
    }

    public void setHasAccountRewrite(boolean hasAccountRewrite) {
        this.hasAccountRewrite = hasAccountRewrite;
    }

    private boolean hasAccountRewrite     = false;
    private long    version;
    private int     interfaceVersion      = 0;
    private long    mainClassLastModified = -1;
    private String  mainClassSHA256       = null;
    private String  mainClassFilename     = null;
    private int     cacheVersion          = -1;

    public int getCacheVersion() {
        return cacheVersion;
    }

    public void setCacheVersion(int cacheVersion) {
        this.cacheVersion = cacheVersion;
    }

    public String getMainClassFilename() {
        return mainClassFilename;
    }

    public void setMainClassFilename(String mainClassFilename) {
        this.mainClassFilename = mainClassFilename;
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

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public boolean isHasConfig() {
        return hasConfig;
    }

    public void setHasConfig(boolean hasConfig) {
        this.hasConfig = hasConfig;
    }

    public boolean isPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    private String premiumUrl;

    public String getPremiumUrl() {
        return premiumUrl;
    }

    public void setPremiumUrl(String premiumUrl) {
        this.premiumUrl = premiumUrl;
    }

    private String classname;
    private String displayName;

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

    public AbstractHostPlugin(String className) {
        this.classname = className;
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

}
