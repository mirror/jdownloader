package org.jdownloader.extensions.eventscripter.sandboxobjects;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.gui.views.SelectionInfo;

public class LinkgrabberSelectionSandbox {

    private SelectionInfo<CrawledPackage, CrawledLink> selectionInfo;

    public LinkgrabberSelectionSandbox(SelectionInfo<CrawledPackage, CrawledLink> selectionInfo) {
        this.selectionInfo = selectionInfo;
    }

    public LinkgrabberSelectionSandbox() {
        // dummy
    }

}
