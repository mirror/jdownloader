package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.IOEQ;

import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.controlling.packagizer.PackagizerRule;
import org.jdownloader.gui.views.components.AbstractRemoveAction;

public class RemoveAction extends AbstractRemoveAction {
    private static final long         serialVersionUID = -477419276505058907L;
    private ArrayList<PackagizerRule> selected;
    private FilterTable               table;
    private ArrayList<PackagizerRule> remove;
    private boolean                   ignoreSelection  = false;

    public RemoveAction(FilterTable table) {
        this.table = table;

        this.ignoreSelection = true;

    }

    public RemoveAction(FilterTable table, ArrayList<PackagizerRule> selected, boolean force) {
        this.table = table;
        this.selected = selected;
    }

    public void actionPerformed(ActionEvent e) {
        remove = selected;
        if (remove == null) {
            remove = table.getExtTableModel().getSelectedObjects();
        }
        if (remove != null && remove.size() > 0) {
            IOEQ.add(new Runnable() {

                public void run() {
                    for (PackagizerRule lf : remove) {
                        PackagizerController.getInstance().remove(lf);
                    }
                    table.getExtTableModel()._fireTableStructureChanged(PackagizerController.getInstance().list(), false);
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
