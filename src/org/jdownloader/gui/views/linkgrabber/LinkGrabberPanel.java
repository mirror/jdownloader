package org.jdownloader.gui.views.linkgrabber;

import javax.swing.JScrollPane;

import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import net.miginfocom.swing.MigLayout;

public class LinkGrabberPanel extends SwitchPanel {
    private LinkGrabberTableModel tableModel;
    private LinkGrabberTable      table;
    private JScrollPane           tableScrollPane;

    public LinkGrabberPanel() {
        super(new MigLayout("ins 0, wrap 1", "[grow, fill]", "[grow, fill]"));
        tableModel = new LinkGrabberTableModel();
        table = new LinkGrabberTable(tableModel);
        tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBorder(null);
        this.add(tableScrollPane, "cell 0 0");
        // this.bottomBar = new BottomBar(table);
        // add(bottomBar, "dock south,hidemode 3");
    }

    @Override
    protected void onShow() {
    }

    @Override
    protected void onHide() {
    }

}
