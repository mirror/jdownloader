package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class MergeToPackageAction extends SelectionAppAction<CrawledPackage, CrawledLink> {

    /**
     * 
     */
    private static final long serialVersionUID = -4468197802870765463L;

    public MergeToPackageAction(SelectionInfo<CrawledPackage, CrawledLink> si) {
        super(si);
        setName(_GUI._.MergeToPackageAction_MergeToPackageAction_());
        setIconKey("package_new");

    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        try {
            final NewPackageDialog d = new NewPackageDialog(getSelection());
            Dialog.getInstance().showDialog(d);
            final String name = d.getName();

            if (name == null | name.trim().length() == 0) return;

            LinkCollector.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    CrawledPackage newPackage = new CrawledPackage();
                    newPackage.setName(name);
                    String f = d.getDownloadFolder();
                    newPackage.setDownloadFolder(f);
                    LinkCollector.getInstance().moveOrAddAt(newPackage, getSelection().getChildren(), 0);
                    return null;
                }

            });
        } catch (DialogNoAnswerException e1) {
        }
    }

}
