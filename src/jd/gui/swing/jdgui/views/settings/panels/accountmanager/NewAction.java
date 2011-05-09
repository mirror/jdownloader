package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.gui.swing.jdgui.actions.ActionController;

import org.jdownloader.gui.translate.T;
import org.jdownloader.images.Theme;

public class NewAction extends AbstractAction {
    /**
     * 
     */
    private static final long   serialVersionUID = 1L;
    private PremiumAccountTable table;

    public NewAction(PremiumAccountTable table) {
        this.table = table;
        this.putValue(NAME, T._.settings_accountmanager_add());
        this.putValue(AbstractAction.SMALL_ICON, Theme.getIcon("add", 20));
    }

    public void actionPerformed(ActionEvent e) {
        ActionController.getToolBarAction("action.premiumview.addacc").actionPerformed(new ActionEvent(this, 0, "addaccount"));
    }

}
