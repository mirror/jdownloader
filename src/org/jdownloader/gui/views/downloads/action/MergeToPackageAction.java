package org.jdownloader.gui.views.downloads.action;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.controlling.packagizer.PackagizerController;

public class MergeToPackageAction extends org.jdownloader.gui.views.linkgrabber.contextmenu.AbstractMergeToPackageAction<FilePackage, DownloadLink> {
    private static final long serialVersionUID = -4468197802870765463L;

    public MergeToPackageAction() {
        super();
    }

    @Override
    protected FilePackage createNewPackage(final String downloadFolder) {
        final FilePackage newPackage = FilePackage.getInstance();
        final String name = getName();
        newPackage.setName(name);
        newPackage.setDownloadDirectory(PackagizerController.replaceDynamicTags(downloadFolder, name, newPackage));
        return newPackage;
    }
}
