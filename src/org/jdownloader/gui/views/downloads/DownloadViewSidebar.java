package org.jdownloader.gui.views.downloads;



import org.appwork.swing.MigPanel;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.updatev2.gui.LAFOptions;

public class DownloadViewSidebar extends MigPanel {

    public DownloadViewSidebar(DownloadsTable table) {
        super("ins 0,wrap 1", "[grow,fill]", "[]");

        LAFOptions.getInstance().applyPanelBackground(this);
    }
}
