package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;

import jd.controlling.AccountController;
import jd.plugins.Account;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class RemoveAction extends AbstractAction {
    /**
     * 
     */
    private static final long   serialVersionUID = 1L;
    private PremiumAccountTable table;
    private boolean             force            = false;

    public RemoveAction(PremiumAccountTable table, boolean force) {
        this.table = table;
        this.putValue(NAME, _GUI._.settings_accountmanager_delete());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("remove", ActionColumn.SIZE));
        this.force = force;
    }

    public void actionPerformed(ActionEvent e) {
        final ArrayList<Account> selection = table.getExtTableModel().getSelectedObjects();
        if (selection != null && selection.size() > 0) {
            new Thread() {
                public void run() {
                    StringBuilder sb = new StringBuilder();
                    for (Account account : selection) {
                        if (sb.length() > 0) sb.append("\r\n");
                        sb.append(account.getHoster() + "-Account (" + account.getUser() + ")");
                    }
                    try {
                        if (!force) Dialog.getInstance().showConfirmDialog(Dialog.STYLE_LARGE, _GUI._.account_remove_action_title(selection.size()), _GUI._.account_remove_action_msg(selection.size() <= 1 ? sb.toString() : "\r\n" + sb.toString()));
                        for (Account account : selection) {
                            AccountController.getInstance().removeAccount((String) null, account);
                        }
                    } catch (DialogClosedException e1) {
                        e1.printStackTrace();
                    } catch (DialogCanceledException e1) {
                        e1.printStackTrace();
                    }
                }
            }.start();

        }

    }

    @Override
    public boolean isEnabled() {
        final ArrayList<Account> selection = table.getExtTableModel().getSelectedObjects();
        if (selection != null && selection.size() > 0) { return true; }
        return false;
    }

}
