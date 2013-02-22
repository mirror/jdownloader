package org.jdownloader.gui.views.downloads.overviewpanel;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JSeparator;

import jd.gui.swing.jdgui.menu.ChunksEditor;
import jd.gui.swing.jdgui.menu.ParalellDownloadsEditor;
import jd.gui.swing.jdgui.menu.ParallelDownloadsPerHostEditor;
import jd.gui.swing.jdgui.menu.SpeedlimitEditor;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;

public class DownloadOverview extends MigPanel {

    private DownloadsTable downloadTable;

    public DownloadOverview(DownloadsTable table) {
        super("ins 0", "[grow,fill][][]", "");
        this.downloadTable = table;
        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor();

        if (c >= 0) {
            setBackground(new Color(c));
            setOpaque(true);
        }
        add(new JLabel("Blabla"), "gaptop 3,gapbottom 3");
        add(new JSeparator(JSeparator.VERTICAL), "pushy,growy");

        MigPanel settings = new MigPanel("ins 0,wrap 2", "[fill][fill]", "[]0[]");
        SwingUtils.setOpaque(settings, false);
        settings.add(new ChunksEditor());
        settings.add(new ParalellDownloadsEditor());
        settings.add(new ParallelDownloadsPerHostEditor());
        settings.add(new SpeedlimitEditor());
        add(settings);
    }

}
