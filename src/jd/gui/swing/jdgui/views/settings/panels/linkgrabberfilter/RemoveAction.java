package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.event.ActionEvent;

import jd.controlling.IOEQ;

import org.appwork.swing.exttable.ExtTable;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;
import org.jdownloader.gui.views.components.AbstractRemoveAction;
import org.jdownloader.translate._JDT;

public class RemoveAction extends AbstractRemoveAction {
    private static final long                     serialVersionUID = -477419276505058907L;
    private java.util.List<LinkgrabberFilterRule> selected;

    private java.util.List<LinkgrabberFilterRule> remove;
    private boolean                               ignoreSelection  = false;
    private AbstractFilterTable                   table;
    private LinkgrabberFilter                     linkgrabberFilter;

    public RemoveAction(LinkgrabberFilter linkgrabberFilter) {
        this.linkgrabberFilter = linkgrabberFilter;
        this.ignoreSelection = true;

    }

    public RemoveAction(AbstractFilterTable table, java.util.List<LinkgrabberFilterRule> selected, boolean force) {
        this.table = table;
        this.selected = selected;
    }

    public void actionPerformed(ActionEvent e) {
        if (!rly(_JDT._.RemoveAction_actionPerformed_rly_msg())) return;
        remove = selected;
        if (remove == null) {
            remove = getTable().getModel().getSelectedObjects();
        }
        if (remove != null && remove.size() > 0) {
            IOEQ.add(new Runnable() {

                public void run() {
                    for (LinkgrabberFilterRule lf : remove) {
                        LinkFilterController.getInstance().remove(lf);
                    }
                    getTable().getModel()._fireTableStructureChanged(LinkFilterController.getInstance().list(), false);
                }

            }, true);

        }

    }

    private ExtTable<LinkgrabberFilterRule> getTable() {
        if (table != null) return table;
        return linkgrabberFilter.getTable();
    }

    @Override
    public boolean isEnabled() {
        if (selected != null) {
            for (LinkgrabberFilterRule rule : selected) {
                if (rule.isStaticRule()) return false;
            }
        }
        if (ignoreSelection) return super.isEnabled();
        return selected != null && selected.size() > 0;
    }

}
