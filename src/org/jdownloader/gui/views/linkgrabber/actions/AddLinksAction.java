package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkcrawler.CrawledLinkInfo;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.plugins.DownloadLink;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
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

        new Thread("Crawljob") {
            public void run() {
                try {

                    AddLinksDialog dialog = new AddLinksDialog();
                    CrawlerJob crawljob = Dialog.getInstance().showDialog(dialog);
                    System.out.println(crawljob);
                    LinkCrawler lc = new LinkCrawler() {
                        protected void handleFinalLink(CrawledLinkInfo link) {
                            super.handleFinalLink(link);
                            System.out.println(link);
                            LinkChecker.getInstance().check(new DownloadLink[] { link.getDownloadLink() }, false);
                        }

                    };
                    if (crawljob.isDeepAnalyse()) {
                        lc.crawlDeep(crawljob.getText());

                    } else {
                        lc.crawlNormal(crawljob.getText());
                    }
                    lc.waitForCrawling();
                    System.out.println(lc.getCrawledLinks().size());
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }
            }
        }.start();

    }

}
