package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;

import jd.HostPluginWrapper;
import jd.controlling.AccountController;
import jd.plugins.Account;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class RefreshAction extends AbstractAction {
    /**
     * 
     */
    private static final long   serialVersionUID = 1L;
    private PremiumAccountTable table;
    private ArrayList<Account>  selection;

    public RefreshAction(PremiumAccountTable table) {
        this.table = table;
        this.putValue(NAME, _GUI._.settings_accountmanager_refresh());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("refresh", 20));
    }

    public RefreshAction(ArrayList<Account> selectedObjects) {
        selection = selectedObjects;
        this.putValue(NAME, _GUI._.settings_accountmanager_refresh());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("refresh", 20));
    }

    public void actionPerformed(ActionEvent e) {

        new Thread("AccountChecker") {
            public void run() {
                ArrayList<Account> list;
                if (selection != null) {
                    list = selection;
                } else {
                    list = new ArrayList<Account>();
                    final ArrayList<HostPluginWrapper> plugins = HostPluginWrapper.getHostWrapper();

                    for (HostPluginWrapper plugin : plugins) {
                        ArrayList<Account> accs = AccountController.getInstance().getAllAccounts(plugin.getHost());
                        for (Account acc : accs) {
                            list.add(acc);
                            acc.setHoster(plugin.getHost());
                        }

                    }

                }
                for (int i = 0; i < list.size(); i++) {
                    Account acc = list.get(i);
                    AccountController.getInstance().updateAccountInfo((String) null, acc, false);
                }
            }
        }.start();

    }

}
