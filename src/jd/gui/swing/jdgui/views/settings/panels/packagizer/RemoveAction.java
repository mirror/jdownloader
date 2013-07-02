package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import java.awt.event.ActionEvent;

import jd.controlling.IOEQ;

import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.controlling.packagizer.PackagizerRule;
import org.jdownloader.gui.views.components.AbstractRemoveAction;
import org.jdownloader.translate._JDT;

public class RemoveAction extends AbstractRemoveAction {
    private static final long              serialVersionUID = -477419276505058907L;
    private java.util.List<PackagizerRule> selected;
    private PackagizerFilterTable          table;
    private java.util.List<PackagizerRule> remove;
    private boolean                        ignoreSelection  = false;

    public RemoveAction(PackagizerFilterTable table) {
        this.table = table;

        this.ignoreSelection = true;

    }

    public RemoveAction(PackagizerFilterTable table, java.util.List<PackagizerRule> selected, boolean force) {
        this.table = table;
        this.selected = selected;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        if (!rly(_JDT._.RemoveAction_actionPerformed_rly_msg())) return;
        remove = selected;
        if (remove == null) {
            remove = table.getModel().getSelectedObjects();
        }
        if (remove != null && remove.size() > 0) {
            IOEQ.add(new Runnable() {

                public void run() {
                    for (PackagizerRule lf : remove) {
                        PackagizerController.getInstance().remove(lf);
                    }
                    table.getModel()._fireTableStructureChanged(PackagizerController.getInstance().list(), false);
                }

            }, true);

        }
    }

    @Override
    public boolean isEnabled() {
        if (selected != null) {
            for (PackagizerRule rule : selected) {
                if (rule.isStaticRule()) return false;
            }
        }
        if (ignoreSelection) return super.isEnabled();
        return selected != null && selected.size() > 0;
    }

}
