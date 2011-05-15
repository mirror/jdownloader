package jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork;

import java.util.ArrayList;

import javax.swing.Icon;

import jd.controlling.IOEQ;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.ProxyEvent;
import jd.controlling.proxy.ProxyInfo;
import jd.utils.JDTheme;

import org.appwork.utils.event.DefaultEventListener;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.columns.ExtCheckColumn;
import org.appwork.utils.swing.table.columns.ExtPasswordEditorColumn;
import org.appwork.utils.swing.table.columns.ExtRadioColumn;
import org.appwork.utils.swing.table.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;

public class ProxyTableModel extends ExtTableModel<ProxyInfo> implements DefaultEventListener<ProxyEvent<ProxyInfo>> {

    private static final long serialVersionUID = -5584463272737285033L;
    private Icon              icon;

    public ProxyTableModel() {
        super("proxyTable");
        tableData = new ArrayList<ProxyInfo>(ProxyController.getInstance().getList());
        icon = JDTheme.II("gui.images.proxy", 20, 20);
        ProxyController.getInstance().getEventSender().addListener(this);
    }

    @Override
    protected void initColumns() {
        this.addColumn(new ExtCheckColumn<ProxyInfo>(_GUI._.gui_column_use(), this) {

            private static final long serialVersionUID = 6843580898685333774L;

            @Override
            public boolean isEditable(ProxyInfo obj) {
                return true;
            }

            @Override
            protected boolean getBooleanValue(ProxyInfo value) {
                return value.isEnabled();
            }

            @Override
            protected void setBooleanValue(final boolean value, final ProxyInfo object) {
                IOEQ.add(new Runnable() {
                    public void run() {
                        ProxyController.getInstance().setEnabled(object, value);
                    }
                });
            }
        });
        this.addColumn(new ExtTextColumn<ProxyInfo>(_GUI._.gui_column_host(), this) {

            private static final long serialVersionUID = -7209180150340921804L;

            @Override
            protected String getStringValue(ProxyInfo value) {
                if (value.getProxy().isDirect()) return "DIRECT";
                if (value.getProxy().isNone()) return "NONE";
                return value.getProxy().getHost();
            }

            @Override
            public boolean isHidable() {
                return false;
            }

        });
        this.addColumn(new ExtTextColumn<ProxyInfo>(_GUI._.gui_column_user(), this) {

            private static final long serialVersionUID = -7209180150340921804L;

            @Override
            public boolean isEditable(final ProxyInfo obj) {
                if (obj.getProxy().isLocal()) return false;
                return true;
            }

            @Override
            protected void setStringValue(String value, ProxyInfo object) {
                if (object.getProxy().isLocal()) return;
                object.getProxy().setUser(value);
            }

            @Override
            protected String getStringValue(ProxyInfo value) {
                if (value.getProxy().isLocal()) return "";
                return value.getProxy().getUser();
            }

        });
        this.addColumn(new ExtPasswordEditorColumn<ProxyInfo>(_GUI._.gui_column_pass(), this) {

            private static final long serialVersionUID = -7209180150340921804L;

            @Override
            public boolean isEditable(final ProxyInfo obj) {
                if (obj.getProxy().isLocal()) return false;
                return true;
            }

            @Override
            protected void setStringValue(String value, ProxyInfo object) {
                if (object.getProxy().isLocal()) return;
                object.getProxy().setPass(value);
            }

            @Override
            protected String getStringValue(ProxyInfo value) {
                if (value.getProxy().isLocal()) return "";
                return "******";
            }

            @Override
            protected String getPlainStringValue(ProxyInfo value) {
                if (value.getProxy().isLocal()) return "";
                return value.getProxy().getPass();
            }

        });
        this.addColumn(new ExtTextColumn<ProxyInfo>(_GUI._.gui_column_port(), this) {

            private static final long serialVersionUID = -7209180150340921804L;

            @Override
            protected String getStringValue(ProxyInfo value) {
                if (value.getProxy().isLocal()) return "";
                return value.getProxy().getPort() + "";
            }

        });

        this.addColumn(new ExtTextColumn<ProxyInfo>(_GUI._.gui_column_proxystatus(), this) {

            private static final long serialVersionUID = -7209180150340921804L;

            @Override
            protected String getStringValue(ProxyInfo value) {
                return value.getProxy().getStatus().name();
            }
        });
        this.addColumn(new ExtTextColumn<ProxyInfo>(_GUI._.gui_column_proxytype(), this) {

            private static final long serialVersionUID = -7209180150340921804L;

            @Override
            protected String getStringValue(ProxyInfo value) {
                return value.getProxy().getType().name();
            }

            @Override
            protected Icon getIcon(ProxyInfo value) {
                return icon;
            }
        });

        this.addColumn(new ExtRadioColumn<ProxyInfo>(_GUI._.gui_column_defaultproxy(), this) {

            private static final long serialVersionUID = 6843580898685333774L;

            @Override
            public boolean isEditable(ProxyInfo obj) {
                return true;
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

    public void onEvent(ProxyEvent<ProxyInfo> event) {
        if (event.getType().equals(ProxyEvent.Types.ADDED) || event.getType().equals(ProxyEvent.Types.REMOVED)) {
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    tableData = new ArrayList<ProxyInfo>(ProxyController.getInstance().getList());
                    ProxyTableModel.this.fireTableStructureChanged();
                }
            };
        } else {
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    ProxyTableModel.this.fireTableStructureChanged();
                }
            };
        }
    }
}