package org.jdownloader.api.downloads.v2;

import jd.plugins.FilePackage;

import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.bindings.FilePackageStorable;

public class FilePackageAPIStorableV2 extends FilePackageStorable implements Storable {

    public FilePackageAPIStorableV2(/* Storable */) {
    }

    public FilePackageAPIStorableV2(FilePackage pkg) {
        setName(pkg.getName());
        setUuid(pkg.getUniqueID().getID());
    }

}