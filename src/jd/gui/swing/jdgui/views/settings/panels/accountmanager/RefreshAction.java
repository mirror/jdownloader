package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;

import jd.controlling.AccountController;
import jd.controlling.IOEQ;
import jd.controlling.accountchecker.AccountChecker;
import jd.plugins.Account;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class RefreshAction extends AbstractAction {
    /**
     * 
     */
    private static final long  serialVersionUID = 1L;
    private List<AccountEntry> selection;
    private boolean            ignoreSelection  = false;

    public RefreshAction() {
        this(null);
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("refresh", 20));
        ignoreSelection = true;
    }

    public RefreshAction(List<AccountEntry> selectedObjects) {
        selection = selectedObjects;
        this.putValue(NAME, _GUI._.settings_accountmanager_refresh());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("refresh", 16));
    }

    public void actionPerformed(ActionEvent e) {
        IOEQ.add(new Runnable() {
            public void run() {
                if (selection == null) {

                    for (Account acc : AccountController.getInstance().list()) {
                        AccountChecker.getInstance().check(acc, true);
                    }
                }
                for (AccountEntry acc : selection) {
                    AccountChecker.getInstance().check(acc.getAccount(), true);
                }
            }
        });
    }

    @Override
    public boolean isEnabled() {
        if (ignoreSelection) return true;
        return selection != null && selection.size() > 0;
    }

}
