package jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter;

import java.awt.Component;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import org.appwork.utils.swing.table.ExtTableHeaderRenderer;
import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.columns.ExtCheckColumn;
import org.appwork.utils.swing.table.columns.ExtComboColumn;
import org.appwork.utils.swing.table.columns.ExtTextEditorColumn;
import org.jdownloader.extensions.antireconnect.translate.T;
import org.jdownloader.images.Theme;

public class FilterTableModel extends ExtTableModel<LinkFilter> {

    public FilterTableModel(String id) {
        super(id);
        fill();
    }

    private void fill() {
        this.addElement(new LinkFilter(true, LinkFilter.Types.URL, "fsdahkjfsdbldshafsdf"));
        this.addElement(new LinkFilter(true, LinkFilter.Types.URL, "fsdahkjfsd´f df sfsdbldshafsdf"));
        this.addElement(new LinkFilter(false, LinkFilter.Types.PLUGIN, "fddsf dsfsdsf"));
        this.addElement(new LinkFilter(true, LinkFilter.Types.FILENAME, "fsdahkjfsdbldshafsdf"));
        this.addElement(new LinkFilter(true, LinkFilter.Types.URL, "rapidshare.com"));
    }

    @Override
    protected void initColumns() {
        this.addColumn(new ExtCheckColumn<LinkFilter>(T._.settings_linkgrabber_filter_columns_enabled()) {

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        final JLabel bt = new JLabel(Theme.getIcon("toggle", 12));
                        bt.setHorizontalAlignment(CENTER);
                        return bt;
                    }

                };

                return ret;
            }

            @Override
            protected int getMaxWidth() {
                return 20;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected boolean getBooleanValue(LinkFilter value) {
                return value.isEnabled();
            }

            @Override
            public boolean isEditable(LinkFilter obj) {
                return true;
            }

            @Override
            protected void setBooleanValue(boolean value, LinkFilter object) {
                object.setEnabled(value);
            }
        });

        String[] combo = new String[LinkFilter.Types.values().length];
        for (int i = 0; i < combo.length; i++) {
            switch (LinkFilter.Types.values()[i]) {
            case FILENAME:
                combo[i] = T._.settings_linkgrabber_filter_types_filename();
                break;
            case PLUGIN:
                combo[i] = T._.settings_linkgrabber_filter_types_plugin();
                break;
            case URL:
                combo[i] = T._.settings_linkgrabber_filter_types_url();
                break;
            default:
                combo[i] = LinkFilter.Types.values()[i].name();
            }
        }
        this.addColumn(new ExtComboColumn<LinkFilter>(T._.settings_linkgrabber_filter_columns_type(), new DefaultComboBoxModel(combo)) {

            @Override
            protected int getComboBoxItem(LinkFilter value) {
                return value.getType().ordinal();
            }

            @Override
            public boolean isEnabled(LinkFilter obj) {
                return true;
            }

            @Override
            protected int getMaxWidth() {
                return 60;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public boolean isSortable(LinkFilter obj) {
                return true;
            }

            @Override
            public boolean isEditable(LinkFilter obj) {
                return true;
            }

            @Override
            public void setValue(Object value, LinkFilter object) {

                object.setType(LinkFilter.Types.values()[(Integer) value]);
            }
        });
        this.addColumn(new ExtTextEditorColumn<LinkFilter>(T._.settings_linkgrabber_filter_columns_regex()) {

            @Override
            public boolean isEditable(LinkFilter obj) {
                return true;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected String getStringValue(LinkFilter value) {
                return value.getRegex();
            }

            @Override
            protected void setStringValue(String value, LinkFilter object) {
                object.setRegex(value);
            }

        });
    }
}
