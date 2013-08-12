package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import jd.controlling.TaskQueue;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.contextmenu.LinkgrabberContextMenuManager;

public class ResetAction extends AppAction {
    /**
     * 
     */
    private static final long                          serialVersionUID = 6027982395476716687L;
    private SelectionInfo<CrawledPackage, CrawledLink> selection;

    public ResetAction(SelectionInfo<CrawledPackage, CrawledLink> selection) {
        setIconKey("reset");
        putValue(SHORT_DESCRIPTION, _GUI._.ResetAction_ResetAction_tt());
        this.selection = selection;

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
                                final LinkGrabberTable table = LinkgrabberContextMenuManager.getInstance().getTable();
                                table.getModel().setSortColumn(null);
                                table.getModel().refreshSort();
                                table.getTableHeader().repaint();
                            }
                        };
                    }
                    if (dialog.getSettings().isClearSearchFilter()) {
                        LinkgrabberContextMenuManager.getInstance().getPanel().resetSearch();

                    }
                    return null;
                }
            });
        } catch (DialogNoAnswerException e1) {
            e1.printStackTrace();
        }
    }

}
