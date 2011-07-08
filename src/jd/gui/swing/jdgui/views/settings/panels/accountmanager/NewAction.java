package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.gui.swing.jdgui.actions.ActionController;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class NewAction extends AbstractAction {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public NewAction() {
        this.putValue(NAME, _GUI._.settings_accountmanager_add());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("add", 16));
    }

    public void actionPerformed(ActionEvent e) {
        ActionController.getToolBarAction("action.premiumview.addacc").actionPerformed(new ActionEvent(this, 0, "addaccount"));
    }

}
