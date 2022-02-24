package org.jdownloader.api.downloads;

import org.appwork.storage.Storable;
import org.appwork.storage.StorableValidatorIgnoresMissingSetter;

import jd.plugins.FilePackage;

public class FilePackageAPIStorable implements Storable {
    private FilePackage                                       pkg;
    private org.jdownloader.myjdownloader.client.json.JsonMap infoMap = null;

    public FilePackageAPIStorable(/* Storable */) {
    }

    public FilePackageAPIStorable(FilePackage pkg) {
        this.pkg = pkg;
    }

    @StorableValidatorIgnoresMissingSetter
    public String getName() {
        FilePackage lpkg = pkg;
        if (lpkg != null) {
            return lpkg.getName();
        }
        return null;
    }

    @StorableValidatorIgnoresMissingSetter
    public long getUUID() {
        FilePackage lpkg = pkg;
        if (lpkg != null) {
            return lpkg.getUniqueID().getID();
        }
        return 0;
    }

    public org.jdownloader.myjdownloader.client.json.JsonMap getInfoMap() {
        return infoMap;
    }

    public void setInfoMap(org.jdownloader.myjdownloader.client.json.JsonMap infoMap) {
        this.infoMap = infoMap;
    }
}