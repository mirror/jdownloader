package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;

import jd.controlling.IOEQ;

import org.jdownloader.controlling.filter.FilterRule;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class RemoveAction extends AbstractAction {
    private static final long     serialVersionUID = -477419276505058907L;
    private ArrayList<FilterRule> selected;
    private FilterTable           table;
    private ArrayList<FilterRule> remove;
    private boolean               ignoreSelection  = false;

    public RemoveAction(FilterTable table) {
        this.table = table;
        this.putValue(NAME, _GUI._.settings_linkgrabber_filter_action_remove());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("remove", 20));
        this.ignoreSelection = true;

    }

    public RemoveAction(FilterTable table, ArrayList<FilterRule> selected, boolean force) {
        this.table = table;
        this.selected = selected;
        this.putValue(NAME, _GUI._.settings_linkgrabber_filter_action_remove());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("remove", 16));
    }

    public void actionPerformed(ActionEvent e) {
        remove = selected;
        if (remove == null) {
            remove = table.getExtTableModel().getSelectedObjects();
        }
        if (remove != null && remove.size() > 0) {
            IOEQ.add(new Runnable() {

                public void run() {
                    for (FilterRule lf : remove) {
                        LinkFilterController.getInstance().remove(lf);
                    }
                    table.getExtTableModel()._fireTableStructureChanged(LinkFilterController.getInstance().list(), false);
                }

            }, true);

        }
    }

    @Override
    public boolean isEnabled() {
        if (ignoreSelection) return super.isEnabled();
        return selected != null && selected.size() > 0;
    }

}
