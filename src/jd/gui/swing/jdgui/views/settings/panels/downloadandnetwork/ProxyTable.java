package jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork;

import jd.controlling.proxy.ProxyInfo;
import jd.gui.swing.jdgui.BasicJDTable;

public class ProxyTable extends BasicJDTable<ProxyInfo> {

    public ProxyTable() {
        super(new ProxyTableModel());
    }

    public void update() {
        ((ProxyTableModel) getExtTableModel()).fill();
    }

}
