package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.controlling.packagizer.PackagizerRule;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class FilterTableModel extends ExtTableModel<PackagizerRule> {

    private static final long serialVersionUID = -7756459932564776739L;

    public FilterTableModel(String id) {
        super(id);

    }

    @Override
    protected void initColumns() {

        this.addColumn(new ExtCheckColumn<PackagizerRule>(_GUI._.settings_linkgrabber_filter_columns_enabled()) {

            private static final long serialVersionUID = -4667150369226691276L;

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    private static final long serialVersionUID = 3938290423337000265L;

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
            public int getMaxWidth() {
                return 30;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected boolean getBooleanValue(PackagizerRule value) {
                return value.isEnabled();
            }

            @Override
            public boolean isEditable(PackagizerRule obj) {
                return true;
            }

            @Override
            protected void setBooleanValue(boolean value, PackagizerRule object) {
                object.setEnabled(value);
            }
        });

        addColumn(new ExtTextColumn<PackagizerRule>(_GUI._.settings_linkgrabber_filter_columns_name()) {

            @Override
            public boolean isEnabled(PackagizerRule obj) {
                return obj.isEnabled();
            }

            @Override
            public String getStringValue(PackagizerRule value) {
                return value.getName();
            }
        });

        addColumn(new ExtTextColumn<PackagizerRule>(_GUI._.FilterTableModel_initColumns_filename()) {

            @Override
            public boolean isDefaultVisible() {
                return false;
            }

            @Override
            public String getStringValue(PackagizerRule value) {
                return !value.isEnabled() ? "" : value.getFilenameFilter().toString();
            }
        });

        addColumn(new ExtTextColumn<PackagizerRule>(_GUI._.FilterTableModel_initColumns_filesize()) {
            @Override
            public boolean isDefaultVisible() {
                return false;
            }

            @Override
            public String getStringValue(PackagizerRule value) {
                return !value.isEnabled() ? "" : value.getFilesizeFilter().toString();
            }
        });
        addColumn(new ExtTextColumn<PackagizerRule>(_GUI._.FilterTableModel_initColumns_filetype()) {
            @Override
            public boolean isDefaultVisible() {
                return false;
            }

            @Override
            public String getStringValue(PackagizerRule value) {
                return !value.isEnabled() ? "" : value.getFiletypeFilter().toString();
            }
        });

        addColumn(new ExtTextColumn<PackagizerRule>(_GUI._.FilterTableModel_initColumns_hoster()) {
            @Override
            public boolean isDefaultVisible() {
                return false;
            }

            @Override
            public String getStringValue(PackagizerRule value) {
                return !value.isEnabled() ? "" : value.getHosterURLFilter().toString();
            }
        });

        addColumn(new ExtTextColumn<PackagizerRule>(_GUI._.FilterTableModel_initColumns_source()) {
            @Override
            public boolean isDefaultVisible() {
                return false;
            }

            @Override
            public String getStringValue(PackagizerRule value) {
                return !value.isEnabled() ? "" : value.getSourceURLFilter().toString();
            }
        });
        addColumn(new ExtTextColumn<PackagizerRule>(_GUI._.settings_linkgrabber_filter_columns_condition()) {
            {
                rendererField.setHorizontalAlignment(JLabel.RIGHT);
            }

            @Override
            public boolean isEnabled(PackagizerRule obj) {
                return obj.isEnabled();
            }

            @Override
            public void resetRenderer() {
                super.resetRenderer();
                rendererField.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 1));

            }

            // @Override
            // public boolean isHidable() {
            // return false;
            // }

            @Override
            public String getStringValue(PackagizerRule value) {
                return _GUI._.settings_linkgrabber_filter_columns_if(value.toString());
            }
        });

    }

}
