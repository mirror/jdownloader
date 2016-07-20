package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.util.List;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;

public class DownloadlistSelectionSandbox {

    private final SelectionInfo<FilePackage, DownloadLink> selectionInfo;

    public DownloadlistSelectionSandbox(SelectionInfo<FilePackage, DownloadLink> selectionInfo) {
        this.selectionInfo = selectionInfo;
    }

    public DownloadlistSelectionSandbox() {
        this(null);
    }

    public DownloadLinkSandBox[] getDownloadLinks() {
        if (selectionInfo == null) {
            return null;
        }
        final List<DownloadLink> childs = selectionInfo.getChildren();
        final DownloadLinkSandBox[] ret = new DownloadLinkSandBox[childs.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new DownloadLinkSandBox(childs.get(i));
        }
        return ret;
    }

    public FilePackageSandBox[] getPackages() {
        if (selectionInfo == null) {
            return null;
        }
        final List<PackageView<FilePackage, DownloadLink>> packageViews = selectionInfo.getPackageViews();
        final FilePackageSandBox[] ret = new FilePackageSandBox[packageViews.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new FilePackageSandBox(packageViews.get(i).getPackage());
        }
        return ret;
    }

    public FilePackageSandBox getContextPackage() {
        if (selectionInfo == null) {
            return new FilePackageSandBox();
        }
        final FilePackage cl = selectionInfo.getContextPackage();
        return cl == null ? null : new FilePackageSandBox(cl);
    }

    public DownloadLinkSandBox getContextLink() {
        if (selectionInfo == null) {
            return new DownloadLinkSandBox();
        }
        final DownloadLink cl = selectionInfo.getContextLink();
        return cl == null ? null : new DownloadLinkSandBox(cl);
    }
}
