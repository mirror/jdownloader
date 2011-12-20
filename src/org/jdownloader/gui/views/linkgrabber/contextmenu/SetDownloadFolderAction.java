package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinknameCleaner;
import jd.controlling.linkcollector.VariousCrawledPackage;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.Dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.Dialog.FileChooserType;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.translate._JDT;

public class SetDownloadFolderAction extends AppAction {

    /**
     * 
     */
    private static final long                               serialVersionUID = -6632019767606316873L;

    private boolean                                         retOkay          = false;

    private HashSet<CrawledPackage>                         packages;
    private HashMap<CrawledPackage, ArrayList<CrawledLink>> newPackages;

    private CrawledPackage                                  pkg;

    public boolean newValueSet() {
        return retOkay;
    }

    public SetDownloadFolderAction(AbstractNode node, ArrayList<AbstractNode> selectio2) {
        if (selectio2 == null) {
            selectio2 = new ArrayList<AbstractNode>();
            selectio2.add(node);
        }

        packages = new HashSet<CrawledPackage>();
        HashSet<AbstractNode> map = new HashSet<AbstractNode>();
        for (AbstractNode n : selectio2) {
            map.add(n);
            if (n instanceof CrawledPackage) {
                if (pkg == null) pkg = (CrawledPackage) n;
                packages.add((CrawledPackage) n);
            } else {
                if (pkg == null) pkg = ((CrawledLink) n).getParentNode();
                packages.add(((CrawledLink) n).getParentNode());
            }

        }
        newPackages = new HashMap<CrawledPackage, ArrayList<CrawledLink>>();
        for (Iterator<CrawledPackage> it = packages.iterator(); it.hasNext();) {
            CrawledPackage next = it.next();
            boolean allChildren = true;
            boolean noChildren = true;
            ArrayList<CrawledLink> splitme = new ArrayList<CrawledLink>();
            for (CrawledLink l : next.getChildren()) {
                if (!map.contains(l)) {
                    allChildren = false;

                } else {
                    splitme.add(l);
                    noChildren = false;
                }
            }
            // if we do not have all links selected,
            if (allChildren || noChildren) {
                // keep package
            } else {
                // we might need to split packages
                it.remove();
                newPackages.put(next, splitme);

            }

        }
        setName(_GUI._.SetDownloadFolderAction_SetDownloadFolderAction_());
        setIconKey("save");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;

        try {

            final File[] dest = Dialog.getInstance().showFileChooser("downloadFolderDialog", _GUI._.SetDownloadFolderAction_SetDownloadFolderAction_(), FileChooserSelectionMode.DIRECTORIES_ONLY, null, false, FileChooserType.SAVE_DIALOG, new File(pkg.getDownloadFolder()));
            if (!isDownloadFolderValid(dest[0])) return;
            retOkay = true;
            IOEQ.add(new Runnable() {

                public void run() {

                    for (CrawledPackage pkg : packages) {
                        pkg.setDownloadFolder(dest[0].getAbsolutePath());
                    }

                    for (final Entry<CrawledPackage, ArrayList<CrawledLink>> entry : newPackages.entrySet()) {
                        if (!(entry.getKey() instanceof VariousCrawledPackage)) {
                            try {
                                if (entry.getKey().getDownloadFolder().equals(dest[0].getAbsolutePath())) continue;
                                Dialog.getInstance().showConfirmDialog(Dialog.LOGIC_DONOTSHOW_BASED_ON_TITLE_ONLY | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN,

                                _JDT._.SetDownloadFolderAction_actionPerformed_(entry.getKey().getName()), _JDT._.SetDownloadFolderAction_msg(entry.getKey().getName(), entry.getValue().size()), null, _JDT._.SetDownloadFolderAction_yes(), _JDT._.SetDownloadFolderAction_no());
                                entry.getKey().setDownloadFolder(dest[0].getAbsolutePath());
                                continue;
                            } catch (DialogClosedException e) {
                                return;
                            } catch (DialogCanceledException e) {

                            }
                        }
                        final CrawledPackage pkg = new CrawledPackage();
                        pkg.setExpanded(true);
                        pkg.setCreated(System.currentTimeMillis());
                        if (entry.getKey() instanceof VariousCrawledPackage) {
                            pkg.setName(LinknameCleaner.cleanFileName(entry.getValue().get(0).getName()));
                        } else {
                            pkg.setName(entry.getKey().getName());
                        }
                        pkg.setDownloadFolder(dest[0].getAbsolutePath());
                        IOEQ.getQueue().add(new QueueAction<Object, RuntimeException>() {

                            @Override
                            protected Object run() {
                                LinkCollector.getInstance().addmoveChildren(pkg, entry.getValue(), -1);
                                return null;
                            }

                            @Override
                            protected void postRun() {
                                // add to set selection later
                                packages.add(pkg);
                                LinkGrabberTableModel.getInstance().setSelectedObjects(new ArrayList<AbstractNode>(packages));
                            }

                        });
                    }
                }

            });
        } catch (DialogNoAnswerException e1) {
        }

    }

    @Override
    public boolean isEnabled() {

        if (newPackages.size() > 0 || packages.size() > 0) { return true;

        }
        return false;
    }

    /**
     * checks if the given file is valid as a downloadfolder, this means it must
     * be an existing folder or at least its parent folder must exist
     * 
     * @param file
     * @return
     */
    public static boolean isDownloadFolderValid(File file) {
        if (file == null || file.isFile()) return false;
        if (file.isDirectory()) return true;
        File parent = file.getParentFile();
        if (parent != null && parent.isDirectory() && parent.exists()) return true;
        return false;
    }

}
