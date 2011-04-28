package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import javax.swing.JPopupMenu;

import org.appwork.utils.swing.table.ExtTableContextMenuController;

public class ContextMenuListener extends ExtTableContextMenuController<PremiumAccountTable> {

    public ContextMenuListener(PremiumAccountTable table) {
        super(table);

    }

    @Override
    protected JPopupMenu getPopup() {
        return null;
    }

    @Override
    protected JPopupMenu getEmptyPopup() {
        return null;
    }

}
