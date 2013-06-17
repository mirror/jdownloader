package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberPanel;
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
            final LinkGrabberTable table = LinkgrabberContextMenuManager.getInstance().getTable();
            final LinkGrabberPanel panel = LinkgrabberContextMenuManager.getInstance().getPanel();
            final ResetLinkGrabberOptionDialog dialog = new ResetLinkGrabberOptionDialog();
            Dialog.getInstance().showDialog(dialog);

            IOEQ.add(new Runnable() {

                public void run() {
                    if (dialog.getSettings().isRemoveLinks()) {
                        LinkCollector.getInstance().clear();
                    }
                    if (dialog.getSettings().isResetSorter()) {
                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {

                                table.getExtTableModel().setSortColumn(null);
                                table.getExtTableModel().refreshSort();
                                table.getTableHeader().repaint();
                            }
                        };
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
            e1.printStackTrace();
        }
    }

}
