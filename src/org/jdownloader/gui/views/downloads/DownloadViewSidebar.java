package org.jdownloader.gui.views.downloads;

import java.awt.Color;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.MigPanel;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;

public class DownloadViewSidebar extends MigPanel {

    public DownloadViewSidebar(DownloadsTable table) {
        super("ins 0,wrap 1", "[grow,fill]", "[]");
        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor();

        if (c >= 0) {
            setBackground(new Color(c));
            setOpaque(true);
        }

    }
}
