package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

import jd.plugins.Account;

public class EditAction extends AbstractAction {
    /**
     *
     */
    private static final long        serialVersionUID = 1L;
    private final List<AccountEntry> selection;

    public EditAction(final List<AccountEntry> selectedObjects) {
        selection = selectedObjects;
        this.putValue(NAME, _GUI.T.literally_edit());
        this.putValue(AbstractAction.SMALL_ICON, new AbstractIcon(IconKey.ICON_EDIT, 16));
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) {
            return;
        }
        final Account acc = selection.get(0).getAccount();
        EditAccountDialog editDialog = new EditAccountDialog(acc);
        try {
            Dialog.getInstance().showDialog(editDialog);
        } catch (DialogNoAnswerException e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public boolean isEnabled() {
        return selection != null && selection.size() == 1 && selection.get(0).getAccount().getPlugin() != null;
    }

}
