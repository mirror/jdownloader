package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.appwork.utils.swing.table.utils.MinimumSelectionObserver;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.Theme;

public class RemoveAction extends AbstractAction {
    /**
     * 
     */
    private static final long   serialVersionUID = 1L;
    private PremiumAccountTable table;

    public RemoveAction(PremiumAccountTable table) {
        this.table = table;
        this.putValue(NAME, _GUI._.settings_accountmanager_delete());
        this.putValue(AbstractAction.SMALL_ICON, Theme.getIcon("remove", 20));
        table.getSelectionModel().addListSelectionListener(new MinimumSelectionObserver(table, this, 1));

    }

    public void actionPerformed(ActionEvent e) {

    }

}
