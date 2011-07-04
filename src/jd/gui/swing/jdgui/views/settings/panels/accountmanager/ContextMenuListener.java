package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import javax.swing.JPopupMenu;

import org.appwork.utils.swing.table.ExtTableContextMenuController;

public class ContextMenuListener extends ExtTableContextMenuController<PremiumAccountTable> {

    public ContextMenuListener(PremiumAccountTable table) {
        super(table);

    }

    @Override
    protected JPopupMenu getPopup() {
        JPopupMenu pu = new JPopupMenu();
        pu.add(new NewAction(table));
        pu.add(new RemoveAction(table));
        pu.add(new RefreshAction(table.getExtTableModel().getSelectedObjects()));
        return pu;
    }

    @Override
    protected JPopupMenu getEmptyPopup() {
        JPopupMenu pu = new JPopupMenu();
        pu.add(new NewAction(table));
        pu.add(new RemoveAction(table));
        pu.add(new BuyAction(null, table));
        pu.add(new RefreshAction(null));
        return pu;
    }

}
