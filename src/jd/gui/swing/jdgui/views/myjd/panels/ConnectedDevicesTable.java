package jd.gui.swing.jdgui.views.myjd.panels;

import java.awt.Dimension;

import jd.gui.swing.jdgui.BasicJDTable;

import org.jdownloader.api.useragent.ConnectedDevice;

public class ConnectedDevicesTable extends BasicJDTable<ConnectedDevice> {

    public ConnectedDevicesTable() {
        super(new ConnectedDevicesTableModel());
        setShowHorizontalLineBelowLastEntry(false);
        setShowHorizontalLines(true);

    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        Dimension dim = super.getPreferredScrollableViewportSize();
        // here we return the pref height
        dim.height = getPreferredSize().height;
        return dim;
    }
}
