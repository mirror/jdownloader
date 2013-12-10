package org.jdownloader.api.downloads;

import java.util.HashMap;

import jd.plugins.FilePackage;

import org.appwork.storage.Storable;

public class FilePackageAPIStorable implements Storable {
    private FilePackage             pkg;
    private org.jdownloader.myjdownloader.client.json.JsonMap infoMap = null;

    public FilePackageAPIStorable(/* Storable */) {
    }

    public FilePackageAPIStorable(FilePackage pkg) {
        this.pkg = pkg;
    }

    public String getName() {
        return pkg.getName();
    }

    public long getUUID() {
        return pkg.getUniqueID().getID();
    }

    public org.jdownloader.myjdownloader.client.json.JsonMap getInfoMap() {
        return infoMap;
    }

    public void setInfoMap(org.jdownloader.myjdownloader.client.json.JsonMap infoMap) {
        this.infoMap = infoMap;
    }
}