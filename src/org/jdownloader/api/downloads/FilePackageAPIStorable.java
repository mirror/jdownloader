package org.jdownloader.api.downloads;

import jd.plugins.FilePackage;

import org.appwork.remoteapi.QueryResponseMap;
import org.appwork.storage.Storable;

public class FilePackageAPIStorable implements Storable {
    private FilePackage      pkg;
    private QueryResponseMap infoMap = null;

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

    public QueryResponseMap getInfoMap() {
        return infoMap;
    }

    public void setInfoMap(QueryResponseMap infoMap) {
        this.infoMap = infoMap;
    }
}