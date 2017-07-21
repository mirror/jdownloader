package jd.gui.swing.jdgui.views.settings.panels.basicauthentication;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import jd.gui.swing.jdgui.GUIUtils;

import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtComboColumn;
import org.appwork.swing.exttable.columns.ExtPasswordEditorColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeListener;
import org.jdownloader.auth.AuthenticationController;
import org.jdownloader.auth.AuthenticationInfo;
import org.jdownloader.auth.AuthenticationInfo.Type;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class AuthTableModel extends ExtTableModel<AuthenticationInfo> implements ChangeListener {
    private static final long serialVersionUID = 1L;

    public AuthTableModel() {
        super("AuthTableModel");
        AuthenticationController.getInstance().getEventSender().addListener(this, true);
    }

    @Override
    protected void initColumns() {
        this.addColumn(new ExtCheckColumn<AuthenticationInfo>(_GUI.T.authtablemodel_column_enabled()) {
            private static final long serialVersionUID = 1L;

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {
                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {
                    private static final long serialVersionUID = 1L;
                    private final Icon        ok               = NewTheme.I().getIcon(IconKey.ICON_OK, 14);

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(ok);
                        setHorizontalAlignment(CENTER);
                        setText(null);
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
            protected boolean getBooleanValue(AuthenticationInfo value) {
                return value.isEnabled();
            }

            @Override
            public boolean isEditable(AuthenticationInfo obj) {
                return true;
            }

            @Override
            protected void setBooleanValue(boolean value, AuthenticationInfo object) {
                object.setEnabled(value);
            }
        });
        this.addColumn(new ExtComboColumn<AuthenticationInfo, AuthenticationInfo.Type>(_GUI.T.authtablemodel_column_type(), null) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isHidable() {
                return false;
            }

            private ComboBoxModel<AuthenticationInfo.Type> typeModel = new DefaultComboBoxModel<AuthenticationInfo.Type>(new AuthenticationInfo.Type[] { AuthenticationInfo.Type.FTP, AuthenticationInfo.Type.HTTP });

            @Override
            protected String modelItemToString(Type selectedItem, AuthenticationInfo value) {
                switch (selectedItem) {
                case FTP:
                    return _GUI.T.authtablemodel_column_type_ftp();
                case HTTP:
                    return _GUI.T.authtablemodel_column_type_http();
                }
                return null;
            }

            @Override
            public ComboBoxModel<AuthenticationInfo.Type> updateModel(ComboBoxModel<AuthenticationInfo.Type> dataModel, AuthenticationInfo value) {
                return typeModel;
            }

            @Override
            public boolean isEditable(AuthenticationInfo obj) {
                return true;
            }

            @Override
            public boolean isEnabled(AuthenticationInfo obj) {
                return obj.isEnabled();
            }

            @Override
            public int getDefaultWidth() {
                return 60;
            }

            @Override
            public int getMinWidth() {
                return 30;
            }

            @Override
            protected Type getSelectedItem(AuthenticationInfo object) {
                return object.getType();
            }

            @Override
            protected void setSelectedItem(AuthenticationInfo object, Type value) {
                object.setType(value);
            }
        });
        this.addColumn(new ExtTextColumn<AuthenticationInfo>(_GUI.T.authtablemodel_column_host()) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public String getStringValue(AuthenticationInfo value) {
                return value.getHostmask();
            }

            @Override
            public boolean isEditable(AuthenticationInfo obj) {
                return true;
            }

            @Override
            public boolean isEnabled(AuthenticationInfo obj) {
                return obj.isEnabled();
            }

            @Override
            protected void setStringValue(String value, AuthenticationInfo object) {
                object.setHostmask(value);
            }
        });
        this.addColumn(new ExtTextColumn<AuthenticationInfo>(_GUI.T.authtablemodel_column_realm()) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isHidable() {
                return true;
            }

            @Override
            public String getStringValue(AuthenticationInfo value) {
                return value.getRealm();
            }

            @Override
            public boolean isDefaultVisible() {
                return false;
            }

            @Override
            public boolean isEditable(AuthenticationInfo obj) {
                return true;
            }

            @Override
            public boolean isEnabled(AuthenticationInfo obj) {
                return obj.isEnabled();
            }

            @Override
            protected void setStringValue(String value, AuthenticationInfo object) {
                object.setRealm(value);
            }
        });
        this.addColumn(new ExtTextColumn<AuthenticationInfo>(_GUI.T.authtablemodel_column_username()) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public String getStringValue(AuthenticationInfo value) {
                return GUIUtils.getAccountName(value.getUsername());
            }

            @Override
            public boolean isEditable(AuthenticationInfo obj) {
                // prevent hash values from been edited...
                if (CFG_GUI.CFG.isPresentationModeEnabled()) {
                    return false;
                }
                return true;
            }

            @Override
            public boolean isEnabled(AuthenticationInfo obj) {
                return obj.isEnabled();
            }

            @Override
            protected void setStringValue(String value, AuthenticationInfo object) {
                object.setUsername(value);
            }
        });
        this.addColumn(new ExtPasswordEditorColumn<AuthenticationInfo>(_GUI.T.authtablemodel_column_password()) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public int getMaxWidth() {
                return 140;
            }

            @Override
            public int getDefaultWidth() {
                return 110;
            }

            @Override
            public int getMinWidth() {
                return 100;
            }

            @Override
            protected String getPlainStringValue(AuthenticationInfo value) {
                return value.getPassword();
            }

            @Override
            public boolean isEnabled(AuthenticationInfo obj) {
                return obj.isEnabled();
            }

            @Override
            protected void setStringValue(String value, AuthenticationInfo object) {
                object.setPassword(value);
            }
        });
    }

    public void onChangeEvent(ChangeEvent event) {
        this._fireTableStructureChanged(new ArrayList<AuthenticationInfo>(AuthenticationController.getInstance().list()), false);
    }
}
