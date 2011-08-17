package jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork;

import jd.controlling.proxy.ProxyInfo;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;

public class HostColumn extends ExtTextColumn<ProxyInfo> {

    public HostColumn() {
        super(_GUI._.gui_column_host());

    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public String getStringValue(ProxyInfo value) {
        switch (value.getProxy().getType()) {
        case NONE:
            return "";
        case DIRECT:
            return value.getProxy().getLocalIP().getHostAddress();
        default:
            return value.getProxy().getHost();
        }
    }

    @Override
    public boolean isEditable(ProxyInfo obj) {
        return !obj.getProxy().isLocal();
    }

    @Override
    public boolean isEnabled(ProxyInfo obj) {
        return true;
    }

    @Override
    public void setValue(Object value, ProxyInfo object) {
        super.setValue(value, object);
    }

    @Override
    public int getDefaultWidth() {
        return 200;
    }

    @Override
    public int getMinWidth() {
        return 100;
    }

    @Override
    public boolean isHidable() {
        return false;
    }

}
