package jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.controlling.TaskQueue;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.hosterrule.AccountUsageRule;
import org.jdownloader.controlling.hosterrule.HosterRuleController;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class RemoveAction extends AppAction {
    /**
     * 
     */
    private static final long      serialVersionUID = 1L;
    private HosterRuleTable        table;
    private boolean                force            = false;
    private List<AccountUsageRule> selection        = null;

    public RemoveAction(HosterRuleTable table) {
        this.table = table;
        setName(_GUI._.literally_remove());
        setIconKey(IconKey.ICON_REMOVE);
    }

    public RemoveAction(List<AccountUsageRule> selection2, boolean force) {
        this.force = force;
        this.selection = selection2;
        setName(_GUI._.literally_remove());
        setIconKey(IconKey.ICON_REMOVE);
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        final List<AccountUsageRule> finalSelection;
        if (selection != null) {
            finalSelection = selection;
        } else if (table != null) {
            finalSelection = table.getModel().getSelectedObjects();
        } else {
            finalSelection = null;
        }

        if (finalSelection != null && finalSelection.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (AccountUsageRule account : finalSelection) {
                if (sb.length() > 0) sb.append("\r\n");
                sb.append(account.getHoster());
            }
            try {
                if (!force) Dialog.getInstance().showConfirmDialog(Dialog.STYLE_LARGE, _GUI._.accountUsageRule_remove_action_title(finalSelection.size()), _GUI._.accountUsageRule_remove_action_msg(finalSelection.size() <= 1 ? sb.toString() : "\r\n" + sb.toString()));
                TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                    @Override
                    protected Void run() throws RuntimeException {
                        for (AccountUsageRule account : finalSelection) {
                            HosterRuleController.getInstance().remove(account);
                        }
                        return null;
                    }
                });
            } catch (DialogNoAnswerException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public boolean isEnabled() {
        if (this.table != null) return super.isEnabled();
        return (selection != null && selection.size() > 0);
    }

}
