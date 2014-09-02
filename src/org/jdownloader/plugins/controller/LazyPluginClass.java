package org.jdownloader.plugins.controller;

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

    private final long lastModified;
    private final long revision;

    public final long getRevision() {
        return revision;
    }

    private final int interfaceVersion;

    public LazyPluginClass(String className, byte[] sha256, long lastModified, int interfaceVersion, long revision) {
        this.sha256 = sha256;
        this.className = className;
        this.lastModified = lastModified;
        this.interfaceVersion = interfaceVersion;
        this.revision = revision;
    }

    @Override
    public String toString() {
        return getClassName() + "@" + getRevision() + "|" + getInterfaceVersion();
    }
}
