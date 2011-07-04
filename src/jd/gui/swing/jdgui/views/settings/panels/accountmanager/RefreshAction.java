package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;

import jd.HostPluginWrapper;
import jd.controlling.AccountChecker;
import jd.controlling.AccountController;
import jd.plugins.Account;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class RefreshAction extends AbstractAction {
    /**
     * 
     */
    private static final long  serialVersionUID = 1L;
    private ArrayList<Account> selection;

    public RefreshAction(ArrayList<Account> selectedObjects) {
        selection = selectedObjects;
        this.putValue(NAME, _GUI._.settings_accountmanager_refresh());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("refresh", ActionColumn.SIZE));
    }

    public void actionPerformed(ActionEvent e) {
        ArrayList<Account> list;
        if (selection != null) {
            list = selection;
        } else {
            list = new ArrayList<Account>();
            final ArrayList<HostPluginWrapper> plugins = HostPluginWrapper.getHostWrapper();
            for (HostPluginWrapper plugin : plugins) {
                ArrayList<Account> accs = AccountController.getInstance().getAllAccounts(plugin.getHost());
                if (accs != null) {
                    for (Account acc : accs) {
                        list.add(acc);
                        acc.setHoster(plugin.getHost());
                    }
                }
            }
        }
        for (Account acc : list) {
            AccountChecker.getInstance().check(acc, true);
        }

    }

    @Override
    public boolean isEnabled() {
        return selection == null || selection.size() > 0;
    }

}
