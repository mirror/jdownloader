package org.jdownloader.gui.views.downloads.properties;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public class DownloadLinkPropertiesPanel extends DownloadListPropertiesPanel<DownloadLinkNodeProperties> {

    @Override
    protected DownloadLinkNodeProperties createAbstractNodeProperties(AbstractNode abstractNode) {
        return new DownloadLinkNodeProperties((DownloadLink) abstractNode);
    }

    @Override
    protected void refreshOnDownloadLinkUpdate(DownloadLink downloadlink) {
        final DownloadLinkNodeProperties current = getAbstractNodeProperties();
        if (current != null && !current.isDifferent(downloadlink)) {
            refresh();
        }
    }

    @Override
    protected void refreshOnFilePackageUpdate(final FilePackage pkg) {
        final DownloadLinkNodeProperties current = getAbstractNodeProperties();
        if (current != null && current.samePackage(pkg)) {
            refresh();
        }
    }

}
