package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;

public class LinkgrabberSelectionSandbox {

    private final SelectionInfo<CrawledPackage, CrawledLink> selectionInfo;

    public LinkgrabberSelectionSandbox(SelectionInfo<CrawledPackage, CrawledLink> selectionInfo) {
        this.selectionInfo = selectionInfo;
    }

    public LinkgrabberSelectionSandbox() {
        this(null);
    }

    public CrawledLinkSandbox[] getLinks() {
        if (selectionInfo == null) {
            return null;
        }
        final List<CrawledLink> childs = selectionInfo.getChildren();
        final CrawledLinkSandbox[] ret = new CrawledLinkSandbox[childs.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new CrawledLinkSandbox(childs.get(i));
        }
        return ret;
    }

    public CrawledPackageSandbox[] getPackages() {
        if (selectionInfo == null) {
            return null;
        }
        final List<PackageView<CrawledPackage, CrawledLink>> packageViews = selectionInfo.getPackageViews();
        final CrawledPackageSandbox[] ret = new CrawledPackageSandbox[packageViews.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new CrawledPackageSandbox(packageViews.get(i).getPackage());
        }
        return ret;
    }

    public CrawledPackageSandbox getContextPackage() {
        if (selectionInfo == null) {
            return new CrawledPackageSandbox();
        }
        final CrawledPackage cl = selectionInfo.getContextPackage();
        return cl == null ? null : new CrawledPackageSandbox(cl);
    }

    public CrawledLinkSandbox getContextLink() {
        if (selectionInfo == null) {
            return new CrawledLinkSandbox();
        }
        final CrawledLink cl = selectionInfo.getContextLink();
        return cl == null ? null : new CrawledLinkSandbox(cl);
    }

}
