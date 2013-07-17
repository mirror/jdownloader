package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.LinkCrawler;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.AddLinksDialog;

public class AddLinksAction extends AppAction implements CachableInterface {
    /**
     * 
     */
    private static final long serialVersionUID = -1824957567580275989L;

    public AddLinksAction(String string) {
        setName(string);
        setIconKey("add");
        setTooltipText(_GUI._.AddLinksAction_AddLinksAction_tt());
        setAccelerator(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK);

    }

    public AddLinksAction() {
        this(_GUI._.AddLinksToLinkgrabberAction());

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

    @Override
    public void setData(String data) {
    }

}
