package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.awt.Component;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.ProxyInfo;

import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtComboColumn;
import org.appwork.swing.exttable.columns.ExtPasswordEditorColumn;
import org.appwork.swing.exttable.columns.ExtSpinnerColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.net.httpconnection.HTTPProxy.TYPE;
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

        DefaultComboBoxModel<TYPE> model = new DefaultComboBoxModel<TYPE>(new TYPE[] { TYPE.HTTP, TYPE.SOCKS5, TYPE.SOCKS4 });
        this.addColumn(new ExtComboColumn<ProxyInfo, org.appwork.utils.net.httpconnection.HTTPProxy.TYPE>(_GUI._.gui_column_proxytype(), model) {

            @Override
            public boolean isEditable(ProxyInfo obj) {
                if (obj == null) return false;
                switch (obj.getType()) {

                case NONE:
                case DIRECT:
                    return false;
                default:
                    return true;
                }
            }

            protected boolean isDefaultResizable() {

                return false;
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
            protected String modelItemToString(TYPE selectedItem) {
                switch (selectedItem) {
                case DIRECT:
                    return _GUI._.gui_column_proxytype_direct();
                case HTTP:
                    return _GUI._.gui_column_proxytype_http();
                case NONE:
                    return _GUI._.gui_column_proxytype_no_proxy();
                case SOCKS4:
                    return _GUI._.gui_column_proxytype_socks4();
                case SOCKS5:
                    return _GUI._.gui_column_proxytype_socks5();
                }
                return null;

            }

            @Override
            public boolean isEnabled(ProxyInfo obj) {
                return true;
            }

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            // @Override
            // protected int getSelectedIndex(ProxyInfo value) {
            // switch (value.getType()) {
            // case DIRECT:
            // case NONE:
            // return -1;
            // case HTTP:
            // return 0;
            //
            // case SOCKS5:
            // return 1;
            //
            // case SOCKS4:
            // return 2;
            // default:
            // throw new RuntimeException("Unknown Proxy Type");
            // }
            //
            // }

            @Override
            public boolean isHidable() {
                return false;
            }

            //
            // @Override
            // protected void setSelectedIndex(int value, ProxyInfo object) {
            // switch (value) {
            // // case 0:
            // // object.getProxy().setType(HTTPProxy.TYPE.NONE);
            // // return;
            // // case 0:
            // // object.getProxy().setType(HTTPProxy.TYPE.DIRECT);
            // // return;
            // case 0:
            // object.setType(HTTPProxy.TYPE.HTTP);
            // return;
            // case 1:
            // object.setType(HTTPProxy.TYPE.SOCKS5);
            // return;
            // case 2:
            // object.setType(HTTPProxy.TYPE.SOCKS4);
            // return;
            // default:
            // throw new RuntimeException("Unknown Proxy Type");
            // }
            // }

            @Override
            protected TYPE getSelectedItem(ProxyInfo object) {
                return object.getType();
            }

            @Override
            protected void setSelectedItem(ProxyInfo object, TYPE value) {
                object.setType(value);
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

            public int getDefaultWidth() {

                return 100;
            }

            protected boolean isDefaultResizable() {

                return false;
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

        this.addColumn(new ExtCheckColumn<ProxyInfo>(_GUI._.gui_column_nativeauth(), this) {

            private static final long serialVersionUID = -4667150369226691276L;

            @Override
            public int getMaxWidth() {
                return 60;
            }

            public boolean isDefaultVisible() {
                return false;
            }

            @Override
            protected boolean getBooleanValue(ProxyInfo value) {
                return value.isPreferNativeImplementation();
            }

            @Override
            public boolean isEditable(ProxyInfo obj) {
                switch (obj.getType()) {
                case HTTP:
                    return true;
                default:
                    return false;
                }
            }

            @Override
            protected void setBooleanValue(final boolean value, final ProxyInfo object) {
                object.setPreferNativeImplementation(value);
            }
        });
    }
}