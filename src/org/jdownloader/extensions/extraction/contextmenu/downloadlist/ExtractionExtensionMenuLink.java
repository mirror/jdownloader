package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import javax.swing.JComponent;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.extensions.AbstractExtensionContextMenuLink;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadTableContext;

public class ExtractionExtensionMenuLink extends AbstractExtensionContextMenuLink<ExtractionExtension> {
    public ExtractionExtensionMenuLink() {
        setName(org.jdownloader.extensions.extraction.translate.T._.linkname());
        setIconKey("archive");
    }

    @Override
    protected void link(JComponent root, SelectionInfo<?, ?> selection, ExtractionExtension extension) {

        extension.onExtendPopupMenu(new DownloadTableContext(root, (SelectionInfo<FilePackage, DownloadLink>) selection, selection.getContextColumn()));
    }

}
