package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.controlling.AccountController;
import jd.plugins.Account;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class RenewAction extends TableBarAction {
    private static final long serialVersionUID = 8346982706972553448L;
    private PluginForHost     plugin           = null;

    public RenewAction() {

        this.putValue(NAME, _GUI._.settings_accountmanager_renew());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("renew", ActionColumn.SIZE));
    }

    public void actionPerformed(ActionEvent e) {
        if (plugin == null) return;
        CrossSystem.openURLOrShowMessage(plugin.getBuyPremiumUrl());
    }

    @Override
    public void setAccount(Account account) {
        /* TODO: optimize this as this is called often in cellrenderer */
        super.setAccount(account);
        String hosterName = AccountController.getInstance().getHosterName(getAccount());
        if (hosterName == null) {
            plugin = null;
            return;
        }
        plugin = JDUtilities.getPluginForHost(hosterName);
    }

}
