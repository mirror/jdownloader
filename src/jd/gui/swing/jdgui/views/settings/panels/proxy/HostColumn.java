package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.Icon;

import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.controlling.proxy.NoProxySelector;
import jd.controlling.proxy.PacProxySelectorImpl;
import jd.controlling.proxy.SingleBasicProxySelectorImpl;
import jd.controlling.proxy.SingleDirectGatewaySelector;

import org.appwork.storage.JSonStorage;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;

public class HostColumn extends ExtTextColumn<AbstractProxySelectorImpl> {

    public HostColumn() {
        super(_GUI._.gui_column_host2());

    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    protected Icon getIcon(AbstractProxySelectorImpl value) {

        return super.getIcon(value);
    }

    @Override
    public boolean isSortable(final AbstractProxySelectorImpl obj) {
        return false;
    }

    @Override
    protected void setStringValue(String value, AbstractProxySelectorImpl object) {
        if (object instanceof NoProxySelector) {
            java.awt.Toolkit.getDefaultToolkit().beep();
            return;
        } else if (object instanceof SingleDirectGatewaySelector) {

            try {
                ((SingleDirectGatewaySelector) object).getProxy().setLocalIP(InetAddress.getByName(value));
            } catch (UnknownHostException e) {
                java.awt.Toolkit.getDefaultToolkit().beep();
                return;
            }
            return;
        } else if (object instanceof SingleBasicProxySelectorImpl) {
            ((SingleBasicProxySelectorImpl) object).setHost(value);
            return;
        } else if (object instanceof PacProxySelectorImpl) {
            ((PacProxySelectorImpl) object).setPACUrl(value);
            return;
        }
        throw new IllegalStateException("Unknown Factory Type: " + object.getClass());

    }

    @Override
    public String getStringValue(AbstractProxySelectorImpl value) {
        try {
            if (value instanceof NoProxySelector) {
                return "";
            } else if (value instanceof SingleDirectGatewaySelector) {
                return ((SingleDirectGatewaySelector) value).getProxy().getLocalIP().getHostAddress();
            } else if (value instanceof SingleBasicProxySelectorImpl) {
                return ((SingleBasicProxySelectorImpl) value).getProxy().getHost();
            } else if (value instanceof PacProxySelectorImpl) {

                return ((PacProxySelectorImpl) value).getPACUrl();
            }
            throw new IllegalStateException("Unknown Factory Type: " + value.getClass());
        } catch (Throwable e) {
            return "Invalid Proxy: " + JSonStorage.serializeToJson(value.toProxyData());
        }

    }

    @Override
    public boolean isEditable(AbstractProxySelectorImpl value) {
        if (value instanceof NoProxySelector) {
            return false;
        } else if (value instanceof SingleDirectGatewaySelector) {
            return true;
        }
        return true;
    }

    @Override
    public boolean isEnabled(AbstractProxySelectorImpl obj) {
        return true;
    }

    @Override
    public void setValue(Object value, AbstractProxySelectorImpl object) {
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
