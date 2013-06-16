package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.controlling.AccountController;
import jd.controlling.IOEQ;

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
    private List<AccountEntry>  selection        = null;

    public RemoveAction(PremiumAccountTable table) {

        this.table = table;
    }

    public RemoveAction(List<AccountEntry> selection2, boolean force) {

        this.force = force;
        this.selection = selection2;

    }

    public void actionPerformed(ActionEvent e) {
        List<AccountEntry> selection = this.selection;
        if (selection == null && this.table != null) selection = table.getExtTableModel().getSelectedObjects();
        if (selection != null && selection.size() > 0) {
            final List<AccountEntry> fselection = selection;
            IOEQ.add(new Runnable() {
                public void run() {
                    StringBuilder sb = new StringBuilder();
                    for (AccountEntry account : fselection) {
                        if (sb.length() > 0) sb.append("\r\n");
                        sb.append(account.getAccount().getHoster() + "-Account (" + account.getAccount().getUser() + ")");
                    }
                    try {
                        if (!force) Dialog.getInstance().showConfirmDialog(Dialog.STYLE_LARGE, _GUI._.account_remove_action_title(fselection.size()), _GUI._.account_remove_action_msg(fselection.size() <= 1 ? sb.toString() : "\r\n" + sb.toString()));
                        for (AccountEntry account : fselection) {
                            AccountController.getInstance().removeAccount(account.getAccount());
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
