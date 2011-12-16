package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class MergeToPackageAction extends AppAction {

    /**
     * 
     */
    private static final long       serialVersionUID = -4468197802870765463L;
    private ArrayList<AbstractNode> selection;

    public MergeToPackageAction(ArrayList<AbstractNode> selection) {
        setName(_GUI._.MergeToPackageAction_MergeToPackageAction_());
        setIconKey("package_new");
        this.selection = selection;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        try {
            final String name = Dialog.getInstance().showInputDialog(0, _GUI._.MergeToPackageAction_MergeToPackageAction_(), null);
            if (name == null | name.trim().length() == 0) return;

            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    CrawledPackage newPackage = new CrawledPackage();

                    newPackage.setName(name);
                    HashSet<CrawledLink> links = new HashSet<CrawledLink>();
                    for (AbstractNode node : selection) {
                        if (node instanceof CrawledLink) {
                            links.add(((CrawledLink) node));
                        } else if (node instanceof CrawledPackage) {
                            synchronized (node) {
                                links.addAll(((CrawledPackage) node).getChildren());
                            }
                        }
                    }
                    LinkCollector.getInstance().addmoveChildren(newPackage, new ArrayList<CrawledLink>(links), 0);
                    return null;
                }

            });
        } catch (DialogNoAnswerException e1) {
        }
    }

    @Override
    public boolean isEnabled() {
        return selection != null && selection.size() > 0;
    }

}
