package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import org.jdownloader.controlling.LinkFilter;

import jd.gui.swing.jdgui.BasicJDTable;

public class FilterTable extends BasicJDTable<LinkFilter> {

    private static final long serialVersionUID = 4698030718806607175L;

    public FilterTable() {
        super(new FilterTableModel("FilterTable2"));

    }
}
