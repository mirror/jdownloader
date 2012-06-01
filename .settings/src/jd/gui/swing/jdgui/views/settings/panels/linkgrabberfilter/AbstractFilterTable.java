package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.exttable.ExtTableModel;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;

public class AbstractFilterTable extends BasicJDTable<LinkgrabberFilterRule> {

    public AbstractFilterTable(ExtTableModel<LinkgrabberFilterRule> tableModel) {
        super(tableModel);
    }

}
