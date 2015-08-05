package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.util.List;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.gui.views.SelectionInfo;

public class DownloadlistSelectionSandbox {

    private SelectionInfo<FilePackage, DownloadLink> selectionInfo;

    public DownloadlistSelectionSandbox(SelectionInfo<FilePackage, DownloadLink> selectionInfo) {
        this.selectionInfo = selectionInfo;
    }

    public DownloadlistSelectionSandbox() {
        // Test params

    }

    public DownloadLinkSandBox[] getDownloadLinks() {
        if (selectionInfo == null) {
            return null;
        }
        List<DownloadLink> childs = selectionInfo.getChildren();
        DownloadLinkSandBox[] ret = new DownloadLinkSandBox[childs.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new DownloadLinkSandBox(childs.get(i));
        }
        return ret;

    }

    public FilePackageSandBox getContextPackage() {
        if (selectionInfo == null) {
            return new FilePackageSandBox();
        }
        FilePackage cl = selectionInfo.getContextPackage();
        return cl == null ? null : new FilePackageSandBox(cl);
    }

    public DownloadLinkSandBox getContextLink() {
        if (selectionInfo == null) {
            return new DownloadLinkSandBox();
        }
        DownloadLink cl = selectionInfo.getContextLink();
        return cl == null ? null : new DownloadLinkSandBox(cl);
    }
}
