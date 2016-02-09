package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

import jd.controlling.AccountController;
import jd.controlling.TaskQueue;
import jd.controlling.accountchecker.AccountChecker;
import jd.plugins.Account;

public class RefreshAction extends AbstractAction {
    /**
     *
     */
    private static final long  serialVersionUID = 1L;
    private List<AccountEntry> selection;
    private boolean            ignoreSelection  = false;

    public RefreshAction() {
        this(null);
        this.putValue(AbstractAction.SMALL_ICON, new AbstractIcon(IconKey.ICON_REFRESH, 20));
        ignoreSelection = true;
    }

    public RefreshAction(List<AccountEntry> selectedObjects) {
        selection = selectedObjects;
        this.putValue(NAME, _GUI.T.settings_accountmanager_refresh());
        this.putValue(AbstractAction.SMALL_ICON, new AbstractIcon(IconKey.ICON_REFRESH, 16));
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) {
            return;
        }
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                if (selection == null) {
                    for (Account acc : AccountController.getInstance().list()) {
                        if (acc == null || acc.isEnabled() == false || acc.isValid() == false || acc.isTempDisabled()) {
                            continue;
                        }
                        AccountChecker.getInstance().check(acc, true);
                    }
                } else {
                    for (AccountEntry accEntry : selection) {
                        Account acc = accEntry.getAccount();
                        if (acc == null || acc.isEnabled() == false) {
                            continue;
                        }
                        AccountChecker.getInstance().check(acc, true);
                    }
                }
                return null;
            }
        });
    }

    @Override
    public boolean isEnabled() {
        if (ignoreSelection) {
            return true;
        }
        return selection != null && selection.size() > 0;
    }

}
