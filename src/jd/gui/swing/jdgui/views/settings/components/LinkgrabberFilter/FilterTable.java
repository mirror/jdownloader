package jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter;

import jd.gui.swing.jdgui.views.settings.panels.components.SettingsTable;

public class FilterTable extends SettingsTable<LinkFilter> {

    public FilterTable() {
        super(new FilterTableModel("FilterTable"));
        this.addMouseListener(new ContextMenuListener(this));

    }
}
