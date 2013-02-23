package org.jdownloader.gui.views.downloads.overviewpanel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.Timer;

import jd.controlling.downloadcontroller.DownloadController;
import jd.gui.swing.jdgui.menu.ChunksEditor;
import jd.gui.swing.jdgui.menu.ParalellDownloadsEditor;
import jd.gui.swing.jdgui.menu.ParallelDownloadsPerHostEditor;
import jd.gui.swing.jdgui.menu.SpeedlimitEditor;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;

public class DownloadOverview extends MigPanel implements ActionListener {

    private DownloadsTable downloadTable;
    private JLabel         packageCount;
    private JLabel         linkCount;

    public DownloadOverview(DownloadsTable table) {
        super("ins 0", "[][grow,fill][][]", "");
        this.downloadTable = table;
        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor();

        if (c >= 0) {
            setBackground(new Color(c));
            setOpaque(true);
        }
        MigPanel info = new MigPanel("ins 2 0 0 0 ,wrap 2", "[grow]10[grow]", "[]2[]");
        packageCount = new JLabel();
        linkCount = new JLabel();
        // total size
        // selected
        // filtered
        // speed
        // eta
        info.add(createHeaderLabel(_GUI._.DownloadOverview_DownloadOverview_packages()), "alignx right");
        info.add(packageCount);
        info.add(createHeaderLabel(_GUI._.DownloadOverview_DownloadOverview_links()), "newline,alignx right");
        info.add(linkCount);
        MigPanel settings = new MigPanel("ins 2 0 0 0 ,wrap 2", "[fill][fill]", "[]2[]");
        SwingUtils.setOpaque(settings, false);
        settings.add(new ChunksEditor(true), "height 20!");
        settings.add(new ParalellDownloadsEditor(true));
        settings.add(new ParallelDownloadsPerHostEditor(true));
        settings.add(new SpeedlimitEditor(true));
        add(info);
        add(Box.createHorizontalGlue());
        add(new JSeparator(JSeparator.VERTICAL), "pushy,growy");
        add(settings);

        new Timer(1000, this).start();
    }

    private JComponent createHeaderLabel(String label) {
        JLabel lbl = new JLabel(label);
        SwingUtils.toBold(lbl);
        lbl.setEnabled(false);
        return lbl;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        packageCount.setText(DownloadController.getInstance().size() + "");
    }

}
