package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

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
        System.out.println("klick");
    }

}
