package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class MergeToPackageAction extends AppAction {

    /**
     * 
     */
    private static final long                          serialVersionUID = -4468197802870765463L;
    private SelectionInfo<CrawledPackage, CrawledLink> selection;

    public MergeToPackageAction(SelectionInfo<CrawledPackage, CrawledLink> si) {
        setName(_GUI._.MergeToPackageAction_MergeToPackageAction_());
        setIconKey("package_new");
        this.selection = si;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        try {
            String defValue = _GUI._.MergeToPackageAction_actionPerformed_newpackage_();
            try {
                defValue = selection.getFirstPackage().getName();
            } catch (Throwable e2) {
                // too many unsafe casts. catch problems - just to be sure
                Log.exception(e2);
            }

            final NewPackageDialog d = new NewPackageDialog(selection);
            Dialog.getInstance().showDialog(d);
            final String name = d.getName();

            if (name == null | name.trim().length() == 0) return;

            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    CrawledPackage newPackage = new CrawledPackage();
                    newPackage.setName(name);
                    String f = d.getDownloadFolder();
                    newPackage.setDownloadFolder(f);
                    // HashSet<String> rawDownloadFolder = new HashSet<String>();

                    LinkCollector.getInstance().moveOrAddAt(newPackage, selection.getSelectedChildren(), 0);
                    return null;
                }

            });
        } catch (DialogNoAnswerException e1) {
        }
    }

    @Override
    public boolean isEnabled() {
        return !selection.isEmpty();
    }

}
