package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.controlling.AccountController;
import jd.plugins.Account;
import jd.plugins.PluginForHost;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class RenewAction extends TableBarAction {
    private static final long serialVersionUID = 8346982706972553448L;

    public RenewAction() {
        this.putValue(NAME, _GUI.T.settings_accountmanager_renew());
        this.putValue(AbstractAction.SMALL_ICON, new AbstractIcon(IconKey.ICON_RENEW, ActionColumn.SIZE));
    }

    public void actionPerformed(ActionEvent e) {
        final Account lAcc = getAccount();
        if (lAcc != null) {
            final PluginForHost plugin = lAcc.getPlugin();
            AccountController.openAfflink(plugin != null ? plugin.getLazyP() : null, plugin, "RenewAction");
        }
    }
}
