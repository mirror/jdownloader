package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberPanel;

public class ResetAction extends AppAction {
    /**
     * 
     */
    private static final long serialVersionUID = 6027982395476716687L;
    private LinkGrabberPanel  panel;

    public ResetAction(LinkGrabberPanel linkGrabberPanel) {
        setIconKey("reset");
        putValue(SHORT_DESCRIPTION, _GUI._.ResetAction_ResetAction_tt());
        this.panel = linkGrabberPanel;
    }

    public void actionPerformed(ActionEvent e) {
        try {

            final ResetLinkGrabberOptionDialog dialog = new ResetLinkGrabberOptionDialog();
            Dialog.getInstance().showDialog(dialog);

            IOEQ.add(new Runnable() {

                public void run() {
                    if (dialog.getSettings().isRemoveLinks()) {
                        LinkCollector.getInstance().clear();
                    }
                    if (dialog.getSettings().isResetSorter()) {
                        panel.getTable().getExtTableModel().setSortColumn(null);
                        panel.getTable().getExtTableModel().refreshSort();
                        panel.getTable().getTableHeader().repaint();
                    }
                    if (dialog.getSettings().isClearSearchFilter()) {
                        panel.resetSearch();
                    }
                    if (dialog.getSettings().isInterruptCrawler()) {
                        LinkCollector.getInstance().abort();
                    }
                }

            }, true);
        } catch (DialogNoAnswerException e1) {
        }
    }

}
