package org.jdownloader.gui.views.downloads.table;

import java.awt.datatransfer.DataFlavor;

import jd.plugins.FilePackage;

public class FilePackagesDataFlavor extends DataFlavor {

    public static FilePackagesDataFlavor Flavor = new FilePackagesDataFlavor();

    private FilePackagesDataFlavor() {
        /*
         * It is important to use this constructor, without it dragdrop will not
         * work correctly
         */
        super(FilePackage.class, "FilePackages");
    }

    /*
     * we want to reuse the Content and not serialize/deserialize it every time
     */
    @Override
    public boolean isFlavorSerializedObjectType() {
        return false;
    }

}
