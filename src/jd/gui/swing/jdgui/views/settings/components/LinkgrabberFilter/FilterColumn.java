package jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter;

import java.awt.Component;
import java.util.HashMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import jd.HostPluginWrapper;

import org.appwork.utils.swing.table.ExtColumn;
import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.columns.ExtComboColumn;
import org.appwork.utils.swing.table.columns.ExtCompoundColumn;
import org.appwork.utils.swing.table.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;

public class FilterColumn extends ExtCompoundColumn<LinkFilter> {

    private ExtTextColumn<LinkFilter>  txt;
    private ExtComboColumn<LinkFilter> combo;

    public FilterColumn() {
        super(_GUI._.settings_linkgrabber_filter_columns_regex(), null);

        txt = new ExtTextColumn<LinkFilter>("") {

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

        };

        final HostPluginWrapper[] options = HostPluginWrapper.getHostWrapper().toArray(new HostPluginWrapper[] {});
        final HashMap<String, Integer> map = new HashMap<String, Integer>();
        for (int i = 0; i < options.length; i++) {
            map.put(options[i].getHost(), i);
        }
        combo = new ExtComboColumn<LinkFilter>("", new DefaultComboBoxModel(options)) {

            @Override
            protected int getComboBoxItem(LinkFilter value) {
                Integer ret = map.get(value.getRegex());
                if (ret == null) return 0;
                return ret;
            }

            public boolean isEnabled(LinkFilter obj) {
                return true;
            }

            @Override
            public boolean isEditable(LinkFilter obj) {
                return true;
            }

            public boolean isSortable(LinkFilter obj) {
                return true;
            }

            @Override
            protected void setSelectedIndex(int value, LinkFilter object) {

                object.setRegex(options[value].getHost());

            }

        };

        final ListCellRenderer def = combo.getRenderer();
        combo.setRenderer(new ListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                return def.getListCellRendererComponent(list, ((HostPluginWrapper) value).getHost(), index, isSelected, cellHasFocus);
            }
        });

    }

    @Override
    public ExtColumn<LinkFilter> selectColumn(LinkFilter object) {
        switch (object.getType()) {
        case PLUGIN:
            return combo;
        default:
            return txt;
        }
    }

    @Override
    public void setModelToCompounds(ExtTableModel<LinkFilter> model) {
        txt.setModel(model);
        combo.setModel(model);
    }

    @Override
    public boolean isEditable(LinkFilter obj) {
        return true;
    }

    @Override
    public boolean isHidable() {
        return false;
    }

    @Override
    public boolean isEnabled(LinkFilter obj) {
        return obj.isEnabled();
    }

    @Override
    public boolean isSortable(LinkFilter obj) {
        return true;
    }

    @Override
    public String getSortString(LinkFilter o1) {
        return o1.getRegex();
    }

}
