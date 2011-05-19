package jd.gui.swing.jdgui.views.settings.panels.basicauthentication;

import java.awt.Component;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import jd.controlling.authentication.AuthenticationController;
import jd.controlling.authentication.AuthenticationInfo;

import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.table.ExtTableHeaderRenderer;
import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.columns.ExtCheckColumn;
import org.appwork.utils.swing.table.columns.ExtComboColumn;
import org.appwork.utils.swing.table.columns.ExtPasswordEditorColumn;
import org.appwork.utils.swing.table.columns.ExtTextColumn;
import org.jdownloader.extensions.antireconnect.translate.T;
import org.jdownloader.images.NewTheme;

public class AuthTableModel extends ExtTableModel<AuthenticationInfo> {

    public AuthTableModel() {
        super("AuthTableModel");
        update();
    }

    public void update() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                clear();
                addAllElements(AuthenticationController.getInstance().list());
                fireTableStructureChanged();
            }
        };

    }

    @Override
    protected void initColumns() {

        this.addColumn(new ExtCheckColumn<AuthenticationInfo>(T._.authtablemodel_column_enabled()) {

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("ok", 14));
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        return this;
                    }

                };

                return ret;
            }

            @Override
            protected int getMaxWidth() {
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
        this.addColumn(new ExtComboColumn<AuthenticationInfo>(T._.authtablemodel_column_type(), null) {
            @Override
            public boolean isHidable() {
                return false;
            }

            private ComboBoxModel typeModel = new DefaultComboBoxModel(new String[] { T._.authtablemodel_column_type_http(), T._.authtablemodel_column_type_ftp() });

            @Override
            public ComboBoxModel updateModel(ComboBoxModel dataModel, AuthenticationInfo value) {
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
            protected int getComboBoxItem(AuthenticationInfo value) {
                return value.getType().ordinal();
            }

            @Override
            protected void setSelectedIndex(int value, AuthenticationInfo object) {
                object.setType(AuthenticationInfo.Type.values()[value]);
            }

        });
        this.addColumn(new ExtTextColumn<AuthenticationInfo>(T._.authtablemodel_column_host()) {
            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected String getStringValue(AuthenticationInfo value) {

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
        this.addColumn(new ExtTextColumn<AuthenticationInfo>(T._.authtablemodel_column_username()) {
            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected String getStringValue(AuthenticationInfo value) {
                return value.getUsername();
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
                object.setUsername(value);
            }

        });

        this.addColumn(new ExtPasswordEditorColumn<AuthenticationInfo>(T._.authtablemodel_column_password()) {
            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected int getMaxWidth() {
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
}
