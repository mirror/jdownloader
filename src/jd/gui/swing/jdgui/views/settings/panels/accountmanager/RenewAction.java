package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class RenewAction extends TableBarAction {
    public RenewAction() {

        this.putValue(NAME, _GUI._.settings_accountmanager_renew());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("renew", ActionColumn.SIZE));
    }

    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public boolean isEnabled() {
        // if (account == null) return false;
        // AccountInfo ai = account.getAccountInfo();
        // if (ai == null) return true;
        // if (ai.isExpired()) return true;
        // return false;
        return true;
    }

}
