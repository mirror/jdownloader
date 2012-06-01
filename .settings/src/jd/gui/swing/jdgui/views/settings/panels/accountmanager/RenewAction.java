package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.plugins.Account;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class RenewAction extends TableBarAction {
    private static final long serialVersionUID = 8346982706972553448L;

    public RenewAction() {
        this.putValue(NAME, _GUI._.settings_accountmanager_renew());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("renew", ActionColumn.SIZE));
    }

    public void actionPerformed(ActionEvent e) {
        Account lAcc = getAccount();
        if (lAcc == null) { return; }
        PluginForHost plugin = JDUtilities.getPluginForHost(lAcc.getHoster());
        if (plugin != null) CrossSystem.openURLOrShowMessage(plugin.getBuyPremiumUrl());
    }

}
