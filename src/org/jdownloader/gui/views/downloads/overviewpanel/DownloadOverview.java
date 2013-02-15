package org.jdownloader.gui.views.downloads.overviewpanel;

import java.awt.Color;

import javax.swing.JLabel;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.MigPanel;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;

public class DownloadOverview extends MigPanel {

    private DownloadsTable downloadTable;

    public DownloadOverview(DownloadsTable table) {
        super("ins 0", "", "");
        this.downloadTable = table;
        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor();

        if (c >= 0) {
            setBackground(new Color(c));
            setOpaque(true);
        }
        add(new JLabel("Still empty here... you will find stats of all downloads here."));
    }

}
