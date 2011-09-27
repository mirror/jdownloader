package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import jd.controlling.linkcollector.LinkCollector;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class ClearAction extends AppAction {
    public ClearAction() {
        setIconKey("clear");

        putValue(SHORT_DESCRIPTION, _GUI._.ClearAction_tt_());
    }

    public void actionPerformed(ActionEvent e) {

        try {
            Dialog.getInstance().showConfirmDialog(0, _GUI._.ClearAction_actionPerformed_(), _GUI._.ClearAction_actionPerformed_msg(), null, _GUI._.literally_yes(), _GUI._.literall_no());
            LinkCollector.getInstance().clear();
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

}
