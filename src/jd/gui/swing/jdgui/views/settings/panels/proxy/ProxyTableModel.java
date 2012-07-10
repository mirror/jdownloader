package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.awt.Component;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import jd.controlling.IOEQ;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.ProxyInfo;

import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtComboColumn;
import org.appwork.swing.exttable.columns.ExtPasswordEditorColumn;
import org.appwork.swing.exttable.columns.ExtRadioColumn;
import org.appwork.swing.exttable.columns.ExtSpinnerColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ProxyTableModel extends ExtTableModel<ProxyInfo> {

    private static final long serialVersionUID = -5584463272737285033L;

    public ProxyTableModel() {
        super("proxyTable2");
    }

    @Override
    protected void initColumns() {

        DefaultComboBoxModel model = new DefaultComboBoxModel(new String[] { _GUI._.gui_column_proxytype_http(), _GUI._.gui_column_proxytype_socks5(), _GUI._.gui_column_proxytype_socks4() });
        this.addColumn(new ExtComboColumn<ProxyInfo>(_GUI._.gui_column_proxytype(), model) {

            @Override
            public boolean isEditable(ProxyInfo obj) {

                switch (obj.getType()) {

                case NONE:
                case DIRECT:
                    return false;
                default:
                    return true;
                }
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
            public String getStringValue(ProxyInfo value) {
                switch (value.getType()) {

                case NONE:
                    return _GUI._.gui_column_proxytype_no_proxy();
                case DIRECT:
                    return _GUI._.gui_column_proxytype_direct();

                }
                throw new RuntimeException("Unknown Proxy Type");

            }

            @Override
            protected String getTooltipText(ProxyInfo obj) {
                switch (obj.getType()) {

                case NONE:
                    return _GUI._.gui_column_proxytype_no_proxy_tt();
                case DIRECT:
                    return _GUI._.gui_column_proxytype_direct_tt();
                case HTTP:
                    return _GUI._.gui_column_proxytype_http_tt();
                case SOCKS5:
                    return _GUI._.gui_column_proxytype_socks5_tt();
                case SOCKS4:
                    return _GUI._.gui_column_proxytype_socks4_tt();
                default:
                    throw new RuntimeException("Unknown Proxy Type");
                }
            }

            @Override
            public boolean isEnabled(ProxyInfo obj) {
                return true;
            }

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            protected int getSelectedIndex(ProxyInfo value) {
                switch (value.getType()) {
                case DIRECT:
                case NONE:
                    return -1;
                case HTTP:
                    return 0;

                case SOCKS5:
                    return 1;

                case SOCKS4:
                    return 2;
                default:
                    throw new RuntimeException("Unknown Proxy Type");
                }

            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected void setSelectedIndex(int value, ProxyInfo object) {
                switch (value) {
                // case 0:
                // object.getProxy().setType(HTTPProxy.TYPE.NONE);
                // return;
                // case 0:
                // object.getProxy().setType(HTTPProxy.TYPE.DIRECT);
                // return;
                case 0:
                    object.setType(HTTPProxy.TYPE.HTTP);
                    return;
                case 1:
                    object.setType(HTTPProxy.TYPE.SOCKS5);
                    return;
                case 2:
                    object.setType(HTTPProxy.TYPE.SOCKS4);
                    return;
                default:
                    throw new RuntimeException("Unknown Proxy Type");
                }
            }
        });

        this.addColumn(new HostColumn());
        this.addColumn(new ExtTextColumn<ProxyInfo>(_GUI._.gui_column_user(), this) {

            private static final long serialVersionUID = -7209180150340921804L;

            @Override
            public boolean isEditable(final ProxyInfo obj) {
                if (obj.isLocal()) return false;
                return true;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected void setStringValue(String value, ProxyInfo object) {
                if (object.isLocal()) return;
                object.setUser(value);
            }

            @Override
            public String getStringValue(ProxyInfo value) {
                if (value.isLocal()) return "";
                return value.getUser();
            }

        });
        this.addColumn(new ExtPasswordEditorColumn<ProxyInfo>(_GUI._.gui_column_pass(), this) {

            private static final long serialVersionUID = -7209180150340921804L;

            @Override
            public boolean isEditable(final ProxyInfo obj) {
                if (obj.isLocal()) return false;
                return true;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected void setStringValue(String value, ProxyInfo object) {
                if (object.isLocal()) return;
                object.setPass(value);
            }

            @Override
            public String getStringValue(ProxyInfo value) {

                if (value.isLocal()) return "";
                return super.getStringValue(value);
            }

            @Override
            protected String getPlainStringValue(ProxyInfo value) {
                if (value.isLocal()) return "";
                return value.getPass();
            }

        });
        this.addColumn(new ExtSpinnerColumn<ProxyInfo>(_GUI._.gui_column_port()) {

            /**
			 * 
			 */
            private static final long serialVersionUID = 7770193565380136904L;

            @Override
            protected Number getNumber(ProxyInfo value) {
                return value.getPort();
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public boolean isEditable(final ProxyInfo obj) {
                if (obj.isLocal()) return false;
                return true;
            }

            @Override
            protected void setNumberValue(Number value, ProxyInfo object) {
                object.setPort(value.intValue());
            }

            @Override
            public String getStringValue(ProxyInfo value) {
                if (value.isLocal()) return "";
                return value.getPort() + "";
            }
        });

        this.addColumn(new ExtCheckColumn<ProxyInfo>(_GUI._.gui_column_use(), this) {

            private static final long serialVersionUID = -4667150369226691276L;

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    private static final long serialVersionUID = 3938290423337000265L;

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("proxy_rotate", 14));
                        // defaultProxy
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        setToolTipText(_GUI._.gui_column_proxytype_rotation_check());
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public int getMaxWidth() {
                return 30;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected boolean getBooleanValue(ProxyInfo value) {
                return value.isProxyRotationEnabled();
            }

            @Override
            public boolean isEditable(ProxyInfo obj) {
                return true;
            }

            @Override
            protected void setBooleanValue(final boolean value, final ProxyInfo object) {

                ProxyController.getInstance().setproxyRotationEnabled(object, value);
                if (!value) {
                    if (ProxyController.getInstance().hasRotation() == false) {
                        if (object == ProxyController.getInstance().getNone()) {
                            Dialog.getInstance().showMessageDialog(_GUI._.proxytablemodel_atleast_one_rotate_required());
                            ProxyController.getInstance().setproxyRotationEnabled(object, true);
                        } else {
                            Dialog.getInstance().showMessageDialog(_GUI._.proxytablemodel_atleast_one_rotate_required());
                            ProxyController.getInstance().setproxyRotationEnabled(ProxyController.getInstance().getNone(), true);
                        }
                    }
                }

            }
        });
        this.addColumn(new ExtRadioColumn<ProxyInfo>(_GUI._.gui_column_defaultproxy(), this) {
            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    private static final long serialVersionUID = 3938290423337000265L;

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("defaultProxy", 14));//
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        setToolTipText(_GUI._.gui_column_proxytype_default());
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public int getMaxWidth() {
                return 30;
            }

            private static final long serialVersionUID = 6843580898685333774L;

            @Override
            public boolean isEditable(ProxyInfo obj) {
                return true;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected boolean getBooleanValue(ProxyInfo value) {
                return ProxyController.getInstance().getDefaultProxy() == value;
            }

            @Override
            protected void setBooleanValue(boolean value, final ProxyInfo object) {
                IOEQ.add(new Runnable() {
                    public void run() {
                        ProxyController.getInstance().setDefaultProxy(object);
                    }
                });
            }
        });
    }
}