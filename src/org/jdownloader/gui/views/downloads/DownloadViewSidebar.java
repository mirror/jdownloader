package org.jdownloader.gui.views.downloads;


import jd.gui.swing.laf.LAFOptions;

import org.appwork.swing.MigPanel;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;

public class DownloadViewSidebar extends MigPanel {

    public DownloadViewSidebar(DownloadsTable table) {
        super("ins 0,wrap 1", "[grow,fill]", "[]");

        LAFOptions.getInstance().applyPanelBackground(this);
    }
}
