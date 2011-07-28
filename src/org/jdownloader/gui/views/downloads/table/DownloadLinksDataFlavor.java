package org.jdownloader.gui.views.downloads.table;

import java.awt.datatransfer.DataFlavor;

import jd.plugins.DownloadLink;

public class DownloadLinksDataFlavor extends DataFlavor {

    public static DownloadLinksDataFlavor Flavor = new DownloadLinksDataFlavor();

    private DownloadLinksDataFlavor() {
        /*
         * It is important to use this constructor, without it dragdrop will not
         * work correctly, we want to reuse the Content and not
         * serialize/deserialize it every time
         */
        super(DownloadLink.class, "DownloadLinks");
    }

    /*
     * we want to reuse the Content and not serialize/deserialize it every time
     */
    @Override
    public boolean isFlavorSerializedObjectType() {
        return false;
    }
}
