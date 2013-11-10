package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import jd.controlling.TaskQueue;
import jd.controlling.linkcollector.LinkCollector;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkgrabberSearchField;
import org.jdownloader.gui.views.linkgrabber.contextmenu.MenuManagerLinkgrabberTableContext;

public class ResetAction extends CustomizableAppAction {
    /**
     * 
     */
    private static final long serialVersionUID = 6027982395476716687L;

    public ResetAction() {

        setIconKey("reset");
        putValue(SHORT_DESCRIPTION, _GUI._.ResetAction_ResetAction_tt());

    }

    public void actionPerformed(ActionEvent e) {
        try {
            final ResetLinkGrabberOptionDialog dialog = new ResetLinkGrabberOptionDialog();
            Dialog.getInstance().showDialog(dialog);
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    if (dialog.getSettings().isInterruptCrawler()) {
                        LinkCollector.getInstance().abort();
                    }

                    if (dialog.getSettings().isRemoveLinks()) {
                        LinkCollector.getInstance().clear();
                    }
                    if (dialog.getSettings().isResetSorter()) {
                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {
                                final LinkGrabberTable table = MenuManagerLinkgrabberTableContext.getInstance().getTable();
                                table.getModel().setSortColumn(null);
                                table.getModel().refreshSort();
                                table.getTableHeader().repaint();
                            }
                        };
                    }
                    if (dialog.getSettings().isClearSearchFilter()) {

                        LinkgrabberSearchField.getInstance().setText("");
                        LinkgrabberSearchField.getInstance().onChanged();

                    }
                    return null;
                }
            });
        } catch (DialogNoAnswerException e1) {
            e1.printStackTrace();
        }
    }

}
