package org.jdownloader.gui.views;

import javax.swing.JScrollPane;

import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import net.miginfocom.swing.MigLayout;

import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;

public class DownloadsPanel extends SwitchPanel {

    /**
     * 
     */
    private static final long   serialVersionUID = -2610465878903778445L;
    private DownloadsTable      table;
    private JScrollPane         tableScrollPane;
    private DownloadsTableModel tableModel;

    public DownloadsPanel() {
        super(new MigLayout("ins 0, wrap 1", "[grow, fill]", "[grow, fill]"));
        tableModel = new DownloadsTableModel();
        table = new DownloadsTable(tableModel);
        tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBorder(null);
        this.add(tableScrollPane, "cell 0 0");
    }

    @Override
    protected void onShow() {
        tableModel.recreateModel();
    }

    @Override
    protected void onHide() {
    }

}
