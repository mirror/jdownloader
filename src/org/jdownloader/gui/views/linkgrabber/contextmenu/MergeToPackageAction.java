package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.packagehistorycontroller.DownloadPathHistoryManager;
import org.jdownloader.gui.translate._GUI;

public class MergeToPackageAction extends CustomizableTableContextAppAction<CrawledPackage, CrawledLink> implements ActionContext {

    /**
     * 
     */
    private static final long serialVersionUID = -4468197802870765463L;
    private boolean           lastPathDefault  = false;

    public MergeToPackageAction() {
        setName(_GUI._.MergeToPackageAction_MergeToPackageAction_());
        setIconKey("package_new");
        setLastPathDefault(true);
    }

    @Customizer(name = "Use latest selected path as default one")
    public boolean isLastPathDefault() {
        return lastPathDefault;
    }

    public void setLastPathDefault(boolean lastPathDefault) {
        this.lastPathDefault = lastPathDefault;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        try {
            final NewPackageDialog d = new NewPackageDialog(getSelection());
            if (isLastPathDefault()) {
                List<String> pathes = DownloadPathHistoryManager.getInstance().listPathes((String[]) null);
                if (pathes != null && pathes.size() > 0) {
                    d.setDownloadFolder(pathes.get(0));
                }
            }
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
