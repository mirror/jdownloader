package org.jdownloader.gui.views.downloads;

import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.menu.ChunksEditor;
import jd.gui.swing.jdgui.menu.DownloadOverView;
import jd.gui.swing.jdgui.menu.ParalellDownloadsEditor;
import jd.gui.swing.jdgui.menu.ParallelDownloadsPerHostEditor;
import jd.gui.swing.jdgui.menu.SpeedlimitEditor;

public class QuickSettingsPopup extends JPopupMenu {
    public QuickSettingsPopup() {
        super();
        add(new ChunksEditor());
        add(new ParalellDownloadsEditor());
        add(new ParallelDownloadsPerHostEditor());
        add(new SpeedlimitEditor());
        add(new DownloadOverView());
    }
}
