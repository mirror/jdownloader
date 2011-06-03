package jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork;

import jd.controlling.proxy.ProxyInfo;
import jd.gui.swing.jdgui.views.settings.panels.components.SettingsTable;

public class ProxyTable extends SettingsTable<ProxyInfo> {

    public ProxyTable() {
        super(new ProxyTableModel());
    }

    public void update() {
        ((ProxyTableModel) getExtTableModel()).fill();
    }

}
