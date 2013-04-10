package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.controlling.IOEQ;

import org.appwork.uio.UIOManager;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class ClearDownloadListAction extends AppAction {
    /**
     * 
     */
    private static final long serialVersionUID = 6027982395476716687L;

    public ClearDownloadListAction() {
        setIconKey("clear");
        putValue(SHORT_DESCRIPTION, _GUI._.ClearAction_tt_());
    }

    public void actionPerformed(ActionEvent e) {

        ConfirmDeleteLinksDialog d = new ConfirmDeleteLinksDialog(_GUI._.ClearDownloadListAction_actionPerformed_());

        UIOManager.I().show(ConfirmDeleteLinksDialogInterface.class, d);

        IOEQ.add(new Runnable() {

            public void run() {
                // LinkCollector.getInstance().clear();
            }

        }, true);

    }

}
