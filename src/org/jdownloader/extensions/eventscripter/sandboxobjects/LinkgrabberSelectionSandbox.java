package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.util.List;

import org.jdownloader.gui.views.SelectionInfo;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

public class LinkgrabberSelectionSandbox {

    private SelectionInfo<CrawledPackage, CrawledLink> selectionInfo;

    public LinkgrabberSelectionSandbox(SelectionInfo<CrawledPackage, CrawledLink> selectionInfo) {
        this.selectionInfo = selectionInfo;
    }

    public LinkgrabberSelectionSandbox() {
        // dummy
    }

    public CrawledLinkSandbox[] getLinks() {
        if (selectionInfo == null) {
            return null;
        }
        List<CrawledLink> childs = selectionInfo.getChildren();
        CrawledLinkSandbox[] ret = new CrawledLinkSandbox[childs.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new CrawledLinkSandbox(childs.get(i));
        }
        return ret;

    }

    public CrawledPackageSandbox getContextPackage() {
        if (selectionInfo == null) {
            return new CrawledPackageSandbox();
        }
        CrawledPackage cl = selectionInfo.getContextPackage();
        return cl == null ? null : new CrawledPackageSandbox(cl);
    }

    public CrawledLinkSandbox getContextLink() {
        if (selectionInfo == null) {
            return new CrawledLinkSandbox();
        }
        CrawledLink cl = selectionInfo.getContextLink();
        return cl == null ? null : new CrawledLinkSandbox(cl);
    }

}
