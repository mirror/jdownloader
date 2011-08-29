package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.LinkCrawler;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.AddLinksDialog;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.CrawlerJob;
import org.jdownloader.images.NewTheme;

public class AddLinksAction extends AppAction {
    {
        putValue(SMALL_ICON, NewTheme.I().getIcon("add", 16));
        putValue(NAME, _GUI._.AddLinksAction_());

    }

    public void actionPerformed(ActionEvent e) {
        try {
            AddLinksDialog dialog = new AddLinksDialog();
            final CrawlerJob crawljob = Dialog.getInstance().showDialog(dialog);
            new Thread("AddLinksDialog") {
                public void run() {
                    LinkCrawler lc = LinkCollector.getInstance().addCrawlerJob(crawljob);
                    lc.waitForCrawling();
                    System.out.println("JOB DONE: " + lc.crawledLinksFound());
                }
            }.start();
        } catch (DialogNoAnswerException e1) {
        }
    }

}
