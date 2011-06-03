package jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork;

import java.awt.Component;
import java.net.InetAddress;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import jd.controlling.proxy.ProxyInfo;

import org.appwork.utils.net.httpconnection.HTTPProxyUtils;
import org.appwork.utils.swing.table.ExtColumn;
import org.appwork.utils.swing.table.columns.ExtComboColumn;
import org.appwork.utils.swing.table.columns.ExtCompoundColumn;
import org.appwork.utils.swing.table.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;

public class HostColumn extends ExtCompoundColumn<ProxyInfo> {

    private ExtTextColumn<ProxyInfo>  txt;
    private ExtComboColumn<ProxyInfo> combo;

    public HostColumn() {
        super(_GUI._.gui_column_host());
        txt = new ExtTextColumn<ProxyInfo>(getName()) {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            public String getStringValue(ProxyInfo value) {
                switch (value.getProxy().getType()) {
                case NONE:
                    return "";
                default:
                    return value.getProxy().getHost();
                }
            }

            @Override
            public boolean isEditable(ProxyInfo obj) {
                return !obj.getProxy().isNone();
            }

            @Override
            public boolean isEnabled(ProxyInfo obj) {
                return true;
            }

            @Override
            public void setValue(Object value, ProxyInfo object) {
                super.setValue(value, object);
            }

        };

        final DefaultComboBoxModel comboModel = new DefaultComboBoxModel(HTTPProxyUtils.getLocalIPs().toArray(new InetAddress[] {}));
        this.combo = new ExtComboColumn<ProxyInfo>(getName(), comboModel) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            {
                final ListCellRenderer def = getRenderer();
                setRenderer(new ListCellRenderer() {
                    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        return def.getListCellRendererComponent(list, _GUI._.gui_column_host_direct(((InetAddress) value).getHostAddress()), index, isSelected, cellHasFocus);
                    }
                });

            }

            @Override
            public boolean isEnabled(ProxyInfo obj) {
                return true;
            }

            @Override
            protected int getSelectedIndex(ProxyInfo value) {
                for (int i = 0; i < comboModel.getSize(); i++) {
                    if (((InetAddress) comboModel.getElementAt(i)).equals(value.getProxy().getLocalIP())) { return i; }
                }

                return 0;
            }

            @Override
            protected void setSelectedIndex(int value, ProxyInfo object) {
                object.getProxy().setLocalIP((InetAddress) comboModel.getElementAt(value));
            }

        };
    }

    @Override
    public boolean isEditable(ProxyInfo obj) {
        return super.isEditable(obj);
    }

    @Override
    public boolean isEnabled(ProxyInfo obj) {
        return true;
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

    @Override
    public String getSortString(ProxyInfo object) {
        switch (object.getProxy().getType()) {
        case DIRECT:
            return _GUI._.gui_column_host_direct(((InetAddress) object.getProxy().getLocalIP()).getHostAddress());
        default:
            return txt.getStringValue(object);
        }
    }

    @Override
    public ExtColumn<ProxyInfo> selectColumn(ProxyInfo object) {
        switch (object.getProxy().getType()) {
        case DIRECT:
            return combo;
        default:
            return txt;
        }
    }

}
