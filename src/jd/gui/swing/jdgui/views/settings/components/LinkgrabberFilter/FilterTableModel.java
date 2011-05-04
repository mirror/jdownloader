package jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter;

import java.awt.Component;
import java.util.HashMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import jd.HostPluginWrapper;
import jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter.LinkFilter.Types;

import org.appwork.utils.swing.table.ExtTableHeaderRenderer;
import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.columns.ExtCheckColumn;
import org.appwork.utils.swing.table.columns.ExtComboColumn;
import org.jdownloader.extensions.antireconnect.translate.T;
import org.jdownloader.images.Theme;

public class FilterTableModel extends ExtTableModel<LinkFilter> {

    public FilterTableModel(String id) {
        super(id);
        fill();
    }

    private void fill() {
        this.addElement(new LinkFilter(true, LinkFilter.Types.FILENAME, ""));
        this.addElement(new LinkFilter(true, LinkFilter.Types.PLUGIN, "rapidshare.com"));
    }

    @Override
    protected void initColumns() {

        this.addColumn(new ExtCheckColumn<LinkFilter>(T._.settings_linkgrabber_filter_columns_enabled()) {

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(Theme.getIcon("ok", 14));
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

        final HostPluginWrapper[] options = HostPluginWrapper.getHostWrapper().toArray(new HostPluginWrapper[] {});
        final HashMap<String, HostPluginWrapper> map = new HashMap<String, HostPluginWrapper>();
        for (int i = 0; i < options.length; i++) {
            map.put(options[i].getHost(), options[i]);
            map.put(options[i].getPattern() + "", options[i]);
        }
        this.addColumn(new ExtComboColumn<LinkFilter>(T._.settings_linkgrabber_filter_columns_type(), new DefaultComboBoxModel(combo)) {

            @Override
            protected int getComboBoxItem(LinkFilter value) {
                return value.getType().ordinal();
            }

            public boolean isEnabled(LinkFilter obj) {
                return obj.isEnabled();
            }

            @Override
            protected int getMaxWidth() {
                return 90;
            }

            @Override
            public int getMinWidth() {
                return getMaxWidth();
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            public boolean isSortable(LinkFilter obj) {
                return true;
            }

            @Override
            public boolean isEditable(LinkFilter obj) {
                return true;
            }

            @Override
            protected void setSelectedIndex(int value, LinkFilter object) {
                Types nValue = LinkFilter.Types.values()[value];
                if (object.getType() == LinkFilter.Types.PLUGIN && nValue != LinkFilter.Types.PLUGIN) {
                    HostPluginWrapper conv = map.get(object.getRegex());
                    if (conv != null) {
                        object.setRegex(conv.getPattern() + "");
                        object.setFullRegex(true);
                    }

                } else if (nValue == LinkFilter.Types.PLUGIN && object.getType() != LinkFilter.Types.PLUGIN) {
                    HostPluginWrapper conv = map.get(object.getRegex());
                    if (conv != null) {
                        object.setRegex(conv.getHost() + "");
                        object.setFullRegex(false);
                    }
                }
                object.setType(nValue);

            }

        });
        this.addColumn(new FilterColumn());

        this.addColumn(new ExtCheckColumn<LinkFilter>(T._.settings_linkgrabber_filter_columns_advanced()) {
            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(Theme.getIcon("regex", 14));
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
            protected boolean getBooleanValue(LinkFilter value) {
                return value.isFullRegex();
            }

            @Override
            public boolean isEditable(LinkFilter obj) {
                return obj.getType() != LinkFilter.Types.PLUGIN;

            }

            @Override
            public boolean isEnabled(LinkFilter obj) {
                return obj.getType() != LinkFilter.Types.PLUGIN;
            }

            @Override
            protected void setBooleanValue(boolean value, LinkFilter object) {
                object.setFullRegex(value);
            }

        });
    }
}
