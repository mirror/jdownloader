package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import jd.controlling.linkcollector.LinkCollector;

import org.appwork.uio.UIOManager;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class ClearLinkgrabberAction extends AppAction {
    /**
     * 
     */
    private static final long serialVersionUID = 6027982395476716687L;

    public ClearLinkgrabberAction() {
        setIconKey("clear");
        putValue(SHORT_DESCRIPTION, _GUI._.ClearAction_tt_());
    }

    public void actionPerformed(ActionEvent e) {
        try {
            Dialog.getInstance().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.literally_are_you_sure(), _GUI._.ClearAction_actionPerformed_msg(), null, _GUI._.literally_yes(), _GUI._.literall_no());
            LinkCollector.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    LinkCollector.getInstance().clear();
                    return null;
                }
            });
        } catch (DialogNoAnswerException e1) {
        }
    }
}
