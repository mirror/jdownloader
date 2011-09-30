package org.jdownloader.gui.views.linkgrabber;

import java.awt.datatransfer.DataFlavor;

import jd.controlling.linkcrawler.CrawledPackage;

public class CrawledPackagesDataFlavor extends DataFlavor {

    public static CrawledPackagesDataFlavor Flavor = new CrawledPackagesDataFlavor();

    private CrawledPackagesDataFlavor() {
        /*
         * It is important to use this constructor, without it dragdrop will not
         * work correctly, we want to reuse the Content and not
         * serialize/deserialize it every time
         */
        super(CrawledPackage.class, "CrawledPackages");
    }

    /*
     * we want to reuse the Content and not serialize/deserialize it every time
     */
    @Override
    public boolean isRepresentationClassSerializable() {
        return false;
    }
}
