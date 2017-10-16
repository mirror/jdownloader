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

    @Override
    public int hashCode() {
        if (selectionInfo != null) {
            return selectionInfo.hashCode();
        } else {
            return super.hashCode();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DownloadlistSelectionSandbox) {
            return ((DownloadlistSelectionSandbox) obj).selectionInfo == selectionInfo;
        } else {
            return super.equals(obj);
        }
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

    public boolean isLinkContext() {
        if (selectionInfo != null) {
            return selectionInfo.isLinkContext();
        } else {
            return false;
        }
    }

    public boolean isPackageContext() {
        if (selectionInfo != null) {
            return selectionInfo.isPackageContext();
        } else {
            return false;
        }
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
        } else if (isPackageContext()) {
            final FilePackage cl = selectionInfo.getContextPackage();
            return cl == null ? null : new FilePackageSandBox(cl);
        } else {
            return null;
        }
    }

    public DownloadLinkSandBox getContextLink() {
        if (selectionInfo == null) {
            return new DownloadLinkSandBox();
        } else if (isLinkContext()) {
            final DownloadLink cl = selectionInfo.getContextLink();
            return cl == null ? null : new DownloadLinkSandBox(cl);
        } else {
            return null;
        }
    }
}
