package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.test;

import java.awt.event.MouseEvent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.exttable.ExtTableModel;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;

public class ResultTable extends BasicJDTable<CrawledLink> {

    private TestWaitDialog owner;

    public ResultTable(TestWaitDialog testWaitDialog, ExtTableModel<CrawledLink> model) {
        super(model);
        owner = testWaitDialog;
    }

    @Override
    protected boolean onDoubleClick(MouseEvent e, CrawledLink obj) {
        if (obj.getMatchingFilter() != null && obj.getMatchingFilter() instanceof LinkgrabberFilterRule) {
            LinkgrabberFilterRule rule = (LinkgrabberFilterRule) obj.getMatchingFilter();
            owner.edit(rule);
        }
        return false;
    }
}
