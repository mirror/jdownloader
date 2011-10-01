package org.jdownloader.gui.views.components;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public abstract class AbstractRemoveAction extends AppAction {

    public AbstractRemoveAction() {
        super();
        setName(_GUI._.literally_remove());
        setIconKey("remove");

    }

    protected boolean rly(String msg) {

        try {
            Dialog.getInstance().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.literall_are_you_sure(), msg, null, null, null);
            return true;
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
        return false;

    }

}
