package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.Component;
import java.util.HashMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import jd.HostPluginWrapper;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.columns.ExtComboColumn;
import org.appwork.swing.exttable.columns.ExtCompoundColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.controlling.LinkFilter;
import org.jdownloader.gui.translate._GUI;

public class FilterColumn extends ExtCompoundColumn<LinkFilter> {

    private static final long          serialVersionUID = -4066544229005850863L;
    private ExtTextColumn<LinkFilter>  txt;
    private ExtComboColumn<LinkFilter> combo;

    public FilterColumn() {
        super(_GUI._.settings_linkgrabber_filter_columns_regex(), null);

        txt = new ExtTextColumn<LinkFilter>("") {

            private static final long serialVersionUID = -3818106880945441886L;

            @Override
            public boolean isEditable(LinkFilter obj) {
                return true;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public String getStringValue(LinkFilter value) {
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

            private static final long serialVersionUID = -3321450267594884039L;

            @Override
            protected int getSelectedIndex(LinkFilter value) {
                Integer ret = map.get(value.getRegex());
                if (ret == null) return 0;
                return ret;
            }

            public boolean isEnabled(LinkFilter obj) {
                return FilterColumn.this.isEnabled(obj);
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
