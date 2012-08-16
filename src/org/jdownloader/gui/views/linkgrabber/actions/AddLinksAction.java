package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.LinkCrawler;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.AddLinksDialog;

public class AddLinksAction extends AppAction {
    /**
     * 
     */
    private static final long serialVersionUID = -1824957567580275989L;

    public AddLinksAction(String string) {
        setIconKey("add");
        putValue(NAME, string);
    }

    public AddLinksAction() {
        this(_GUI._.AddLinksAction_());

    }

    public void actionPerformed(ActionEvent e) {
        try {
            AddLinksDialog dialog = new AddLinksDialog();
            final LinkCollectingJob crawljob = Dialog.getInstance().showDialog(dialog);

            AddLinksProgress d = new AddLinksProgress(crawljob);
            if (d.isHiddenByDontShowAgain()) {
                Thread thread = new Thread("AddLinksDialog") {
                    public void run() {
                        // we keep a reference to the text for later deep decrypt, because linkcollector clears the text from job
                        final String txt = crawljob.getText();
                        LinkCrawler lc = LinkCollector.getInstance().addCrawlerJob(crawljob);
                        if (lc != null) {
                            lc.waitForCrawling();
                            System.out.println("JOB DONE: " + lc.crawledLinksFound());
                            if (!crawljob.isDeepAnalyse() && lc.processedLinks() == 0 && lc.unhandledLinksFound() > 0) {
                                try {
                                    Dialog.getInstance().showConfirmDialog(0, _GUI._.AddLinksAction_actionPerformed_deep_title(), _GUI._.AddLinksAction_actionPerformed_deep_msg(), null, _GUI._.literally_yes(), _GUI._.literall_no());
                                    crawljob.setDeepAnalyse(true);
                                    crawljob.setText(txt);
                                    lc = LinkCollector.getInstance().addCrawlerJob(crawljob);
                                    lc.waitForCrawling();
                                    System.out.println("DEEP JOB DONE: " + lc.crawledLinksFound());
                                } catch (DialogClosedException e) {
                                    e.printStackTrace();
                                } catch (DialogCanceledException e) {
                                    e.printStackTrace();
                                }

                            }
                        }
                    }
                };

                thread.start();
            } else {
                Dialog.getInstance().showDialog(d);
            }

        } catch (DialogNoAnswerException e1) {
        }
    }

}
