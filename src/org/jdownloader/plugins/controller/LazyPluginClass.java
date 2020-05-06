package org.jdownloader.plugins.controller;

import java.util.List;

public class LazyPluginClass {
    private final byte[] sha256;

    public final byte[] getSha256() {
        return sha256;
    }

    public final long getLastModified() {
        return lastModified;
    }

    public final int getInterfaceVersion() {
        return interfaceVersion;
    }

    private final String className;

    public final String getClassName() {
        return className;
    }

    private volatile long lastModified;

    public final void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    private final long revision;

    public final long getRevision() {
        return revision;
    }

    private final int          interfaceVersion;
    private final List<String> dependencies;

    public final List<String> getDependencies() {
        return dependencies;
    }

    public LazyPluginClass(String className, byte[] sha256, long lastModified, int interfaceVersion, long revision, List<String> dependencies) {
        this.sha256 = sha256;
        this.className = className;
        this.lastModified = lastModified;
        this.interfaceVersion = interfaceVersion;
        this.revision = revision;
        if (dependencies != null && dependencies.size() > 0) {
            this.dependencies = dependencies;
        } else {
            this.dependencies = null;
        }
    }

    @Override
    public String toString() {
        return getClassName() + "@" + getRevision() + "|" + getInterfaceVersion();
    }
}
