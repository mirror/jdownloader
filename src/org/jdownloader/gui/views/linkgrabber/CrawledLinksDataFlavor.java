package org.jdownloader.gui.views.linkgrabber;

import java.awt.datatransfer.DataFlavor;

import jd.controlling.linkcrawler.CrawledLink;

public class CrawledLinksDataFlavor extends DataFlavor {

    public static CrawledLinksDataFlavor Flavor = new CrawledLinksDataFlavor();

    private CrawledLinksDataFlavor() {
        /*
         * It is important to use this constructor, without it dragdrop will not
         * work correctly, we want to reuse the Content and not
         * serialize/deserialize it every time
         */
        super(CrawledLink.class, "CrawledLinks");
    }

    /*
     * we want to reuse the Content and not serialize/deserialize it every time
     */
    @Override
    public boolean isRepresentationClassSerializable() {
        return false;
    }
}
