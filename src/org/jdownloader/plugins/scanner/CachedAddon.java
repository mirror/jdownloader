package org.jdownloader.plugins.scanner;

import org.appwork.storage.Storable;

public class CachedAddon implements Storable {
    private String file;
    private String clazz;
    private String hash;
    private String id;
    private String icon;

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public double getMinJVM() {
        return minJVM;
    }

    public void setMinJVM(double minJVM) {
        this.minJVM = minJVM;
    }

    public boolean isWindows() {
        return windows;
    }

    public void setWindows(boolean windows) {
        this.windows = windows;
    }

    public boolean isMac() {
        return mac;
    }

    public void setMac(boolean mac) {
        this.mac = mac;
    }

    public boolean isLinux() {
        return linux;
    }

    public void setLinux(boolean linux) {
        this.linux = linux;
    }

    public int getInterfaceVersion() {
        return interfaceVersion;
    }

    public void setInterfaceVersion(int interfaceVersion) {
        this.interfaceVersion = interfaceVersion;
    }

    private double  minJVM;
    private boolean windows;
    private boolean mac;
    private boolean linux;
    private int     interfaceVersion;
    private String  revision;
    private boolean defaultEnabled;

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public CachedAddon() {
        // required by Storable
    }

    public CachedAddon(String file, String revision, String clazz, String hash, String id, int interfaceVersion, boolean linux, boolean mac, boolean windows, double minJVM, boolean b, String icon) {
        this.file = file;
        this.clazz = clazz;
        this.defaultEnabled = b;
        this.revision = revision;
        this.hash = hash;
        this.id = id;
        this.interfaceVersion = interfaceVersion;
        this.linux = linux;
        this.mac = mac;
        this.windows = windows;
        this.minJVM = minJVM;
        this.icon = icon;

    }

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public void setDefaultEnabled(boolean defaultEnabled) {
        this.defaultEnabled = defaultEnabled;
    }
}
