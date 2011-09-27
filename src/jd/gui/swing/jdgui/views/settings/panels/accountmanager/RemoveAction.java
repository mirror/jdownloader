package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.AccountController;
import jd.controlling.IOEQ;
import jd.plugins.Account;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.AbstractRemoveAction;

public class RemoveAction extends AbstractRemoveAction {
    /**
     * 
     */
    private static final long   serialVersionUID = 1L;
    private PremiumAccountTable table;
    private boolean             force            = false;
    private ArrayList<Account>  selection        = null;

    public RemoveAction(PremiumAccountTable table) {

        this.table = table;
    }

    public RemoveAction(ArrayList<Account> selection, boolean force) {

        this.force = force;
        this.selection = selection;
        toContextMenuAction();
    }

    public void actionPerformed(ActionEvent e) {
        if (selection == null) selection = table.getExtTableModel().getSelectedObjects();
        if (selection != null && selection.size() > 0) {
            IOEQ.add(new Runnable() {
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
            });
        }

    }

    @Override
    public boolean isEnabled() {
        if (this.table != null) return super.isEnabled();
        return (selection != null && selection.size() > 0);
    }

}
