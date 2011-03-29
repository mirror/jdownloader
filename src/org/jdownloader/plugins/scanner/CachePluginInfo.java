package org.jdownloader.plugins.scanner;

import org.appwork.storage.Storable;

public class CachePluginInfo implements Storable {

    private String name;

    public String getName() {
        return name;
    }

    public String getPattern() {
        return pattern;
    }

    private String host;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public boolean isSettingsAvailable() {
        return settingsAvailable;
    }

    public void setSettingsAvailable(boolean settings) {
        this.settingsAvailable = settings;
    }

    private boolean settingsAvailable;

    private String  pattern;
    private String  hash;

    public String getHash() {
        return hash;
    }

    public String getClassPath() {
        return classPath;
    }

    private String classPath;
    private String revision;

    public String getRevision() {
        return revision;
    }

    protected CachePluginInfo() {
        // required by Storable;
    }

    public CachePluginInfo(String name, String pattern, String revision, String hash, String classPath) {

        this.name = name;
        this.pattern = pattern;
        this.hash = hash;
        this.revision = revision;
        this.classPath = classPath;
    }

}
