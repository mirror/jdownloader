package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging.Log;
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
            String defValue = _GUI._.MergeToPackageAction_actionPerformed_newpackage_();
            try {
                if (selection.get(0) instanceof AbstractPackageNode) {
                    defValue = ((AbstractPackageNode) selection.get(0)).getName();
                } else {
                    defValue = ((AbstractPackageNode) ((AbstractPackageChildrenNode) selection.get(0)).getParentNode()).getName();
                }
            } catch (Throwable e2) {
                // too many unsafe casts. catch problems - just to be sure
                Log.exception(e2);
            }
            final String name = Dialog.getInstance().showInputDialog(0, _GUI._.MergeToPackageAction_MergeToPackageAction_(), defValue);
            if (name == null | name.trim().length() == 0) return;

            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    CrawledPackage newPackage = new CrawledPackage();
                    newPackage.setName(name);
                    HashSet<String> rawDownloadFolder = new HashSet<String>();
                    HashSet<CrawledLink> links = new HashSet<CrawledLink>();
                    for (AbstractNode node : selection) {
                        if (node instanceof CrawledLink) {
                            CrawledLink link = (CrawledLink) node;
                            links.add(link);
                            CrawledPackage parent = link.getParentNode();
                            if (parent != null) {
                                rawDownloadFolder.add(parent.getRawDownloadFolder());
                            }
                        } else if (node instanceof CrawledPackage) {
                            CrawledPackage parent = (CrawledPackage) node;
                            if (parent != null) {
                                rawDownloadFolder.add(parent.getRawDownloadFolder());
                            }
                            synchronized (node) {
                                links.addAll(parent.getChildren());
                            }
                        }
                    }
                    if (rawDownloadFolder.size() == 1) {
                        newPackage.setDownloadFolder(new ArrayList<String>(rawDownloadFolder).get(0));
                    }
                    LinkCollector.getInstance().moveOrAddAt(newPackage, new ArrayList<CrawledLink>(links), 0);
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
