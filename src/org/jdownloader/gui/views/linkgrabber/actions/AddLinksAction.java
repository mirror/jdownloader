package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.LinkCrawler;

import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.AddLinksDialog;

public class AddLinksAction extends CustomizableAppAction {
    /**
     * 
     */
    private static final long serialVersionUID = -1824957567580275989L;

    public AddLinksAction(String string) {
        setName(string);
        setIconKey("add");
        setTooltipText(_GUI._.AddLinksAction_AddLinksAction_tt());
        setAccelerator(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());

    }

    public AddLinksAction() {
        this(_GUI._.AddLinksToLinkgrabberAction());

    }

    public void actionPerformed(ActionEvent e) {
        new Thread("AddLinksAction") {
            public void run() {
                try {
                    AddLinksDialog dialog = new AddLinksDialog();
                    UIOManager.I().show(null, dialog);
                    dialog.throwCloseExceptions();
                    final LinkCollectingJob crawljob = dialog.getReturnValue();

                    AddLinksProgress d = new AddLinksProgress(crawljob);
                    if (d.isHiddenByDontShowAgain()) {
                        Thread thread = new Thread("AddLinksDialog") {
                            public void run() {
                                // we keep a reference to the text for later deep decrypt, because linkcollector clears the text from job
                                final String txt = crawljob.getText();
                                LinkCrawler lc = LinkCollector.getInstance().addCrawlerJob(crawljob);
                                if (lc != null) {
                                    lc.waitForCrawling();
                                    System.out.println("JOB DONE: " + lc.getCrawledLinksFoundCounter());
                                    if (!crawljob.isDeepAnalyse() && lc.getProcessedLinksCounter() == 0 && lc.getUnhandledLinksFoundCounter() > 0) {
                                        try {
                                            Dialog.getInstance().showConfirmDialog(0, _GUI._.AddLinksAction_actionPerformed_deep_title(), _GUI._.AddLinksAction_actionPerformed_deep_msg(), null, _GUI._.literally_yes(), _GUI._.literall_no());
                                            crawljob.setDeepAnalyse(true);
                                            crawljob.setText(txt);
                                            lc = LinkCollector.getInstance().addCrawlerJob(crawljob);
                                            lc.waitForCrawling();
                                            System.out.println("DEEP JOB DONE: " + lc.getCrawledLinksFoundCounter());
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
        }.start();

    }

}
