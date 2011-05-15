package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.Theme;

public class BuyAction extends AbstractAction {
    /**
     * 
     */
    private static final long   serialVersionUID = 1L;
    private PremiumAccountTable table;

    public BuyAction(PremiumAccountTable table) {
        this.table = table;
        this.putValue(NAME, _GUI._.settings_accountmanager_buy());
        this.putValue(AbstractAction.SMALL_ICON, Theme.getIcon("buy", 20));
    }

    public void actionPerformed(ActionEvent e) {

    }

}
