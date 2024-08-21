package org.jdownloader.gui.views.linkgrabber.contextmenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

public class MergeToPackageAction extends org.jdownloader.gui.views.linkgrabber.contextmenu.AbstractMergeToPackageAction<CrawledPackage, CrawledLink> {
    /**
     *
     */
    private static final long serialVersionUID = -4468197802870765463L;

    public MergeToPackageAction() {
        super();
    }

    @Override
    protected CrawledPackage createNewPackage(String downloadFolder) {
        final CrawledPackage newPackage = new CrawledPackage();
        newPackage.setName(getName());
        newPackage.setDownloadFolder(downloadFolder);
        return newPackage;
    }

}
